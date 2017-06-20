package com.github.teocci.simplensd;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Base64;

import com.github.teocci.simplensd.interfaces.ConnectionUpdateListener;
import com.github.teocci.simplensd.interfaces.ServiceResolveListener;
import com.github.teocci.simplensd.utils.Config;
import com.github.teocci.simplensd.utils.LogHelper;
import com.github.teocci.simplensd.utils.NsdHelper;

import java.math.BigInteger;
import java.util.Random;

public class NSDService extends Service implements ServiceResolveListener
{
    private static final String TAG = LogHelper.makeLogTag(NSDService.class);

    private NsdHelper nsdHelper;

    private final RemoteBinder serviceBinder;
    public NSDService()
    {
        serviceBinder = new RemoteBinder();
    }

    public void onCreate()
    {
        super.onCreate();
        LogHelper.d(TAG, "onCreate");
        final String deviceID = getDeviceID(getContentResolver());
        nsdHelper = new NsdHelper(this, deviceID);
    }

    public IBinder onBind(Intent intent)
    {
        LogHelper.d(TAG, "onBind");
        return serviceBinder;
    }

    public boolean onUnbind(Intent intent)
    {
        LogHelper.d(TAG, "onUnbind");
//        channel.setChannelStateListener(null);
        return false;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        LogHelper.d(TAG, "onStartCommand: flags=" + flags + " startId=" + startId);

        final Boolean isServer = intent.getStringExtra(Config.EXTRA_OPERATION_MODE).equals(Config.SERVER_MODE);

        if (isServer) {
            nsdHelper.initializeNSDServer();
        } else {
            nsdHelper.initializeNSDClient();
            nsdHelper.setServiceResolveListener(this);
            nsdHelper.discoverServices();
        }

//        if (audioRecorder == null) {
//            final String deviceID = getDeviceID(getContentResolver());
//
//            final SessionManager sessionManager = new SessionManager();
//            final AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//            audioRecorder = AudioRecorder.create(audioManager, sessionManager, false); // repeat : false
//
//            if (audioRecorder != null) {
//                startForeground(0, null);
//
//                final int audioStream = Config.AUDIO_STREAM;
//                previousVolume = audioManager.getStreamVolume(audioStream);
//
//                final String stationName = intent.getStringExtra(MainActivity.KEY_STATION_NAME);
//                int audioVolume = intent.getIntExtra(MainActivity.KEY_VOLUME, -1);
//                if (audioVolume < 0)
//                    audioVolume = audioManager.getStreamMaxVolume(audioStream) * 2 / 3;
//                LogHelper.d(TAG, "setStreamVolume(" + audioStream + ", " + audioVolume + ")");
//                audioManager.setStreamVolume(audioStream, audioVolume, 0);
//                final Boolean isServer = intent.getStringExtra(OPERATION_MODE).equals(ServerModeActivity.KEY_SERVER_MODE);
//                try {
//                    collider = Collider.create();
//                    colliderThread = new ColliderThread(collider);
//
//                    final TimerQueue timerQueue = new TimerQueue(collider.getThreadPool());
//
//                    channel = new Channel(
//                            deviceID,
//                            stationName,
//                            audioRecorder.getAudioFormat(),
//                            collider,
//                            nsdManager,
//                            SERVICE_TYPE,
//                            SERVICE_NAME,
//                            sessionManager,
//                            timerQueue,
//                            Config.PING_INTERVAL,
//                            isServer
//                    );
//
//                    // Instantiate a new DiscoveryListener
//                    // discoveryListener = new DiscoveryListener();
//                    initDiscoveryListener();
//                    nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
//                    colliderThread.start();
//                } catch (final IOException ex) {
//                    LogHelper.w(TAG, ex.toString());
//                }
//            }
//        }

        return START_REDELIVER_INTENT;
    }

    public void onDestroy()
    {
        LogHelper.d(TAG, "onDestroy");

        nsdHelper.stopDiscovery();
        nsdHelper.tearDown();
//        connection.tearDown();

        LogHelper.d(TAG, "onDestroy: done");
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
    public void onServiceResolved(NsdServiceInfo serviceInfo)
    {

    }

    @Override
    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
    {
        LogHelper.e(TAG, "clickConnect()");
        if (serviceInfo != null) {
            LogHelper.d(TAG, "Connecting.");
            connection.connectToServer(serviceInfo.getHost(), serviceInfo.getPort());
            LogHelper.d(TAG, "Connected");
        } else {
            LogHelper.d(TAG, "No service to connect to!");
        }
    }

    /**
     * RemoteBinder Class should be used for the client Binder.  Because we know this service always
     * runs in the same process as its clients.
     */
    public class RemoteBinder extends Binder
    {
        public NSDService getService()
        {
            // Return this instance of LocalService so clients can call public methods
            return NSDService.this;
        }

        public void setUpdateReceiver(ConnectionUpdateListener connectionUpdateListener)
        {
            nsdHelper.setConnectionUpdateListener(connectionUpdateListener);
        }

        public void sendMessage(String message)
        {
            nsdHelper.sendMessage(message);
        }

//        public void setStationName(String stationName)
//        {
//            setStationName(stationName);
//        }

//        public void setStateListener(ServiceStateListener stateListener, ChannelStateListener channelStateListener)
//        {
//            stateListener.onInit(audioRecorder);
//            channel.setChannelStateListener(channelStateListener);
//        }
//
//        public void setStateListener(ChannelStateListener channelStateListener)
//        {
//            channel.setChannelStateListener(channelStateListener);
//        }
//
//        public void setStationName(String stationName)
//        {
//            channel.setStationName(stationName);
//        }
    }
}
