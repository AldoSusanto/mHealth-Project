package com.mhealth.application;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class NotificationService extends Service {
    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                Timer t = new Timer();
                t.scheduleAtFixedRate(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        if(!SyncService.isApplicationRunning())
                            sendNotification();
                    }
                },0,2*60*1000);
            }
        }.start();

        return Service.START_NOT_STICKY;
    }

    public void sendNotification()
    {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this).
                        setSmallIcon(R.mipmap.ic_launcher).
                        setContentTitle("Watch App Offline").
                        setContentText("Please run the watch app so a connection can be established.");
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0,mBuilder.build());
    }
}
