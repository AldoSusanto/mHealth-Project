package com.mhealth.application;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.mhealth.application.MainActivity.sync_intent;

public class AdminActivity extends AppCompatActivity{
    // variables to store the date of beginning of experiment
    Calendar calendar = Calendar.getInstance();
    Integer year = 0;
    Integer month = 0;
    Integer date = 0;
    boolean dateChosen = false ; // to ensure that the admin inputs the date

    String batchNumber; // the first two digits of the subject ID
    String subjectNumber; // the last two digits of the subject ID

    String subjectID ; // the user ID input that is going to be sent to the watch
    long unixTime ; // the unix time that is going to be sent to the watch
    // the watch app works using unix epoch time, so the date that will be sent to the watch needs to be converted to unix time

    boolean confirmed = false; // variable to ensure that the user has confirmed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // called whenever the create new study button is called
    // The function's purpose is to call the RootFolder activity to create the root folder
    public void createBatchFolder(View view){
        Intent intent = new Intent(this, RootFolderActivity.class);
        startActivity(intent);
    }

    // called whenever the register watch button is pressed
    // The function pops out the setup form that asks for the required input from user
    public void setup(View view){
        LinearLayout setupForm = (LinearLayout) findViewById(R.id.setupLayout);

        // initially, when the layout is still invisible, the first button click of setup will just show the form
        if (setupForm.getVisibility() == View.INVISIBLE){
            setupForm.setVisibility(View.VISIBLE);
        } else {
            setupForm.setVisibility(View.INVISIBLE);
        }

    }

    // called whenever the submit button in the setup form is pressed
    // the function gathers all the input from the form and asks for prompts confirmation
    public void registerWatch(View view){
        EditText userID = (EditText) findViewById(R.id.userIDinput); // gets the value of the User ID inputted from AdminActivity
        subjectID = userID.getText().toString();

        // below are the checkers for the user input

        // we gather the data as a string and ensure they aren't empty
        if (subjectID.isEmpty()) {
            Toast.makeText(this, "Please input a subject ID", Toast.LENGTH_SHORT).show();
            return;
        } else if (!dateChosen) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
        }

        // now we ensure that the input values are in correct format
        if (subjectID.length() != 4){
            Toast.makeText(this, "The subject ID should be 4 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // if the code reaches this place, it means the inputs are correct, including the date button

        batchNumber = subjectID.substring(0,2); // takes the first two digits and assign it as batch number
        subjectNumber= subjectID.substring(2);


        confirmSetupForm();
    }

    // The function creates a subject folder on the drive in the format "Study _ _"
    protected void createSubjectFolder(){
        FolderRequest.createSubject(this, "Study " + batchNumber, batchNumber + subjectNumber, new FolderRequest.FolderListener() {
            @Override
            public void onFolderCreated(Boolean success) {
                if (success){
                    Toast.makeText(AdminActivity.this, "Folder creation successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AdminActivity.this, "Folder creation failed; please try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void logout(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void authorize(View view) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("initialRequest",true);
        startActivity(intent);
    }

    // called when the change password button is pressed
    public void changePass(View view){
        Intent intent = new Intent(this, PasswordActivity.class);
        startActivityForResult(intent,1);
    }

    // used to change the password permanently
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            if(resultCode == Activity.RESULT_OK){
                String result = data.getStringExtra("result");
                SharedPreferences sharedPref = getSharedPreferences("password",0);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("password", result);
                editor.commit();
                MainActivity.password = sharedPref.getString("password","");

            }
            if(resultCode == Activity.RESULT_CANCELED){
                Toast prompt= Toast.makeText(this, "Password change cancelled", Toast.LENGTH_SHORT);
                prompt.show();
            }
        }
    }

    // gets the User ID input and time and send it to the watch setup activity
    protected void launchWatchSetup(){
        Intent intent = new Intent(this, WatchSetupActivity.class);
        intent.putExtra("subjectID", subjectID);
        intent.putExtra("unixTime", unixTime);
        stopService(sync_intent);
        startActivity(intent);
    }

    // pops up a dialog to confirm that the input from admin is correct
    protected void confirmSetupForm(){
        AlertDialog.Builder confirm = new AlertDialog.Builder(this);
        confirm.setMessage("Current Batch study: " + batchNumber +"\nUserID assigned: " + subjectNumber ).setCancelable(false).setPositiveButton("Confirm", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                confirmed = true;
                dialogInterface.dismiss();
                createSubjectFolder(); // when confirmed, the activity will create the subject folder then moves to the next activity
                launchWatchSetup();
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alert = confirm.create();
        alert.setTitle("Please confirm your subject's ID");
        alert.show();

    }

    // invokes the calendar in the setup form for the beginning of experiment date input
    public void showCalendar(View view){
        DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener(){

            @Override
            public void onDateSet(DatePicker datePicker, int yearX, int monthX, int dayofmonthX) {
                year = yearX;
                month = monthX +1;
                date = dayofmonthX;
                dateChosen = true;
                String dateStr = year+"/"+month+"/"+date ;
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
                try {
                    Date Dateobj = formatter.parse(dateStr);
                    unixTime = (long) Dateobj.getTime()/1000;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Toast prompt = Toast.makeText(AdminActivity.this, "Date: " + month + "/" + date + "/" + year , Toast.LENGTH_SHORT);
                prompt.show();
            }
        };
        new DatePickerDialog(this,listener,calendar.get((Calendar.YEAR)),calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }
}

