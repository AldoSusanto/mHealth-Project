package com.mhealth.application;


import android.content.SharedPreferences;
import android.os.StrictMode;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    static Intent sync_intent;

    static String password = ""; // variable containing the password of the device
    String lastSyncDay;

    interface SyncListener {
        void onSyncComplete(String newSyncDay);
    }

    static SyncListener syncListener;

    private TextView lastSyncView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, getFilesDir().getAbsolutePath());

        if (android.os.Build.VERSION.SDK_INT > 19) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        lastSyncView = (TextView) findViewById(R.id.lastSyncDay);
        // waits for lastSyncDay updates
        syncListener = new SyncListener() {
            @Override
            public void onSyncComplete(String newSyncDay) {
                if (newSyncDay != null) {
                    getSharedPreferences("lastSyncDay", 0).edit()
                            .putString("lastSyncDay", newSyncDay)
                            .commit();
                    lastSyncDay = newSyncDay;
                    lastSyncView.setText(lastSyncDay);
                }
            }
        };

        lastSyncDay = getSharedPreferences("lastSyncDay",0).getString("lastSyncDay",null);

        sync();

        // sets up the shared preferences variable for the password
        SharedPreferences sharedPref = getSharedPreferences("password",0);
        SharedPreferences.Editor editor = sharedPref.edit();
        String test = sharedPref.getString("password","");
        if(test.isEmpty()){ // this means that the password value has never been initialized
            editor.putString("password","csce482");
            editor.commit();
            password = "csce482"; // sets the default password to "csce482"
        }else{
            password = test;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check we're connected
        AuthActivity.restoreAuthState(this);

        // update last day synced
        if (lastSyncDay == null) {
            lastSyncView.setText("Never");
        } else {
            lastSyncView.setText(lastSyncDay);
        }

    }

    static void updateLastSyncDay(String newSyncDay) {
        syncListener.onSyncComplete(newSyncDay);
    }

    // This function is invoked whenever the login button is called
    // It checks whether the entered password is correct, if it is, it will call the AdminActivity
    public void login(View view){
        //getting the values from the two input texts
        EditText temp = (EditText) findViewById(R.id.password);
        String passwordInput = temp.getText().toString();
        Toast prompt;

        if (passwordInput.isEmpty()){
            prompt = Toast.makeText(this, "Password required to login", Toast.LENGTH_LONG);
            prompt.show();
        }else if (passwordInput.equals(password)){ // if the password is correct, opens up the AdminActivity
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
        }else{
            prompt = Toast.makeText(this, "Invalid password", Toast.LENGTH_LONG);
            prompt.show();
        }

    }

    public void connect(View view) {
        AuthActivity.restoreAuthState(this);
    }

    public void sync() {
        sync_intent = new Intent(this, SyncService.class);
        startService(sync_intent);

        Intent nIntent = new Intent(this, NotificationService.class);
        startService(nIntent);
    }

    @Override
    public void onBackPressed() {
       // blank to avoid navigating back to restricted Activities
    }
}