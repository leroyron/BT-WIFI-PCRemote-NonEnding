package com.nassosaic.tolo.remotepccontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public final class ServiceTracker {
    private static final String name = "SPYSERVICE_KEY";
    private static final String key = "SPYSERVICE_STATE";

    public static final void setServiceState(Context context, ServiceState state) {
        Utils.log(context.getApplicationContext().getPackageName());
        Utils.log(context.getPackageName());
        SharedPreferences sharedPrefs = getPreferences(context);
        Editor editor = sharedPrefs.edit();
        editor.putString("SPYSERVICE_STATE", state.name());
        editor.apply();
    }

    public static final ServiceState getServiceState(Context context) {
        SharedPreferences sharedPrefs = getPreferences(context);
        String value = sharedPrefs.getString("SPYSERVICE_STATE", ServiceState.STOPPED.name());
        return ServiceState.valueOf(value);
    }

    private static final SharedPreferences getPreferences(Context context) {
        SharedPreferences sharedpreferences = context.getSharedPreferences("SPYSERVICE_KEY", 0);
        return sharedpreferences;
    }
}