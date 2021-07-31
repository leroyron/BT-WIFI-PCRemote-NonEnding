package com.nassosaic.tolo.remotepccontroller.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.nassosaic.tolo.remotepccontroller.EndlessService;
import com.nassosaic.tolo.remotepccontroller.MainActivity;
import com.nassosaic.tolo.remotepccontroller.R;
import com.nassosaic.tolo.remotepccontroller.barcodereader.BarcodeActivity;
import com.nassosaic.tolo.remotepccontroller.connection.AProfileConnecterActivity;
import com.google.android.gms.common.api.CommonStatusCodes;

import static com.nassosaic.tolo.remotepccontroller.MainActivity.currentContext;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.PROFILE_ID;
import static com.nassosaic.tolo.remotepccontroller.barcodereader.BarcodeActivity.BarcodeObject;


public class ProfileActivity extends AProfileConnecterActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;

    public static final int EMPTY_ID = -1;
    private int profileId;
    private Profile mProfile;

    public void ProfileActivityInit(Profile mProfile, int profileId) {
        this.mProfile = mProfile;
        this.profileId = profileId;
        this.mConnectionAttempt = 1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        currentContext = this;

        Toolbar profileToolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        setSupportActionBar(profileToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
        else
            throw new AssertionError("getSupportActionBar returned null");

        // Loads bundle data (stored values)
        profileId = getIntent().getIntExtra(PROFILE_ID, EMPTY_ID);

        if (profileId == EMPTY_ID) {
            mProfile = new Profile();
        } else {
            mProfile = new Profile(this, profileId);
            ((EditText)findViewById(R.id.et_wlanname)).setText(mProfile.getWlanName());
            ((EditText)findViewById(R.id.et_wlanport)).setText(String.valueOf(mProfile.getWlanPort()));
            ((EditText)findViewById(R.id.et_blutoothname)).setText(mProfile.getBlutoothName());
            ((RadioGroup)findViewById(R.id.rg_first_priority)).check(mProfile.getFirstPriority());
            ((RadioGroup)findViewById(R.id.rg_second_priority)).check(mProfile.getSecondPriority());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        appPaused = false;
        activityAccessed = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (activityAccessed)
            appPaused = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appPaused = true;
        activityAccessed = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        appPaused = false;
        activityAccessed = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProfile();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case android.R.id.title:
                //TODO:show popup title editor
                Toast.makeText(this, "Title clicked", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void connect(View view) {
        appPaused = false;
        activityAccessed = true;
        saveProfile();
        connect(mProfile);
    }

    private void saveProfile() {                // TODO: button should only be enabled if input is valid
        mProfile.setWlanName(((EditText) findViewById(R.id.et_wlanname)).getText().toString());
        mProfile.setWlanPort(((EditText) findViewById(R.id.et_wlanport)).getText().toString());
        mProfile.setBlutoothName(((EditText) findViewById(R.id.et_blutoothname)).getText().toString());
        mProfile.setFirstPriority(((RadioGroup) findViewById(R.id.rg_first_priority)).getCheckedRadioButtonId());
        mProfile.setSecondPriority(((RadioGroup) findViewById(R.id.rg_second_priority)).getCheckedRadioButtonId());
        mProfile.saveToDatabase(this);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
    }

    //region scan QRcode
    public void scanQRCode(View v) {
        Intent intent = new Intent(this, BarcodeActivity.class);
        ((Activity) this).startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    String barcodeValue = data.getStringExtra(BarcodeObject);
                    Log.d(TAG, "Barcode read: " + barcodeValue);
                    String[] values = decodeValues(barcodeValue);
                    ((EditText)findViewById(R.id.et_wlanname)).setText(values[0]);
                    ((EditText)findViewById(R.id.et_wlanport)).setText(values[1]);
                    ((EditText)findViewById(R.id.et_blutoothname)).setText(values[2]);

                } else {
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Log.e(TAG, "Barcode read error: " + CommonStatusCodes.getStatusCodeString(resultCode));
            }
        }
        else if (requestCode == 1234) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Connection was OK", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_FIRST_USER) {
                Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Broken Connection", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String[] decodeValues(String displayValue) {
        String[] result = displayValue.split(";");
        if (result.length < 3) {
            Log.e(TAG, "Bad QR Code");
            return new String[]{"", "", ""};
        } else {
            return result;
        }

    }
    //endregion

    // Ensures 1st and 2nd connection priorities are not the same
    public void onPriorityChecked(View view) {
        if (view.getId() == R.id.rb_first_priority_wlan)
            if (((RadioButton)findViewById(R.id.rb_second_priority_wlan)).isChecked())
                ((RadioGroup)findViewById(R.id.rg_second_priority)).check(R.id.rb_second_priority_btooth);
        if (view.getId() == R.id.rb_first_priority_btooth)
            if (((RadioButton)findViewById(R.id.rb_second_priority_btooth)).isChecked())
                ((RadioGroup)findViewById(R.id.rg_second_priority)).check(R.id.rb_second_priority_wlan);
        if (view.getId() == R.id.rb_second_priority_wlan)
            if (((RadioButton)findViewById(R.id.rb_first_priority_wlan)).isChecked())
                ((RadioGroup)findViewById(R.id.rg_first_priority)).check(R.id.rb_first_priority_btooth);
        if (view.getId() == R.id.rb_second_priority_btooth)
            if (((RadioButton)findViewById(R.id.rb_first_priority_btooth)).isChecked())
                ((RadioGroup)findViewById(R.id.rg_first_priority)).check(R.id.rb_first_priority_wlan);
    }

}
