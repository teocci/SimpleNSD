package com.github.teocci.simplensd.ui;

import android.content.ContentResolver;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.teocci.simplensd.R;
import com.github.teocci.simplensd.nio.SocketConnection;
import com.github.teocci.simplensd.utils.LogHelper;
import com.github.teocci.simplensd.utils.NsdHelper;

import java.math.BigInteger;
import java.util.Random;

public class NSDActivity extends AppCompatActivity
{
//    static final String TAG = LogHelper.makeLogTag(NSDActivity.class);
//
//    private NsdHelper nsdHelper;
//
//    private TextView statusView;
//    private Handler updateHandler;
//    private SocketConnection connection;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_nsd);
//        statusView = (TextView) findViewById(R.id.status);
//
//        updateHandler = new Handler()
//        {
//            @Override
//            public void handleMessage(Message msg)
//            {
//                String chatLine = msg.getData().getString("msg");
//                addChatLine(chatLine);
//            }
//        };
//
//        connection = new SocketConnection(updateHandler);
//
//        final String deviceID = getDeviceID(getContentResolver());
////        nsdHelper = new NsdHelper(this, deviceID);
////        nsdHelper.initializeNsd();
//    }
//
//    public void clickAdvertise(View v)
//    {
//        // Register service
//        if (connection.getLocalPort() > -1) {
//            nsdHelper.registerService(connection.getLocalPort());
//        } else {
//            LogHelper.d(TAG, "ServerSocket isn't bound.");
//        }
//    }
//
//    public void clickDiscover(View v)
//    {
//        nsdHelper.discoverServices();
//    }
//
//    public void clickConnect(View v)
//    {
//        NsdServiceInfo service = nsdHelper.getChosenServiceInfo();
//        if (service != null) {
//            LogHelper.d(TAG, "Connecting.");
//            connection.connectToServer(service.getHost(),
//                    service.getPort());
//        } else {
//            LogHelper.d(TAG, "No service to connect to!");
//        }
//    }
//
//    public void clickSend(View v)
//    {
//        EditText messageView = (EditText) this.findViewById(R.id.chatInput);
//        if (messageView != null) {
//            String messageString = messageView.getText().toString();
//            if (!messageString.isEmpty()) {
//                connection.sendMessage(messageString);
//            }
//            messageView.setText("");
//        }
//    }
//
//    public void addChatLine(String line)
//    {
//        statusView.append("\n" + line);
//    }
//
//    @Override
//    protected void onPause()
//    {
//        if (nsdHelper != null) {
//            nsdHelper.stopDiscovery();
//        }
//        super.onPause();
//    }
//
//    @Override
//    protected void onResume()
//    {
//        super.onResume();
//        if (nsdHelper != null) {
//            nsdHelper.discoverServices();
//        }
//    }
//
//    @Override
//    protected void onDestroy()
//    {
//        nsdHelper.tearDown();
//        connection.tearDown();
//        super.onDestroy();
//    }
//
//    private static String getDeviceID(ContentResolver contentResolver)
//    {
//        long deviceID = 0;
//        final String str = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID);
//        if (str != null) {
//            try {
//                final BigInteger bi = new BigInteger(str, 16);
//                deviceID = bi.longValue();
//            } catch (final NumberFormatException ex) {
//                /* Nothing critical */
//                LogHelper.i(TAG, ex.toString());
//            }
//        }
//
//        if (deviceID == 0) {
//            /* Let's use random number */
//            deviceID = new Random().nextLong();
//        }
//
//        final byte[] bb = new byte[Long.SIZE / Byte.SIZE];
//        for (int index = (bb.length - 1); index >= 0; index--) {
//            bb[index] = (byte) (deviceID & 0xFF);
//            deviceID >>= Byte.SIZE;
//        }
//
//        return Base64.encodeToString(bb, (Base64.NO_PADDING | Base64.NO_WRAP));
//    }
}
