package com.mhealth.application;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import static com.mhealth.application.AuthActivity.appAuthState;


class UploadRequest {

    private final String TAG = "UploadRequest";

    // for handling whether the upload was successful
    interface UploadListener {
        void onUploadComplete(Boolean success, String lastSyncDay);
    }
    
    private long daysSinceStart;
    private long currentWeek;
    private long dayOfWeek;
    private long daysSinceLastSync;
    private long daysRemaining;
    private String today;

    private String currentWeekFolderID;
    private String pastWeekFolderID;

    private Context context;
    private List<List<Object>> data;
    private String subjectID;
    private UploadListener listener;
    private AuthorizationService authorizationService;

    private UploadRequest(Context context, List<List<Object>> data, UploadListener listener) {
        this.context = context;
        this.data = data;
        this.listener = listener;
        this.currentWeekFolderID = null;
        this.pastWeekFolderID = null;
        this.authorizationService = new AuthorizationService(context);
    }

    /* data structure

        data[0] = [subjectID] -> subjectID as String
        data[1] = [lastSyncDay] -> Unix timestamp as Integer
        data[2] = [begOfStudy] -> Unix timestamp as String
        data[3] = [current day, 1 day ago, ...] -> Unix timestamps as Integers
        data[4:end] = [current day, 1 day ago, ...] -> HR data as Strings (HR Total-HR Count-HR Avg)

     */

