package com.nassosaic.tolo.remotepccontroller.connection;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.nassosaic.tolo.remotepccontroller.EndlessService;
import com.nassosaic.tolo.remotepccontroller.EditTextCustom;
import com.nassosaic.tolo.remotepccontroller.R;
import com.nassosaic.tolo.remotepccontroller.profile.Profile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.nassosaic.tolo.remotepccontroller.MainActivity.activityAccessed;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.appPaused;
import static com.nassosaic.tolo.remotepccontroller.MainActivity.CURRENTTAG;

public class ConnectionActivity extends AProfileConnecterActivity {
    public static final String TAG = ConnectionActivity.class.getSimpleName();
    private boolean screenDimmed = false;
    public static OutputStream outputStream;
    public static Profile currentProfile;
    private PowerManager.WakeLock mWakeLock; //drains battery
    private FrameLayout mMainLayout;
    private View mPresentationView;
    private View mMediaView;
    private boolean isPresentationLayout = true;

    public static TextView textViewCheckConnect;

    private EditTextCustom inputChar;
    private boolean inputLock = false;
    private int keyWait = 20;
    private String buffString = "";

    ExecutorService myExecutor = Executors.newCachedThreadPool(); //or whatever
    private Runnable myThread;
    private int sleepMms = 500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        mPresentationView = LayoutInflater.from(this).inflate(R.layout.fragment_presentation, null, false);
        mMainLayout = findViewById(R.id.connection_activity);
        mMainLayout.addView(mPresentationView);

