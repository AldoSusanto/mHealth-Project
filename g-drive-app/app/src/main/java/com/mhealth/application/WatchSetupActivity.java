package com.mhealth.application;


import android.app.ListActivity;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.mhealth.application.adapter.IQDeviceAdapter;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener;
import com.garmin.android.connectiq.ConnectIQ.IQConnectType;
import com.garmin.android.connectiq.ConnectIQ.IQDeviceEventListener;
import com.garmin.android.connectiq.ConnectIQ.IQSdkErrorStatus;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQDevice.IQDeviceStatus;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;


public class WatchSetupActivity extends ListActivity {

    private ConnectIQ mConnectIQ;
    private TextView mEmptyView;
    private IQDeviceAdapter mAdapter;
    private boolean mSdkReady = false;

    public String subjectID; // this will be the subjectID output that will be sent to DeviceActivity
    public long unixTime;

    boolean deviceIsLaunched; // this is to ensure that in the onResume function, this intent will finish only after device activity is on

    private IQDeviceEventListener mDeviceEventListener = new IQDeviceEventListener() {

        @Override
        public void onDeviceStatusChanged(IQDevice device, IQDeviceStatus status) {
            mAdapter.updateDeviceStatus(device, status);
        }

    };

    private ConnectIQListener mListener = new ConnectIQListener() {

        @Override
        public void onInitializeError(IQSdkErrorStatus errStatus) {
            if( null != mEmptyView )
                mEmptyView.setText(R.string.initialization_error + errStatus.name());
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchsetup);

        // Here we are specifying that we want to use a WIRELESS bluetooth connection.
        // We could have just called getInstance() which would by default create a version
        // for WIRELESS, unless we had previously gotten an instance passing TETHERED
        // as the connection type.
        mConnectIQ = ConnectIQ.getInstance(this, IQConnectType.WIRELESS);
//        mConnectIQ = ConnectIQ.getInstance(this, IQConnectType.TETHERED);

        mAdapter = new IQDeviceAdapter(this);
        getListView().setAdapter(mAdapter);


        // Initialize the SDK
        mConnectIQ.initialize(this, true, mListener);

        mEmptyView = (TextView)findViewById(android.R.id.empty);

        // assigning batchNum variables
        Bundle extras = getIntent().getExtras();
        subjectID = extras.getString("subjectID");
        unixTime = extras.getLong("unixTime");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mSdkReady) {
            loadDevices();
        }

        // we should finish this activity when the user has registered the watch
        if(deviceIsLaunched){
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // It is a good idea to unregister everything and shut things down to
        // release resources and prevent unwanted callbacks.
        try {
            mConnectIQ.unregisterAllForEvents();
            mConnectIQ.shutdown(this);
        } catch (InvalidStateException e) {
            // This is usually because the SDK was already shut down
            // so no worries.
        }
    }

    // the Device Activity requires the IQDevice activity that this activity figures out
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        IQDevice device = mAdapter.getItem(position);
        deviceIsLaunched = true;

        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra("ID", subjectID);
        intent.putExtra("Time", unixTime);
        intent.putExtra(DeviceActivity.IQDEVICE, device);
        startActivity(intent);
    }

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

        } catch (InvalidStateException e) {
            // This generally means you forgot to call initialize(), but since
            // we are in the callback for initialize(), this should never happen
        } catch (ServiceUnavailableException e) {
            // This will happen if for some reason your app was not able to connect
            // to the ConnectIQ service running within Garmin Connect Mobile.  This
            // could be because Garmin Connect Mobile is not installed or needs to
            // be upgraded.
            if( null != mEmptyView )
                mEmptyView.setText(R.string.service_unavailable);
        }
    }
}

