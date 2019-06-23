package com.mhealth.application;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import android.widget.Toast;

import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.mhealth.application.adapter.IQDeviceAdapter;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener;
import com.garmin.android.connectiq.ConnectIQ.IQConnectType;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQSdkErrorStatus;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;

import java.util.List;

public class SyncService extends Service {

    public static final String IQDEVICE = "IQDevice";
    public static final String MY_APP = "a3421feed289106a538cb9547ab12095";

    private static final String TAG = "SyncService";

    private ConnectIQ mConnectIQ;
    private IQDeviceAdapter mAdapter;
    private IQDevice mDevice;
    private IQApp mMyApp;
    private static boolean mAppIsOpen;
    private boolean mSdkReady = false;

    private Handler handler;

    public SyncService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mAdapter = new IQDeviceAdapter(this);
        //WIRELESS FOR WIRELESS, TETHERED FOR ADB SIMULATOR
        mConnectIQ = ConnectIQ.getInstance(this, IQConnectType.WIRELESS);
//        mConnectIQ = ConnectIQ.getInstance(this, IQConnectType.TETHERED);
        // Initialize the SDK
        mConnectIQ.initialize(this, true, mListener);

        // using a handler because can't use Toast on a background thread
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Toast.makeText(SyncService.this, "Connected to " + mDevice.getFriendlyName(), Toast.LENGTH_SHORT).show();
                runApplication();
            }
        };

        new Thread()
        {
            @Override
            public void run()
            {
                IQDevice device = null;
                while(device == null || device.getStatus() != IQDeviceStatus.CONNECTED) {
                    try{Thread.sleep(1000);}catch(Exception e){}
                    for (int i = 0; i < mAdapter.getCount(); ++i) {
                        if (mAdapter.getItem(i) != null && mAdapter.getItem(i).getStatus() == IQDeviceStatus.CONNECTED) {
                            device = mAdapter.getItem(i);
                            break;
                        }
                    }
                }
                mDevice = device;
                handler.sendEmptyMessage(0);
            }
        }.start();

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private IQDeviceEventListener mDeviceEventListener = new IQDeviceEventListener() {

        @Override
        public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
            mAdapter.updateDeviceStatus(device, status);
        }

    };

    private ConnectIQListener mListener = new ConnectIQListener() {

        @Override
        public void onInitializeError(IQSdkErrorStatus errStatus) {
            mSdkReady = false;
        }

        @Override
        public void onSdkReady() {
            loadDevices();
            mSdkReady = true;
        }

        @Override
        public void onSdkShutDown() {
            mSdkReady = false;
        }

    };

    public void loadDevices() {
        // Retrieve the list of known devices
        try {
            List<IQDevice> devices = mConnectIQ.getKnownDevices();

            if (devices != null) {
                mAdapter.setDevices(devices);

                // Let's register for device status updates.  By doing so we will
                // automatically get a status update for each device so we do not
                // need to call getStatus()

                for (IQDevice device : devices) {
                    mConnectIQ.registerForDeviceEvents(device, mDeviceEventListener);
                }
            }

        }
        catch(Exception e)
        {
            Log.d("Unhandled exception","");
        }
    }

    public static boolean isApplicationRunning()
    {
        return mAppIsOpen;
    }

    private ConnectIQ.IQOpenApplicationListener mOpenAppListener = new ConnectIQ.IQOpenApplicationListener() {
        @Override
        public void onOpenApplicationResponse(IQDevice device, IQApp app, ConnectIQ.IQOpenApplicationStatus status) {
            //Toast.makeText(getApplicationContext(), "App Status: " + status.name(), Toast.LENGTH_SHORT).show();

            if (status == ConnectIQ.IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING) {
                mAppIsOpen = true;
                return;
            }
            mAppIsOpen = false;
        }
    };

    public void runApplication()
    {
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
                Log.wtf("", "InvalidStateException:  We should not be here!");
            }

            // Let's register to receive messages from our application on the device.
            try {
                mAppIsOpen = true;
                mConnectIQ.registerForAppEvents(mDevice, mMyApp, new ConnectIQ.IQApplicationEventListener() {

                    @Override
                    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, ConnectIQ.IQMessageStatus status) {

                        // We know from our Comm sample widget that it will only ever send us strings, but in case
                        // we get something else, we are simply going to do a toString() on each object in the
                        // message list.
                        List<List<Object>> data = null;
                        if (message.size() > 0) {
                            for (Object o : message) {
                                data = (List<List<Object>>) o;
                                Log.i("device-activity",o.toString());
                            }
                        }

                        Toast.makeText(SyncService.this, "Attempting to upload data", Toast.LENGTH_SHORT).show();
                        UploadRequest.upload(SyncService.this, data, new UploadRequest.UploadListener() {
                            @Override
                            public void onUploadComplete(Boolean success, String lastSyncDay) {
                                if (success) {
                                    Toast.makeText(SyncService.this, "Upload successful", Toast.LENGTH_LONG).show();
                                    MainActivity.updateLastSyncDay(lastSyncDay);
                                    try {
                                        mConnectIQ.sendMessage(mDevice, mMyApp, "success", null);
                                    } catch (ServiceUnavailableException | InvalidStateException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(SyncService.this, "Upload failed", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                });

            } catch (Exception e) {
                Log.d("", e.getMessage());
            }
        }
    }
}