        inputChar = findViewById(R.id.inputText);
        inputChar.setImeOptions(0x00000004);
        inputChar.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId==0x00000004){
                sleepMms = 500;
                keyWait = 1;

                buffString += inputChar.getText().toString();
                inputChar.getText().clear();
                inputChar.clearFocus();
                if (!inputLock) charBufferString();
            }
            return false;
        });
        inputChar.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                if (inputLock) {
                    inputChar.setText(buffString);
                    buffString = "";
                }
            }
        });

        Toolbar connectionToolbar = findViewById(R.id.connection_toolbar);
        setSupportActionBar(connectionToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(true);
        else
            throw new AssertionError("getSupportActionBar returned null");
    }

    private void charBufferString () {
        inputLock = true;

        keyWait--;
        if (keyWait > 0) { writeChar(WindowsKey.EMPTY); return; };

        if (buffString.length() == 0) {
            inputLock = false;
            inputChar.setHint("<<< Type");
            return;
        }

        String buffStringChar = buffString.substring(0, 1);
        buffString = buffString.substring(1);
        inputChar.setHint(buffString);
        Log.e(TAG, buffStringChar);
        onChar(buffStringChar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.connection_menu, menu);
        menu.findItem(R.id.action_presentation_layout).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    public void onButtonPing() {
        write(WindowsKey.EMPTY);
    }

    private void write(WindowsKey keyPress) {
        write(WindowsKey.EMPTY, keyPress);
    }

    private void write(WindowsKey keyDown, WindowsKey keyPress) {
        if (outputStream != null) {
            byte[] keys = new byte[]{keyDown.keyCode, keyPress.keyCode};
            myExecutor.execute( () -> {
                try {
                    outputStream.write(keys);
                    Log.v(TAG, "Sent bytes: " + keys[0] + ", " + keys[1]);
                } catch (IOException e) {
                    Log.e(TAG, "Write exception", e);
                    outputStream = null;
                    // Try to reconnect
                    connect(currentProfile);
                }
            });
        } else {
            Log.e(TAG, "Output stream is null");
            finish();
        }
    }

    private void write(WindowsKey keyDown1, WindowsKey keyDown2, WindowsKey keyPress) {
        if (outputStream != null) {
            byte[] keys = new byte[]{keyDown1.keyCode, keyDown2.keyCode, keyPress.keyCode};
            myExecutor.execute( () -> {
                try {
                    outputStream.write(keys);
                    Log.v(TAG, "Sent bytes: " + keys[0] + ", " + keys[1] + ", " + keys[2]);
                } catch (IOException e) {
                    Log.e(TAG, "Write exception", e);
                    outputStream = null;
                    // Try to reconnect
                    connect(currentProfile);
                }
            });
        } else {
            Log.e(TAG, "Output stream is null");
            finish();
        }
    }

    private void writeChar(WindowsKey keyPress) {
        writeChar(WindowsKey.EMPTY, keyPress);
    }

    private void writeChar(WindowsKey keyDown, WindowsKey keyPress) {
        if (outputStream != null) {
            byte[] keys = new byte[]{keyDown.keyCode, keyPress.keyCode};
            myExecutor.execute( myThread = () -> {
                try {
                    Thread.sleep(sleepMms = sleepMms > 50 ? sleepMms-50 : 50);
                    outputStream.write(keys);
                    Log.v(TAG, "Sent bytes: " + keys[0] + ", " + keys[1]);
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Write exception", e);
                    outputStream = null;
                    // Try to reconnect
                    connect(currentProfile);
                } finally {
                    charBufferString();
                }
            });
        } else {
            Log.e(TAG, "Output stream is null");
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        appPaused = true;
        if (mWakeLock.isHeld())
            mWakeLock.release();
    }

    @Override
    public void onResume() {
        super.onResume();
        appPaused = false;
        activityAccessed = true;
        CURRENTTAG = TAG;
        EndlessService.countReset(-1);
        if (!mWakeLock.isHeld() && isPresentationLayout)
            mWakeLock.acquire();
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
        appPaused = true;
        activityAccessed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing stream");
            }
            outputStream = null;
        }
    }

    public void onDimScreenClicked(MenuItem item) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (screenDimmed) {
            params.screenBrightness = -1;
            screenDimmed = false;
        } else {
            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            params.screenBrightness = 0;
            screenDimmed = true;
        }
        getWindow().setAttributes(params);
        item.setChecked(screenDimmed);
    }

    public void setPresentationLayout(MenuItem item) {
        if (!isPresentationLayout) {
            mMainLayout.removeView(mMediaView);
            mMainLayout.addView(mPresentationView);
            item.setChecked(true);
            if (!mWakeLock.isHeld())
                mWakeLock.acquire();
            isPresentationLayout = true;
        }
    }

    public void setMediaLayout(MenuItem item) {
        if (isPresentationLayout) {
            if (mMediaView == null)
                mMediaView = LayoutInflater.from(this).inflate(R.layout.fragment_media, null, false);
            mMainLayout.removeView(mPresentationView);
            mMainLayout.addView(mMediaView);
            item.setChecked(true);
            if (mWakeLock.isHeld())
                mWakeLock.release();
            isPresentationLayout = false;
        }
    }

    //region key mapping
    //
    public void onChar(String s) {
        if (s.matches("[A-Za-z0-9]+"))
            if (Character.isUpperCase(s.charAt(0)))
                writeChar(WindowsKey.SHIFT, WindowsKey.valueOf("VK_"+s.toUpperCase()));
            else
                writeChar(WindowsKey.valueOf("VK_"+s.toUpperCase()));
        else if (s.matches("[- !@#$%^&*()\\\\_+=[{]}|;:'\",<.>/?`~(\r\n|\r|\n)]+"))
        switch (s) {
            case " ":
                writeChar(WindowsKey.SPACE);
                break;
            case "\n":
            case "\r\n":
            case "\r":
                buffString = "\\r\\n"+buffString;
                writeChar(WindowsKey.EMPTY);
                break;
            case "!":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_1);
                break;
            case "@":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_2);
                break;
            case "#":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_3);
                break;
            case "$":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_4);
                break;
            case "%":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_5);
                break;
            case "^":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_6);
                break;
            case "&":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_7);
                break;
            case "*":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_8);
                break;
            case "(":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_9);
                break;
            case ")":
                writeChar(WindowsKey.SHIFT, WindowsKey.VK_0);
                break;
            case "-":
                writeChar(WindowsKey.OEM_MINUS);
                break;
            case "_":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_MINUS);
                break;
            case "=":
                writeChar(WindowsKey.OEM_PLUS);
                break;
            case "+":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_PLUS);
                break;
            case "[":
                writeChar(WindowsKey.OEM_4);
                break;
            case "{":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_4);
                break;
            case "]":
                writeChar(WindowsKey.OEM_6);
                break;
            case "}":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_6);
                break;
            case "\\":
                writeChar(WindowsKey.OEM_5);
                break;
            case "|":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_5);
                break;
            case ";":
                writeChar(WindowsKey.OEM_1);
                break;
            case ":":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_1);
                break;
            case "'":
                writeChar(WindowsKey.OEM_7);
                break;
            case "\"":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_7);
                break;
            case ",":
                writeChar(WindowsKey.OEM_COMMA);
                break;
            case "<":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_COMMA);
                break;
            case ".":
                writeChar(WindowsKey.OEM_PERIOD);
                break;
            case ">":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_PERIOD);
                break;
            case "/":
                writeChar(WindowsKey.OEM_2);
                break;
            case "?":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_2);
                break;
            case "`":
                writeChar(WindowsKey.OEM_3);
                break;
            case "~":
                writeChar(WindowsKey.SHIFT, WindowsKey.OEM_3);
                break;
            default:
                Log.e(TAG, "Invalid key: " + s);
                break;
        }
        else
            writeChar(WindowsKey.EMPTY);

    }

    // Buttons are mapped to command bytes by their IDs
    public void onButtonDown(View v) {
        if (inputLock) {
            inputChar.setText(buffString);
            buffString = "";
        }
        switch (v.getId()) {
            case R.id.up:
                write(WindowsKey.UP);
                break;
            case R.id.down:
                write(WindowsKey.DOWN);
                break;
            case R.id.left:
                write(WindowsKey.LEFT);
                break;
            case R.id.right:
                write(WindowsKey.RIGHT);
                break;
            case R.id.spacebar:
                write(WindowsKey.SPACE);
                break;
            case R.id.page_up:
                write(WindowsKey.PRIOR);
                break;
            case R.id.page_down:
                write(WindowsKey.NEXT);
                break;
            case R.id.win:
                write(WindowsKey.LWIN);
                break;
            case R.id.tab:
                write(WindowsKey.TAB);
                break;
            case R.id.enter:
                write(WindowsKey.RETURN);
                break;
            case R.id.shift_tab:
                write(WindowsKey.SHIFT, WindowsKey.TAB);
                break;
            case R.id.win_shift_k:
                write(WindowsKey.LWIN, WindowsKey.SHIFT, WindowsKey.VK_K);
                break;
            case R.id.win_tab:
                write(WindowsKey.LWIN, WindowsKey.TAB);
                break;
            case R.id.media_pause:
                write(WindowsKey.MEDIA_PLAY_PAUSE);
                break;
            case R.id.media_next:
                write(WindowsKey.MEDIA_NEXT_TRACK);
                break;
            case R.id.media_previous:
                write(WindowsKey.MEDIA_PREV_TRACK);
                break;
            case R.id.volume_mute:
                write(WindowsKey.VOLUME_MUTE);
                break;
            case R.id.volume_up:
                write(WindowsKey.VOLUME_UP);
                break;
            case R.id.volume_down:
                write(WindowsKey.VOLUME_DOWN);
                break;
            default:
                Log.e(TAG, "Invalid key: " + v.toString());
                break;
        }
    }

    // Volume keys are mapped to arrows:
    //   volume down -> left arrow
    //   volume up   -> right arrow
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            write(WindowsKey.BACK);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            write(WindowsKey.LEFT);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            write(WindowsKey.RIGHT);
        } else {
            return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    // Disables volume key sound
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        /*if (keyCode == KeyEvent.KEYCODE_DEL) {

        }*/
        return super.onKeyUp(keyCode, event);
    }

    //
    //endregion key mapping
}

