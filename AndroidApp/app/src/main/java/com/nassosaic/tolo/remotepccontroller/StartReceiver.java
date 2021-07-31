package com.nassosaic.tolo.remotepccontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

//adb shell am broadcast -a com.nassosaic.tolo.remotepccontroller
public final class StartReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED && ServiceTracker.getServiceState(context) == ServiceState.STARTED) {
            intent = new Intent(context, EndlessService.class);
            intent.setAction(Actions.START.name());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Utils.log("Starting the service in >=26 Mode from a BroadcastReceiver");
                Utils.log(context.getApplicationContext().getPackageName());
                Utils.log(context.getPackageName());
                context.startForegroundService(intent);
                return;
            }

            Utils.log("Starting the service in < 26 Mode from a BroadcastReceiver");
            context.startService(intent);
        }
    }
}
