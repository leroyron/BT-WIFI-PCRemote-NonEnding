package com.nassosaic.tolo.remotepccontroller.connection;

import java.io.OutputStream;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nassosaic.tolo.remotepccontroller.R;
import com.nassosaic.tolo.remotepccontroller.bluetooth.ConnectBluetoothTask;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;
import com.nassosaic.tolo.remotepccontroller.wifi.ConnectWlanTask;

import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appConnected;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.bluetooth.ConnectBluetoothTask.REQUEST_ENABLE_BT;

public abstract class AProfileConnecterActivity extends AppCompatActivity {
    public static final String TAG = AProfileConnecterActivity.class.getSimpleName();
    private ConnectBluetoothTask mConnectBluetoothTask = null;
    private ConnectWlanTask mConnectWlanTask = null;
    private Profile mProfile;
    public int mConnectionAttempt;

    public void connect(Profile profile) {
        mProfile = profile;
        mConnectionAttempt = 1;
        connect();
    }

    private void connect() {
        if ((mConnectionAttempt == 1) && (mProfile.getFirstPriority() == R.id.rb_first_priority_wlan))
            connectWlan(mProfile);
        else if ((mConnectionAttempt == 1) && (mProfile.getFirstPriority() == R.id.rb_first_priority_btooth))
            connectBluetooth(mProfile);
        else if ((mConnectionAttempt == 2) && (mProfile.getSecondPriority() == R.id.rb_second_priority_wlan))
            connectWlan(mProfile);
        else if ((mConnectionAttempt == 2) && (mProfile.getSecondPriority() == R.id.rb_second_priority_btooth))
            connectBluetooth(mProfile);
        else {
            Log.e(TAG, "Could not connect");
            if (!appPaused && activityAccessed)
                Toast.makeText(this, R.string.could_not_connect, Toast.LENGTH_SHORT).show();
            if (this instanceof ConnectionActivity) {
                finish();
            }
        }
    }

    private void connectWlan(Profile profile) {
        mConnectWlanTask = new ConnectWlanTask(this);
        mConnectWlanTask.execute(profile);
    }

    private void connectBluetooth(Profile profile) {
        mConnectBluetoothTask = new ConnectBluetoothTask(this);
        mConnectBluetoothTask.execute(profile);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Log.i(TAG, "Bluetooth enabled");
            connect();
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            Log.i(TAG, "Bluetooth canceled");
            mConnectionAttempt++;
            connect();
        } else {
            Log.i(TAG, "Unknown activity result");
        }
    }

    public void onReceiveConnection(OutputStream oStream) {
        if (oStream != null) {
            appConnected = true;
            Log.v(TAG, "Asynctask stream received");
            ConnectionActivity.outputStream = oStream;
            ConnectionActivity.currentProfile = mProfile;

            if (appPaused || !activityAccessed) return;

            Toast.makeText(this, R.string.connected, Toast.LENGTH_SHORT).show();

            if (!(this instanceof ConnectionActivity)) {
                Intent intent = new Intent(this, ConnectionActivity.class);
                startActivity(intent);
            }
        } else {
            appConnected = false;
            Log.v(TAG, "Asynctask no stream");
            mConnectionAttempt++;
            connect();
        }
    }
}
