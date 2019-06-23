/**
 * Copyright (C) 2015 Garmin International Ltd.
 * Subject to Garmin SDK License Agreement and Wearables Application Developer Agreement.
 */
package com.mhealth.application;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.IQApplicationEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationListener;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationStatus;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;

public class DeviceService extends Service {

    public static final String IQDEVICE = "IQDevice";
    public static final String MY_APP = "a3421feed289106a538cb9547ab12095";

    private static final String TAG = DeviceActivity.class.getSimpleName();

    private ConnectIQ mConnectIQ;
    private IQDevice mDevice;
    private IQApp mMyApp;
    private boolean mAppIsOpen;

    @Override
    public IBinder onBind(Intent intent){
        throw new UnsupportedOperationException();
    }

    private IQOpenApplicationListener mOpenAppListener = new IQOpenApplicationListener() {
        @Override
        public void onOpenApplicationResponse(IQDevice device, IQApp app, IQOpenApplicationStatus status) {
            Toast.makeText(getApplicationContext(), "App Status: " + status.name(), Toast.LENGTH_SHORT).show();

            if (status == IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING) {
                mAppIsOpen = true;
            } else {
                mAppIsOpen = false;
            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //setContentView(R.layout.activity_device);

        //Intent intent = getIntent();
        mDevice = (IQDevice)intent.getParcelableExtra(IQDEVICE);
        mMyApp = new IQApp(MY_APP);
        mAppIsOpen = false;

        if (mDevice != null) {

            // Get our instance of ConnectIQ.  Since we initialized it
            // in our MainActivity, there is no need to do so here, we
            // can just get a reference to the one and only instance.
            mConnectIQ = ConnectIQ.getInstance();
            try {
                mConnectIQ.registerForDeviceEvents(mDevice, new IQDeviceEventListener() {

                    @Override
                    public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
                        // Since we will only get updates for this device, just display the status
                        //mDeviceStatus.setText(status.name());
                    }

                });
            } catch (InvalidStateException e) {
                Log.wtf(TAG, "InvalidStateException:  We should not be here!");
            }

            // Let's register to receive messages from our application on the device.
            try {
                mConnectIQ.registerForAppEvents(mDevice, mMyApp, new IQApplicationEventListener() {

                    @Override
                    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, IQMessageStatus status) {

                        // We know from our Comm sample widget that it will only ever send us strings, but in case
                        // we get something else, we are simply going to do a toString() on each object in the
                        // message list.
                        List<List<Object>> data = new ArrayList<>();
                        if (message.size() > 0) {
                            for (Object o : message) {
                                data = (List<List<Object>>) o;
                                Log.i("device-activity", o.toString());
                            }
                        }

                    }

                });
            } catch (InvalidStateException e) {
                Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show();
            }
        }
        return START_STICKY;
    }
}
