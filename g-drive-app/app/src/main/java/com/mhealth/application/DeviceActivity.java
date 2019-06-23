package com.mhealth.application;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSendMessageListener;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationListener;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationStatus;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;


public class DeviceActivity extends AppCompatActivity {

    public static final String IQDEVICE = "IQDevice";
    public static final String MY_APP = "a3421feed289106a538cb9547ab12095";

    private static final String TAG = DeviceActivity.class.getSimpleName();

    private TextView mDeviceName;
    private TextView mDeviceStatus;
    private ConnectIQ mConnectIQ;
    private IQDevice mDevice;
    private IQApp mMyApp;
    private boolean mAppIsOpen;

    private IQOpenApplicationListener mOpenAppListener = new IQOpenApplicationListener() {
        @Override
        public void onOpenApplicationResponse(IQDevice device, IQApp app, IQOpenApplicationStatus status) {
            Toast.makeText(getApplicationContext(), "App Status: " + status.name(), Toast.LENGTH_SHORT).show();

            if (status == IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING) {
                mAppIsOpen = true;
                return;
            }
            mAppIsOpen = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        Intent intent = getIntent();
        mDevice = intent.getParcelableExtra(IQDEVICE);
        mMyApp = new IQApp(MY_APP);
        mAppIsOpen = false;

        mDeviceName = (TextView)findViewById(R.id.devicename);
        mDeviceStatus = (TextView)findViewById(R.id.devicestatus);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDevice != null) {
            mDeviceName.setText(mDevice.getFriendlyName());
            mDeviceStatus.setText(mDevice.getStatus().name());

            // Get our instance of ConnectIQ.  Since we initialized it
            // in our MainActivity, there is no need to do so here, we
            // can just get a reference to the one and only instance.
            mConnectIQ = ConnectIQ.getInstance();
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, new IQDeviceEventListener() {

                    @Override
                    public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
                        // Since we will only get updates for this device, just display the status
                        mDeviceStatus.setText(status.name());
                    }

                });
            } catch (InvalidStateException e) {
                Log.wtf(TAG, "InvalidStateException:  We should not be here!");
            }

            // Let's check the status of our application on the device.
            try {
                mConnectIQ.getApplicationInfo(MY_APP, mDevice, new IQApplicationInfoListener() {

                    @Override
                    public void onApplicationInfoReceived(IQApp app) {
                        // This is a good thing. Now we can show our list of message options.

                        // Send a message to open the app
                        try {
                            Toast.makeText(getApplicationContext(), "Opening app...", Toast.LENGTH_SHORT).show();
                            mConnectIQ.openApplication(mDevice, app, mOpenAppListener);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onApplicationNotInstalled(String applicationId) {
                        // The Comm widget is not installed on the device so we have
                        // to let the user know to install it.
                        AlertDialog.Builder dialog = new AlertDialog.Builder(DeviceActivity.this);
                        dialog.setTitle(R.string.missing_widget);
                        dialog.setMessage(R.string.missing_widget_message);
                        dialog.setPositiveButton(android.R.string.ok, null);
                        dialog.create().show();
                    }

                });
            } catch (InvalidStateException|ServiceUnavailableException e) {
                e.printStackTrace();
            }
        }
    }

    // This function is invoked when the register watch button is pressed
    // It receives the values sent from WatchSetup, concatenate them into one string, and calls the transfer data function with the message
    public void onButtonClick(View v){

        Bundle extras = getIntent().getExtras();
        String ID = extras.getString("ID"); // receives the UserID received from the WatchSetup
        long Time = extras.getLong("Time");
        String t = String.valueOf(Time);
        ID = ID + t; // combines the value of the ID and the unixtime, the first 4 digits of this message will always be the UserID
        Object message = ID;

        MainActivity.updateLastSyncDay("Never");

        transferData(message);

        finish();
    }

    // Sends the message to the connected watch
    public void transferData(Object message){
        try {
            mConnectIQ.sendMessage(mDevice, mMyApp, message, new IQSendMessageListener() {

                @Override
                public void onMessageStatus(IQDevice device, IQApp app, IQMessageStatus status) {
                    Toast.makeText(DeviceActivity.this, status.name(), Toast.LENGTH_SHORT).show();
                }

            });
        } catch (InvalidStateException e) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show();
        } catch (ServiceUnavailableException e) {
            Toast.makeText(this, "ConnectIQ service is unavailable. Is Garmin Connect Mobile installed and running?", Toast.LENGTH_LONG).show();
        }
    }


}
