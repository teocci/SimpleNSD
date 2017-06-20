package com.github.teocci.simplensd.ui;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.github.teocci.simplensd.NSDService;
import com.github.teocci.simplensd.R;
import com.github.teocci.simplensd.interfaces.ConnectionUpdateListener;
import com.github.teocci.simplensd.model.StationInfo;
import com.github.teocci.simplensd.utils.Config;
import com.github.teocci.simplensd.utils.LogHelper;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by teocci on 3/23/17.
 */

public class ClientModeActivity extends AppCompatActivity implements ConnectionUpdateListener
{
    static final String TAG = LogHelper.makeLogTag(ClientModeActivity.class);

    private TextView statusView;

    private boolean isExit;
    private Intent serviceIntent;
    private ServiceConnection serviceConnection;
    private NSDService.RemoteBinder binder;

    private String stationName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_nsd);
        statusView = (TextView) findViewById(R.id.status);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        isExit = false;

        final SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        stationName = sharedPreferences.getString(Config.KEY_STATION_NAME, null);
        if ((stationName == null) || stationName.isEmpty())
            stationName = Build.MODEL;

        if (!stationName.isEmpty()) {
            final String title = getString(R.string.app_name) + ": " + stationName;
            setTitle(title);
        }

        serviceIntent = new Intent(this, NSDService.class);
        serviceIntent.putExtra(Config.EXTRA_DISPLAY_NAME, stationName);
        serviceIntent.putExtra(Config.EXTRA_OPERATION_MODE, Config.CLIENT_MODE);
        final ComponentName componentName = startService(serviceIntent);

        serviceConnection = new ServiceConnection()
        {
            public void onServiceConnected(ComponentName name, IBinder binder)
            {
                LogHelper.d(TAG, "onServiceConnected");
                ClientModeActivity.this.binder = (NSDService.RemoteBinder) binder;
                ClientModeActivity.this.binder.setUpdateReceiver(
                        ClientModeActivity.this); // ConnectionUpdateListener
            }

            public void onServiceDisconnected(ComponentName name)
            {
                LogHelper.d(TAG, "onServiceDisconnected");
            }
        };

        final boolean bindRC = bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        if (!bindRC)
            serviceConnection = null;
        LogHelper.d(TAG, "componentName=" + componentName + " bindRC=" + bindRC);

//        if (nsdHelper != null) {
//            nsdHelper.discoverServices();
//        }
    }

    public void clickSend(View v)
    {
        EditText messageView = (EditText) this.findViewById(R.id.chatInput);
        if (messageView != null) {
            String messageString = messageView.getText().toString();
            if (!messageString.isEmpty()) {
                binder.sendMessage(messageString);
            }
            messageView.setText("");
        }
    }

    @Override
    protected void onPause()
    {
//        if (nsdHelper != null) {
//            nsdHelper.stopDiscovery();
//        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    private static String getDeviceID(ContentResolver contentResolver)
    {
        long deviceID = 0;
        final String str = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
        if (str != null) {
            try {
                final BigInteger bi = new BigInteger(str, 16);
                deviceID = bi.longValue();
            } catch (final NumberFormatException ex) {
                /* Nothing critical */
                LogHelper.i(TAG, ex.toString());
            }
        }

        if (deviceID == 0) {
            /* Let's use random number */
            deviceID = new Random().nextLong();
        }

        final byte[] bb = new byte[Long.SIZE / Byte.SIZE];
        for (int index = (bb.length - 1); index >= 0; index--) {
            bb[index] = (byte) (deviceID & 0xFF);
            deviceID >>= Byte.SIZE;
        }

        return Base64.encodeToString(bb, (Base64.NO_PADDING | Base64.NO_WRAP));
    }

    @Override
    public void onStationListChanged(StationInfo[] stationInfo)
    {

    }

    @Override
    public void onMessageUpdate(String message)
    {
        addChatLine(message);
    }

    public void addChatLine(String line)
    {
        statusView.append("\n" + line);
    }
}
