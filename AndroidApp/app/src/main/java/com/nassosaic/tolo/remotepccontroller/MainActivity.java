package com.nassosaic.tolo.remotepccontroller;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.database.Cursor;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.CursorAdapter;

import com.nassosaic.tolo.remotepccontroller.connection.AProfileConnecterActivity;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;
import com.nassosaic.tolo.remotepccontroller.profile.ProfileActivity;
import com.nassosaic.tolo.remotepccontroller.profile.ProfileDataDbHelper;

import static com.nassosaic.tolo.remotepccontroller.profile.ProfileData.ProfileEntry._ID;
import static com.nassosaic.tolo.remotepccontroller.profile.ProfileData.ProfileEntry.COLUMN_NAME_BLUETOOTHNAME;
import static com.nassosaic.tolo.remotepccontroller.profile.ProfileData.ProfileEntry.COLUMN_NAME_WLANNAME;
import static com.nassosaic.tolo.remotepccontroller.profile.ProfileData.ProfileEntry.COLUMN_NAME_WLANPORT;

public final class MainActivity extends AProfileConnecterActivity/*AppCompatActivity*/ {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String PROFILE_ID = "ProfileID";
    public static String CURRENTTAG;
    private ProfileCursorAdapter mProfileAdapter;
    private ProfileDataDbHelper mDbHelper;
    public static boolean appConnected, activityAccessed = false;
    public static boolean appPaused = true;

    public static Context mainContext;
    public static Context currentContext;
    public static TextView textViewAutoConnect;
    public static ListView listView;

    private boolean autoStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.setTitle("Endless Service");

        this.mainContext = this.currentContext = this;

        mDbHelper = new ProfileDataDbHelper(this);
        // bind profile data to listView
        mProfileAdapter = new ProfileCursorAdapter(this, mDbHelper.queryAllProfiles());

        listView = findViewById(R.id.list_view);
        listView.setAdapter(mProfileAdapter);

        textViewAutoConnect = findViewById(R.id.textViewAutoConnect);
        textViewAutoConnect.setTextColor(ContextCompat.getColor(this, R.color.colorAccentRipple));

        final Button startButton = (Button) findViewById(R.id.btnStartService);
        startButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Utils.log("START THE FOREGROUND SERVICE ON DEMAND");
                actionOnService(Actions.START);
            }
        });

        final Button stopButton = (Button) findViewById(R.id.btnStopService);
        stopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Utils.log("STOP THE FOREGROUND SERVICE ON DEMAND");
                actionOnService(Actions.STOP);
            }
        });
    }

    private final void actionOnService(Actions action) {
        if (ServiceTracker.getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP)
            return;

        Intent intent = new Intent(this, EndlessService.class);
        intent.setAction(action.name());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.log("Starting the service in >=26 Mode");
            this.startForegroundService(intent);
            return;
        }

        Utils.log("Starting the service in < 26 Mode");
        this.startService(intent);

    }


    @Override
    public void onStart() {
        super.onStart();
        activityAccessed = appPaused = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!activityAccessed)
            appPaused = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        activityAccessed = appPaused = false;
        CURRENTTAG = TAG;
        EndlessService.countReset(-1);
        mProfileAdapter.changeCursor(mDbHelper.queryAllProfiles());
        mProfileAdapter.notifyDataSetChanged();
        if (!appPaused)
            resetColor();
    }

    private void resetColor() {
        for (int i = 0; i < listView.getChildCount(); i++) {
            ((TextView) listView.getChildAt(i).findViewById(R.id.listitem_title)).setTextColor(Color.parseColor("#000000"));
            ((TextView) listView.getChildAt(i).findViewById(R.id.listitem_subtitle)).setTextColor(Color.parseColor("#000000"));
            EndlessService.countReset(-1);
        }
    }

    public void connectProfile(int profileId) {
        Profile profile = new Profile(this, profileId);
        connect(profile);
    }

    private void editProfile(int profileId) {
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(PROFILE_ID, profileId);
        startActivity(intent);
    }

    public void addProfile(View v) {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
    }

    private void deleteProfile(final long profileId) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(R.string.confirm_delete_profile);
        alertDialogBuilder
                //.setMessage("All data will be lost!")
                .setCancelable(false)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "Deleting profile...");
                        mDbHelper.deleteProfile(profileId);
                        mProfileAdapter.changeCursor(mDbHelper.queryAllProfiles());
                        mProfileAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, R.string.profile_deleted, Toast.LENGTH_SHORT).show(); //TODO: Snackbar with undo
                        resetColor();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        resetColor();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public class ProfileCursorAdapter extends CursorAdapter {
        private ProfileCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.main_activity_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Profile name and information
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow(_ID));
            final String wlanName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_WLANNAME));
            final int wlanPort = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_WLANPORT));
            final String bluetoothName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_BLUETOOTHNAME));
            //final int firstPriority = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_FIRST_PRIORITY));
            //final int secondPriority = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_SECOND_PRIORITY));

            ((TextView) view.findViewById(R.id.listitem_title)).setText(wlanName + ":" + wlanPort);
            ((TextView) view.findViewById(R.id.listitem_subtitle)).setText(bluetoothName);

            // Short click actions
            View.OnClickListener mOnItemIconClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    connectProfile(id);
                }
            };
            View.OnClickListener mOnItemTextClick = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    editProfile(id);
                }
            };
            view.findViewById(R.id.listitem_connect).setOnClickListener(mOnItemIconClick);
            view.findViewById(R.id.listitem_title).setOnClickListener(mOnItemTextClick);
            view.findViewById(R.id.listitem_subtitle).setOnClickListener(mOnItemTextClick);

            // Long click actions
            View.OnLongClickListener mOnItemLongClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    deleteProfile(id);
                    return false;
                }
            };
            view.findViewById(R.id.listitem_connect).setOnLongClickListener(mOnItemLongClick);
            view.findViewById(R.id.listitem_title).setOnLongClickListener(mOnItemLongClick);
            view.findViewById(R.id.listitem_subtitle).setOnLongClickListener(mOnItemLongClick);

        }
    }
}


