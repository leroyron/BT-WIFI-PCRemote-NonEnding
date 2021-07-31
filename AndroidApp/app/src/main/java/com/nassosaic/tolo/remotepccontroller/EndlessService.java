package com.nassosaic.tolo.remotepccontroller;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.nassosaic.tolo.remotepccontroller.connection.WindowsKey;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;
import com.nassosaic.tolo.remotepccontroller.profile.ProfileActivity;
import com.nassosaic.tolo.remotepccontroller.connection.ConnectionActivity;

import java.io.IOException;

import static com.nassosaic.tolo.remotepccontroller.MainActivity.currentContext;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appConnected;
import static com.nassosaic.tolo.remotepccontroller.connection.AProfileConnecterActivity.TAG;
import static com.nassosaic.tolo.remotepccontroller.connection.ConnectionActivity.currentProfile;
import static com.nassosaic.tolo.remotepccontroller.connection.ConnectionActivity.outputStream;

public class EndlessService extends Service {
    public MainActivity mainActivity;
    public ConnectionActivity connectionActivity;
    public Profile profile;
    public ProfileActivity aProfileActivity;

    private PowerManager.WakeLock wakeLock;
    private boolean isServiceStarted;

    public Handler autoConnectHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Utils.log("Some component want to bind with the service");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.log("onStartCommand executed with startId: " + startId);
        if (intent != null) {
            String action = intent.getAction();
            action = action == null ? Actions.START.name() : action;
            Utils.log("using an intent with action " + action);
            Actions getAction = Actions.valueOf(action);
            switch (getAction) {
                case START:
                    this.startService();
                    break;
                case STOP:
                    this.stopService();
                    break;
                default:
                    Utils.log("This should never happen. No action in the received intent");
                    break;
            }
        } else {
            Utils.log("with a null intent. It has been probably restarted by the system.");
        }

