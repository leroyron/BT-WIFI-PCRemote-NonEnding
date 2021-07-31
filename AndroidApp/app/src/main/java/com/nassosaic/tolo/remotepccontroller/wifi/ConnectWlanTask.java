package com.nassosaic.tolo.remotepccontroller.wifi;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.nassosaic.tolo.remotepccontroller.connection.AProfileConnecterActivity;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.nassosaic.tolo.remotepccontroller.EndlessService.builder;
import static com.nassosaic.tolo.remotepccontroller.EndlessService.notificationManager;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appConnected;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;

public class ConnectWlanTask extends AsyncTask<Profile, OutputStream, OutputStream> {
    private static final String TAG = ConnectWlanTask.class.getSimpleName();
    private AProfileConnecterActivity mConnectionActivity;
    private Socket mSocket = null;
    private OutputStream mOutStream = null;
    private ProgressDialog mDialog;
    private final int TIMEOUT_MILISECONDS = 3000;

    public ConnectWlanTask(AProfileConnecterActivity act) {
        this.mConnectionActivity = act;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (builder != null) {
            builder.setContentText("Establishing Wi-Fi connection...");
            notificationManager.notify(1, builder.build());
        }

        if (appPaused || !activityAccessed) return;

        mDialog = new ProgressDialog(mConnectionActivity);
        mDialog.setMessage("Establishing WLAN connection...");
        mDialog.show();
    }

    @Override
    protected OutputStream doInBackground(Profile... params) {
        Profile profile = params[0];
        String mServerName = profile.getWlanName();
        int mServerPort = profile.getWlanPort();
        Log.i(TAG, "Connecting to " + mServerName + ":" + mServerPort);
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(mServerName, mServerPort), TIMEOUT_MILISECONDS);
            System.out.println("Connected to " + mServerName + ":" + mServerPort);
            mOutStream = mSocket.getOutputStream();
            return mOutStream;
        } catch (SocketException e) {
            Log.e(TAG, "Socket Exception", e);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                Log.e(TAG, "Unknown host: " + mServerName);
            } else {
                Log.e(TAG, "TCP error", e);
            }
            close();
        }
        return null;
    }

    @Override
    protected void onPostExecute(OutputStream oStream) {
        super.onPostExecute(oStream);
        Log.v(TAG, "PostExecute");
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mConnectionActivity.onReceiveConnection(oStream);
        } else if (appPaused || mDialog == null) {
            mConnectionActivity.onReceiveConnection(oStream);
        } else {
            Log.v(TAG, "BT connection cancelled by user");
            close();
        }
    }

    private void close() {
        appConnected = false;
        try {
            if (mOutStream != null)
                mOutStream.close();
        } catch (IOException e) {
            Log.e(TAG, "BT stream close error", e);
        }
        try {
            if(mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "BT socket close error", e);
        }
    }
}