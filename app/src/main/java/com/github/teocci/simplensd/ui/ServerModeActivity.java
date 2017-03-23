package com.github.teocci.simplensd.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;

import com.github.teocci.simplensd.NSDService;
import com.github.teocci.simplensd.R;
import com.github.teocci.simplensd.interfaces.UpdateReceiver;
import com.github.teocci.simplensd.model.StationInfo;
import com.github.teocci.simplensd.utils.Config;
import com.github.teocci.simplensd.utils.LogHelper;

/**
 * Created by teocci on 3/23/17.
 */

public class ServerModeActivity extends AppCompatActivity implements UpdateReceiver
{
    static final String TAG = LogHelper.makeLogTag(ServerModeActivity.class);

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

    public void addChatLine(String line)
    {
        statusView.append("\n" + line);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }

        if (serviceIntent != null) {
            if (isExit)
                stopService(serviceIntent);
            else {
                final Intent intent = new Intent(this, ServerModeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                final PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
                final Notification.Builder notificationBuilder = new Notification.Builder(this);
                notificationBuilder.setContentTitle(getString(R.string.app_name));
                notificationBuilder.setContentText(getString(R.string.running));
                notificationBuilder.setContentIntent(pendingIntent);
                notificationBuilder.setSmallIcon(R.drawable.ic_status);

                final Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                notificationBuilder.setLargeIcon(largeIcon);

                final Notification notification = notificationBuilder.build();
                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0, notification);
            }
            serviceIntent = null;
        }

        LogHelper.i(TAG, "onPause: done");

//        if (nsdHelper != null) {
//            nsdHelper.stopDiscovery();
//        }
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
        serviceIntent.putExtra(Config.EXTRA_OPERATION_MODE, Config.SERVER_MODE);
        final ComponentName componentName = startService(serviceIntent);

        serviceConnection = new ServiceConnection()
        {
            public void onServiceConnected(ComponentName name, IBinder binder)
            {
                LogHelper.d(TAG, "onServiceConnected");
                ServerModeActivity.this.binder = (NSDService.RemoteBinder) binder;
//                ServerModeActivity.this.binder.setStateListener(
//                        ServerModeActivity.this); // ChannelStateListener
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

    @Override
    protected void onDestroy()
    {
//        nsdHelper.tearDown();
//        connection.tearDown();
        super.onDestroy();
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
}