        return START_STICKY;
    }

    public void onCreate() {
        super.onCreate();
        Utils.log("The service has been created".toUpperCase());
        Notification notification = this.createNotification();
        startForeground(1, notification);
    }

    public void onDestroy() {
        super.onDestroy();
        Utils.log("The service has been destroyed".toUpperCase());
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show();
    }

    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), EndlessService.class);
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Object object = getApplicationContext().getSystemService(Context.ALARM_SERVICE);

        AlarmManager alarmService = (AlarmManager) object;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + (long) 1000, restartServicePendingIntent);
    }

    public static int count = 12;
    public static int countStart = 12;
    public static void countReset (int minus) {
        count = countStart-(minus);
    }

    private final void notifyConnectionTicker() {
        String activityText = "";
        if (activityAccessed) {
            activityText = "Paused...";
        } else if (MainActivity.listView != null && MainActivity.listView.getChildCount() == 0) {
            activityText = "Empty...";
        } else {
            count--;
            if (!appConnected) {
                if (count == countStart-1)
                    initProfile(-1);
                else if (count > 0)
                    activityText = "Auto Connecting: " + count;
                else if (count > -10) // 5 seconds after to restart timer
                    if (count == 0)
                        connectProfile();
                    else if (true)
                        if (count > -5)
                            activityText = "Could not connect";
                        else
                            activityText = "Auto Connecting...";
                    else
                        activityText = "";
                else
                    countReset(0);

            } else {

                if (count > 0)
                    activityText = "Checking Connection: " + count;
                else if (count > -10) // 5 seconds after to restart timer
                    if (count == 0)
                        checkPing();
                    else
                        activityText = "Connected";
                else
                    countReset(0);

            }
        }



        builder.setContentText(activityText);
        notificationManager.notify(1, builder.build());

        if (appPaused) return;
        if (MainActivity.CURRENTTAG == MainActivity.TAG) {
            if (MainActivity.listView.getChildCount() > 0) {
                ((TextView) MainActivity.listView.getChildAt(0).findViewById(R.id.listitem_title))
                        .setTextColor(ContextCompat.getColor(MainActivity.mainContext, R.color.colorAccentRipple));
                ((TextView) MainActivity.listView.getChildAt(0).findViewById(R.id.listitem_subtitle))
                        .setTextColor(ContextCompat.getColor(MainActivity.mainContext, R.color.colorAccentRipple));
            }

            MainActivity.textViewAutoConnect.setText(activityText);
        } else if ((MainActivity.CURRENTTAG == ConnectionActivity.TAG || MainActivity.CURRENTTAG == TAG)) {

            ConnectionActivity.textViewCheckConnect.setText(activityText);
        }
    }

    private void checkPing () {
        if (outputStream != null) {
            byte[] keys = new byte[]{WindowsKey.EMPTY.keyCode, WindowsKey.EMPTY.keyCode};
            new Thread(() -> {
                try {
                    outputStream.write(keys);
                    Log.v(TAG, "Sent bytes: " + keys[0] + ", " + keys[1]);
                } catch (IOException e) {
                    Log.e(TAG, "Write exception", e);
                    outputStream = null;
                    appConnected = false;
                    aProfileActivity.finish();
                    //aProfileActivity = null;
                    // Try to reconnect
                    initProfile(-1);
                    connectProfile();
                }
            }).start();
        } else {
            Log.e(TAG, "Output stream is null");
            outputStream = null;
            appConnected = false;
            aProfileActivity.finish();
            //aProfileActivity = null;
        }
    }

    public void initProfile(int profileId) {
        if (aProfileActivity == null)
            aProfileActivity = new ProfileActivity();
        profile = new Profile(this, profileId);
        aProfileActivity.ProfileActivityInit(profile, profile.id);

    }

    public void connectProfile() {
        aProfileActivity.connect(profile);
    }

    private final void startService() {
        if (!isServiceStarted) {
            Utils.log("Starting the foreground service task");
            Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show();
            isServiceStarted = true;
            ServiceTracker.setServiceState(this, ServiceState.STARTED);
            Object object = this.getSystemService(Context.POWER_SERVICE);

            PowerManager powermanager = (PowerManager) object;
            PowerManager.WakeLock wakelock = powermanager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock");
            wakelock.acquire();
            wakeLock = wakelock;

            // we're starting a loop in a looper
            autoConnectHandler = new Handler(Looper.getMainLooper());
            autoConnectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isServiceStarted) {
                        try {
                            notifyConnectionTicker();
                            Utils.log("Notification Updated. Countdown:" + count);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Utils.log("Notification Update Stopped.");
                        }
                        autoConnectHandler.postDelayed(this, 1000);
                    }
                }
            }, 1000);
        }
    }

    private final void stopService() {
        Utils.log("Stopping the foreground service");
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show();

        try {
            if (wakeLock != null) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }

            stopForeground(true);
            stopSelf();
        } catch (Exception exception) {
            Utils.log("Service stopped without being started: " + exception.getMessage());
        }

        isServiceStarted = false;
        ServiceTracker.setServiceState(this, ServiceState.STOPPED);

        MainActivity.textViewAutoConnect.setText("");
    }

    public static Intent intent;
    public static Notification.Builder builder;
    public static NotificationManager notificationManager;
    public static NotificationChannel notificationchannel;
    public static final String CHANNEL_ID = "AutoNotificationServiceChannel";
    public static final String CHANNEL_NAME = "Auto Start Service Channel Name";

    private final Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Object object = this.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager = (NotificationManager) object;
            notificationchannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(notificationchannel);
        }

        intent = new Intent(this, MainActivity.class);
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, intent, 0);
        PendingIntent pendingIntent = pendingintent;

        builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(
                this,
                CHANNEL_ID
        ) : new Notification.Builder(this);
        Notification notification = builder
                .setContentText("Running...")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_phonelink_ring_black_24dp)
                .setPriority(Notification.PRIORITY_LOW)
                .build();

        return notification;
    }
}