    // get the folder ID for the subject
    private void getSubjectFolderID() {

        subjectID = (String) data.get(0).get(0);

        // query for folder with name = subjectID that isn't in the trash
        // return files array with file name and id fields
        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files?" +
                        "q=name='" + subjectID + "'+" +
                        "and+trashed=false" +
                        "&fields=files(name,id)")
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "We didn't get a response");
                            listener.onUploadComplete(false,null);
                            return;
                        }

                        String subjectFolderID;
                        try {
                            JSONArray files = response.getJSONArray("files");
                            subjectFolderID = files.getJSONObject(0).getString("id");
                            // move onto the week folder
                            getCurrentWeekFolderID(subjectFolderID);
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listener.onUploadComplete(false,null);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // get the folder ID for the current week folder
    private void getCurrentWeekFolderID(final String subjectFolderID) {

        // multiply by 1000 to convert Unix time to milliseconds
        Date lastSyncDay = new Date(((Integer) data.get(1).get(0)).longValue() * 1000);
        Date begOfStudy = new Date(Long.parseLong((String) data.get(2).get(0)) * 1000);
        Date today = new Date();

        Log.i(TAG,"lastSyncDay: " + lastSyncDay.toString());
        Log.i(TAG,"begOfStudy: " + begOfStudy.toString());
        Log.i(TAG,"today: " + today.toString());

        daysSinceStart = (today.getTime() - begOfStudy.getTime()) / (24 * 60 * 60 * 1000);
        currentWeek = (daysSinceStart / 7) + 1;
        // if the beginning of the study was a Wednesday, day 0 is Wednesday
        dayOfWeek = daysSinceStart % 7;
        daysSinceLastSync = Math.min(((today.getTime() - lastSyncDay.getTime()) / (24 * 60 * 60* 1000)), 7);
        daysRemaining = daysSinceLastSync - 1;

        // we don't need to continue if we've already synced today
        if (daysSinceLastSync <= 0) {
            listener.onUploadComplete(false,null);
            Toast.makeText(context, "Already synced", Toast.LENGTH_LONG).show();
            return;
        }

        // query for folder with current week as the name and has the subject's folder as a parent
        HttpRequestTask httpRequest = new HttpRequestTask()
               .setUrl("https://www.googleapis.com/drive/v3/files?" +
                       "q=name='week" + currentWeek + "'+" +
                       "and+'" + subjectFolderID + "'+in+parents+" +
                       "and+trashed=false" +
                       "&fields=files(name,id)")
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "We didn't get a response");
                            listener.onUploadComplete(false,null);
                            return;
                        }

                        try {
                            JSONArray files = response.getJSONArray("files");
                            currentWeekFolderID = files.getJSONObject(0).getString("id");

                            if (dayOfWeek < daysSinceLastSync) {
                                // last sync was last week, so some files should be uploaded to that folder
                                getPastWeekFolderID(subjectFolderID);
                            } else {
                                uploadDataToDrive();
                            }
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // we didn't get an ID
                        createWeekFolder(subjectFolderID, currentWeek);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // get folder ID for last week's folder
    private void getPastWeekFolderID(final String subjectFolderID) {
        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files?" +
                        "q=name='week" + (currentWeek - 1) + "'+" +
                        "and+'" + subjectFolderID + "'+in+parents+" +
                        "and+trashed=false" +
                        "&fields=files(name,id)")
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "We didn't get a response");
                            listener.onUploadComplete(false,null);
                            return;
                        }

                        try {
                            JSONArray files = response.getJSONArray("files");
                            pastWeekFolderID = files.getJSONObject(0).getString("id");
                            uploadDataToDrive();
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        createWeekFolder(subjectFolderID, currentWeek - 1);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // if a week folder doesn't exist we'll create it
    private void createWeekFolder(final String subjectFolderID, final long weekNumber) {

        // set metadata
        HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("name", "week" + weekNumber);
        jsonMap.put("mimeType", "application/vnd.google-apps.folder");
        jsonMap.put("parents", new String[]{subjectFolderID});

        JSONObject jsonObject = new JSONObject(jsonMap);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());

        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files")
                .setBody(body)
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "No response");
                            return;
                        }

                        try {
                            if (weekNumber == currentWeek) {
                                currentWeekFolderID = response.getString("id");
                                if (dayOfWeek < daysSinceLastSync) {
                                    getPastWeekFolderID(subjectFolderID);
                                } else {
                                    uploadDataToDrive();
                                }
                            } else {
                                pastWeekFolderID = response.getString("id");
                                uploadDataToDrive();
                            }
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listener.onUploadComplete(false,null);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // construct .csv and upload to Drive
    private void uploadDataToDrive() {
        try {
            // used to keep track of which files are uploaded to the current week vs past week folder
            long dayCount = dayOfWeek;

            Log.i(TAG,"daysSinceStart: " + daysSinceStart);
            Log.i(TAG,"currentWeek: " + currentWeek);
            Log.i(TAG,"dayOfWeek: " + dayOfWeek);
            Log.i(TAG,"daysSinceLastSync: " + daysSinceLastSync);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM d yyyy", Locale.US);
            dateFormat.setTimeZone(TimeZone.getDefault());

            today = dateFormat.format(new Date());

            List<Integer> steps = new ArrayList<>();
            List<Integer> intensityMinutes = new ArrayList<>();
            List<String> days = new ArrayList<>();
            for (int i = 0; i < data.get(3).size(); ++i) {
                steps.add((Integer) data.get(3).get(i));
                intensityMinutes.add((Integer) data.get(4).get(i));
                days.add(dateFormat.format(new Date(((Integer) data.get(5).get(i)).longValue() * 1000)));
            }

            for (int i = 0; i < days.size(); ++i) {
                Log.i(TAG, days.get(i));
            }

            /*  .csv format

                Day, days[i]
                Steps, steps[i]
                Intensity Min, intensityMinutes[i]
                Interval start time, 0:00, 0:30, 1:30, ...
                HR Count, count @ 0:00, count @ 0:30, ...
                HR Average, avg @ 0:00, avg @ 0:30, ...

             */

            // start at 1 so we don't send partial data from today
            for (int i = 1; i < daysSinceLastSync + 1; ++i) {
                Log.i(TAG, "i: " + i);
                String day = days.get(i);
                String output = "Day," + day + "\n";
                output += "Steps," + steps.get(i).toString() + "\n";
                output += "Intensity Min," + intensityMinutes.get(i).toString() + "\n";
                if (data.get(i+6) != null) {
                    int hour = 0;
                    output += "Interval start time,";
                    for (int j = 0; j < 48; ++j) {
                        if (j % 2 == 0) {
                            output += hour + ":00,";
                        } else {
                            if (j == 47) {
                                output += hour + ":30\n";
                                break;
                            }
                            output += hour + ":30,";
                            hour++;
                        }
                    }
                    output += "HR Count,";
                    String temp = "HR Average,";
                    for (int k = 0; k < data.get(i+6).size(); ++k) {
                        String record[] = data.get(i+6).get(k).toString().split("-");
                        if (k < data.get(i+6).size()-1) {
                            output += record[1].replaceAll("\\p{Cntrl}","") + ",";
                            temp += record[2].replaceAll("\\p{Cntrl}","") + ",";
                        } else {
                            output += record[1].replaceAll("\\p{Cntrl}","") + "\n";
                            temp += record[2].replaceAll("\\p{Cntrl}","") + "\n";
                        }
                    }
                    temp = temp.replace(")","");
                    output += temp;
                }
                output = output.replace("[", "");
                output = output.replace("]", "");
                output += "\n";

                int dayNumber = (int) ((dayCount + 6) % 7) + 1;
                // convert passed List<List<Integer>> to .csv
                String fileName = dayNumber + " " + subjectID + " " + day;
                final File file = new File(context.getFilesDir(), fileName);
                OutputStream outputStream = new FileOutputStream(file);
                DataOutputStream out = new DataOutputStream(outputStream);
                out.writeBytes(output);

                HashMap<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("name", fileName);
                jsonMap.put("mimeType", "application/vnd.google-apps.spreadsheet");

                Log.i(TAG,"dayCount: " + dayCount);
                if (dayCount > 0) {
                    jsonMap.put("parents", new String[]{currentWeekFolderID});
                } else {
                    jsonMap.put("parents", new String[]{pastWeekFolderID});
                }
                dayCount--;
                JSONObject jsonObject = new JSONObject(jsonMap);

                RequestBody metaDataPart = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
                RequestBody filePart = RequestBody.create(MediaType.parse("text/csv"), file);

                MultipartBody body = new MultipartBody.Builder()
                        .addPart(metaDataPart)
                        .addPart(filePart)
                        .build();

                HttpRequestTask httpRequest = new HttpRequestTask()
                        .setUrl("https://www.googleapis.com/upload/drive/v3/files")
                        .setBody(body)
                        .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                            @Override
                            public void onResponseReceived(JSONObject response) {
                                // don't need to keep data files on the device
                                if (!file.delete())
                                    Log.i(TAG, "Failed to delete file");

                                if (response == null) {
                                    Log.i(TAG, "We didn't get a response on upload");
                                    listener.onUploadComplete(false,null);
                                    return;
                                }
                                try {
                                    String id = response.getString("id");
                                    if (id == null) {
                                        listener.onUploadComplete(false,null);
                                        return;

                                    // upload is only considered successful if all files go through
                                    } else if (daysRemaining > 0) {
                                        daysRemaining--;
                                    } else {
                                        listener.onUploadComplete(true, today);
                                    }
                                    return;
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                listener.onUploadComplete(false,null);
                            }
                        });

                appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
            }
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        listener.onUploadComplete(false,null);
    }


    // called via UploadRequest.upload(<args>)
    // listener should be implemented to handle upload success/failure
    static void upload(Context context, List<List<Object>> data, UploadListener listener) {
        // app must have been authorized and have made a connection before attempting to upload
        if (AuthActivity.isOnline()) {
            new UploadRequest(context, data, listener).getSubjectFolderID();
            return;
        }
        else if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "Not connected to Drive", Toast.LENGTH_SHORT).show();
        if (listener != null)
            listener.onUploadComplete(false,null);
    }
}
