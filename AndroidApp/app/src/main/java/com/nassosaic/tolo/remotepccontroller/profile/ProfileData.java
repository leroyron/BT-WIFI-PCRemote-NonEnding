package com.nassosaic.tolo.remotepccontroller.profile;

import android.provider.BaseColumns;

import com.nassosaic.tolo.remotepccontroller.EndlessService;

public final class ProfileData extends EndlessService {

    private ProfileData() {}

    public static class ProfileEntry implements BaseColumns {
        public static final String TABLE_NAME = "Profile";
        public static final String COLUMN_NAME_WLANNAME = "WlanName";
        public static final String COLUMN_NAME_WLANPORT = "WlanPort";
        public static final String COLUMN_NAME_BLUETOOTHNAME = "BluetoothName";
        public static final String COLUMN_NAME_FIRST_PRIORITY = "FirstPriority";
        public static final String COLUMN_NAME_SECOND_PRIORITY = "SecondPriority";
        public static final String[] ALL_COLUMNS = {
                _ID,
                COLUMN_NAME_WLANNAME,
                COLUMN_NAME_WLANPORT,
                COLUMN_NAME_BLUETOOTHNAME,
                COLUMN_NAME_FIRST_PRIORITY,
                COLUMN_NAME_SECOND_PRIORITY
        };
    }

}
