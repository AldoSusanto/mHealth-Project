package com.mhealth.application;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import net.openid.appauth.AuthorizationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.mhealth.application.AuthActivity.appAuthState;


class FolderRequest {

    private final String TAG = "CreateFolderTask";

    private static final int ROOT_FOLDER = 0;
    private static final int SUBJECT_FOLDER = 1;

    // for handling success/failure of folder creation
    interface FolderListener {
        void onFolderCreated(Boolean success);
    }

    private String parent;
    private String folderName;
    private int level;
    private FolderListener listener;
    private AuthorizationService authorizationService;

    private FolderRequest(Context context, String parent, String folderName, int level, FolderListener listener) {
        this.parent = parent;
        this.folderName = folderName;
        this.level = level;
        this.listener = listener;
        this.authorizationService = new AuthorizationService(context);
    }

    // handle cases for folder types
    private void createFolder() {
        switch (level) {
            case ROOT_FOLDER:
                createRootFolder();
                break;
            case SUBJECT_FOLDER:
                getRootFolderID();
                break;
        }
    }

    // create root level folder
    private void createRootFolder() {
        HashMap<String, String> jsonMap = new HashMap<>();
        jsonMap.put("name", folderName);
        jsonMap.put("mimeType", "application/vnd.google-apps.folder");

        JSONObject jsonObject = new JSONObject(jsonMap);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());

        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files")
                .setBody(body)
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response.has("id")) {
                            listener.onFolderCreated(true);
                            return;
                        }
                        Log.i(TAG, "We didn't get a response");
                        listener.onFolderCreated(false);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // must get the root folder ID before we create subject folder
    private void getRootFolderID() {

        // we query for parent folder ("Study __") and return the file id
        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files?" +
                        "q=name='" + parent + "'" +
                        "and trashed=false" +
                        "&fields=files(id)")
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response == null) {
                            Log.i(TAG, "We didn't get a response");
                            listener.onFolderCreated(false);
                            return;
                        }
                        String rootFolderID;
                        try {
                            JSONArray files = response.getJSONArray("files");
                            rootFolderID = files.getJSONObject(0).getString("id");
                            createSubjectFolder(rootFolderID);
                            return;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listener.onFolderCreated(false);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }

    // create subject level folder
    private void createSubjectFolder(String rootFolderID) {
        if (rootFolderID == null) {
            Log.i(TAG, "Folder ID was null");
            return;
        }

        // set metadata
        HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("name", folderName);
        jsonMap.put("mimeType", "application/vnd.google-apps.folder");
        jsonMap.put("parents", new String[]{rootFolderID});

        JSONObject jsonObject = new JSONObject(jsonMap);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());

        HttpRequestTask httpRequest = new HttpRequestTask()
                .setUrl("https://www.googleapis.com/drive/v3/files")
                .setBody(body)
                .setHttpResponseListener(new HttpRequestTask.HttpResponseListener() {
                    @Override
                    public void onResponseReceived(JSONObject response) {
                        if (response.has("id")) {
                            listener.onFolderCreated(true);
                            return;
                        }
                        Log.i(TAG, "We didn't get a response");
                        listener.onFolderCreated(false);
                    }
                });

        appAuthState.performActionWithFreshTokens(authorizationService, httpRequest);
    }


    // listener should be implemented to handle folder creation success/failure
    // called via CreateFolderRequest.createRoot(<args>)
    static void createRoot(Context context, String root, FolderListener listener) {
        // app must have been authorized and have made a connection before attempting to create a folder
        if (AuthActivity.isOnline()) {
            new FolderRequest(context, null, root, ROOT_FOLDER, listener).createFolder();
            return;
        } else if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "Not connected to Drive", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onFolderCreated(false);
        }
    }

    // called via CreateFolderRequest.createSubject(<args>)
    static void createSubject(Context context, String root, String subjectID, FolderListener listener) {
        if (AuthActivity.isOnline()) {
            new FolderRequest(context, root, subjectID, SUBJECT_FOLDER, listener).createFolder();
            return;
        } else if (appAuthState == null) {
            Toast.makeText(context, "App not authorized", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "Not connected to Drive", Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onFolderCreated(false);
        }
    }
}
