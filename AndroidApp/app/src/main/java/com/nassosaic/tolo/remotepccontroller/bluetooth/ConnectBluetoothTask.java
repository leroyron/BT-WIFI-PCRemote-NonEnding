package com.nassosaic.tolo.remotepccontroller.bluetooth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.nassosaic.tolo.remotepccontroller.connection.AProfileConnecterActivity;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import static com.nassosaic.tolo.remotepccontroller.EndlessService.builder;
import static com.nassosaic.tolo.remotepccontroller.EndlessService.notificationManager;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appConnected;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.currentContext;

public class ConnectBluetoothTask extends AsyncTask<Profile, OutputStream, OutputStream> {
    private static final String TAG = ConnectBluetoothTask.class.getSimpleName();
    private BluetoothSocket mSocket = null;
    private OutputStream mOutStream = null;
    private AProfileConnecterActivity mConnectionActivity;
    private final static UUID GUID = UUID.fromString("d07c0736-07b9-4ec5-b876-53647c4d047b"); // used in server

    private ProgressDialog mDialog;
    public static final int REQUEST_ENABLE_BT = 1;
    private boolean mWaitingForPrompt = false;

    public ConnectBluetoothTask(AProfileConnecterActivity act) {
        this.mConnectionActivity = act;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (builder != null) {
            builder.setContentText("Establishing Bluetooth connection...");
            notificationManager.notify(1, builder.build());
        }

        if (appPaused || !activityAccessed) return;

        mDialog = new ProgressDialog(mConnectionActivity);
        mDialog.setMessage("Establishing Bluetooth connection...");
        mDialog.show();
    }


    @Override
    protected OutputStream doInBackground(Profile... params) {
        Profile profile = params[0];
        String hostname = profile.getBlutoothName();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                if (appPaused) return null;
                mWaitingForPrompt = true;
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ((Activity) currentContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                BluetoothDevice btDevice = null;
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        Log.v(TAG, device.getName() + " @ " + device.getAddress());
                        // TODO: scan for unpaired devices
                        if (device.getName().equals(hostname)) {
                            btDevice = device;
                            break;
                        }
                    }
                }
                if (btDevice != null) {
                    Log.i(TAG, "Connecting to " + btDevice.getName());
                    try {
                        mSocket = btDevice.createRfcommSocketToServiceRecord(GUID);
                        mSocket.connect(); // This will block until it succeeds or throws an exception
                        mOutStream = mSocket.getOutputStream();
                        return mOutStream;
                    } catch (IOException e) {
                        Log.e(TAG, "BT socket connecting error", e);
                        close();
                    }
                } else {
                    Log.i(TAG, "Bluetooth device not found: " + hostname);
                    close();
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(OutputStream oStream) {
        super.onPostExecute(oStream);
        Log.v(TAG, "PostExecute");
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            if (!mWaitingForPrompt)
                mConnectionActivity.onReceiveConnection(oStream);
        } else if (appPaused || mDialog == null) {
            if (!mWaitingForPrompt)
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
            if (mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "BT socket close error", e);
        }
    }
}