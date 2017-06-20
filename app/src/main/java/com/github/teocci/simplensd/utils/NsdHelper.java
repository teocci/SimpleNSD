package com.github.teocci.simplensd.utils;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;

import com.github.teocci.simplensd.BuildConfig;
import com.github.teocci.simplensd.interfaces.ConnectionUpdateListener;
import com.github.teocci.simplensd.interfaces.ServiceResolveListener;
import com.github.teocci.simplensd.nio.SocketConnection;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by teocci on 3/22/17.
 */

public class NsdHelper
{
    static final String TAG = LogHelper.makeLogTag(NsdHelper.class);

    private Context context;

    private NsdManager nsdManager;

    private NsdManager.ResolveListener resolveListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    private NsdServiceInfo serviceInfo;

    private ServiceResolveListener serviceResolveListener;
    private ConnectionUpdateListener connectionUpdateListener;


    private final ReentrantLock reentrantLock;
    private Condition condition;
    private CountDownLatch latchSynchronizer;

    private Handler updateHandler;
    private SocketConnection connection;

    private boolean isDiscoveryStarted;

    public String serviceName = Config.SERVICE_NAME;
    private final String deviceID;

//    private boolean isDiscoveryStarted = false;

    public NsdHelper(Context context, String deviceID)
    {
        this.context = context;
        this.deviceID = deviceID;
        this.reentrantLock = new ReentrantLock();
        this.isDiscoveryStarted = false;

        this.serviceResolveListener = null;
        this.connectionUpdateListener = null;

        this.connection = new SocketConnection(updateHandler);
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNSDServer()
    {
        initializeRegistrationListener();
        // Register service
        if (connection.getLocalPort() > -1) {
            registerService(connection.getLocalPort());
            LogHelper.e(TAG, "RegisterService()");
        } else {
            LogHelper.e(TAG, "ServerSocket isn't bound.");
        }
        initializeHandler();
    }

    public void initializeNSDClient()
    {
        initializeDiscoveryListener();
        initializeResolveListener();
        initializeHandler();
    }

    public void initializeHandler()
    {
        updateHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                String chatLine = msg.getData().getString("msg");
                if (connectionUpdateListener != null) {
                    connectionUpdateListener.onMessageUpdate(chatLine);
                }
            }
        };
    }

    public void initializeDiscoveryListener()
    {
        discoveryListener = new NsdManager.DiscoveryListener()
        {
            @Override
            public void onDiscoveryStarted(String regType)
            {
                LogHelper.e(TAG, "Service discovery started");
                reentrantLock.lock();
                try {
                    if (condition == null)
                        isDiscoveryStarted = true;
                    else
                        nsdManager.stopServiceDiscovery(this);
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service)
            {
                LogHelper.e(TAG, "Service discovery success" + service);
                reentrantLock.lock();
                try {
                    if (BuildConfig.DEBUG && (latchSynchronizer != null))
                        throw new AssertionError();

                    if (!service.getServiceType().equals(Config.SERVICE_TYPE + '.')) {
                        LogHelper.e(TAG, "Unknown Service Type: " + service.getServiceType());
                    } else if (service.getServiceName().equals(serviceName)) {
                        LogHelper.e(TAG, "Same machine: " + serviceName);
                    } else if (service.getServiceName().contains(serviceName)) {
                        LogHelper.e(TAG, "resolveService: " + serviceName);
                        nsdManager.resolveService(service, resolveListener);
                    }

                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo)
            {
                LogHelper.e(TAG, "service lost" + nsdServiceInfo);
                if (serviceInfo == nsdServiceInfo) {
                    serviceInfo = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType)
            {
                LogHelper.i(TAG, "Discovery stopped: " + serviceType);
                reentrantLock.lock();
                try {
                    // Wakes up one waiting thread.
                    condition.signal();
                } finally {
                    reentrantLock.unlock();
                }
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode)
            {
                LogHelper.e(TAG, "Discovery failed: Error code:" + errorCode);
                reentrantLock.lock();
                try {
                    if (condition != null)
                        // Wakes up one waiting thread.
                        condition.signal();
                } finally {
                    reentrantLock.unlock();
                }
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode)
            {
                LogHelper.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener()
    {
        resolveListener = new NsdManager.ResolveListener()
        {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode)
            {
                LogHelper.e(TAG, "Resolve failed" + errorCode);

                serviceResolveListener.onResolveFailed(nsdServiceInfo, errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo)
            {
                LogHelper.e(TAG, "Resolve Succeeded. " + nsdServiceInfo);

                reentrantLock.lock();
                try {
                    if (nsdServiceInfo.getServiceName().equals(serviceName)) {
                        LogHelper.e(TAG, "Same IP.");
                        return;
                    }
                    serviceResolveListener.onServiceResolved(nsdServiceInfo);
//                    NsdHelper.this.nsdServiceInfo = nsdServiceInfo;
                } finally {
                    reentrantLock.unlock();
                }
            }
        };
    }

    public void initializeRegistrationListener()
    {
        registrationListener = new NsdManager.RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo nsdServiceInfo)
            {
                serviceName = nsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode)
            {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo)
            {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode)
            {
            }
        };
    }

    public void registerService(int port)
    {
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setPort(port);
        final String serviceName = Config.SERVICE_NAME +
                Config.SERVICE_NAME_SEPARATOR +
                deviceID +
                Config.SERVICE_NAME_SEPARATOR;
        nsdServiceInfo.setServiceName(serviceName);
        nsdServiceInfo.setServiceType(Config.SERVICE_TYPE);

        LogHelper.e(TAG, "registerService() | port: " + port);
        nsdManager.registerService(
                nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    public void discoverServices()
    {
//        if (!isDiscoveryStarted) {
//            isDiscoveryStarted = true;
        nsdManager.discoverServices(Config.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
//        }
    }

    public void stopDiscovery()
    {
        boolean interrupted = false;

        reentrantLock.lock();
        try {
            if (discoveryListener != null) {
                final Condition cond = reentrantLock.newCondition();
                condition = cond;

                if (isDiscoveryStarted) {
                    nsdManager.stopServiceDiscovery(discoveryListener);
                    isDiscoveryStarted = false;
                }

                cond.await();
            }
        } catch (final InterruptedException ex) {
            LogHelper.w(TAG, ex.toString());
            interrupted = true;
        } finally {
            reentrantLock.unlock();
        }
        // Channel has 2 possible operations to stop:
        //    - service registration
        //    - service resolve
        final CountDownLatch stopLatch = new CountDownLatch(2);
        try {
            stopLatch.await();
        } catch (final InterruptedException ex) {
            LogHelper.w(TAG, ex.toString());
            interrupted = true;
        }

        if (interrupted)
            Thread.currentThread().interrupt();
    }

    public void stop(CountDownLatch stopLatch)
    {
        reentrantLock.lock();
        try {
            if (this.latchSynchronizer != null) {
                // Channel is being stopped, should not happen.
                if (BuildConfig.DEBUG)
                    throw new AssertionError();
            } else if (connection != null) {
                this.latchSynchronizer = stopLatch;

                if (registrationListener == null) {
                    // Acceptor is not started yet
                    LogHelper.e(TAG, ": wait channelAcceptor");
                    if (BuildConfig.DEBUG && (localPort != -1))
                        throw new AssertionError();
                } else {
                    LogHelper.e(TAG, ": unregister service");
                    nsdManager.unregisterService(registrationListener);
                }
            } else {
                // ChannelAcceptor == null
                stopLatch.countDown();
            }

            // Discovery is stopped now, onServiceFound()/onServiceLost() will not be called any more.
            final Iterator<Map.Entry<String, ServiceInfo>> it = serviceInfo.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, ServiceInfo> entry = it.next();
                final String serviceName = entry.getKey();
                final ServiceInfo serviceInfo = entry.getValue();
                if (((resolveListener != null) && this.serviceName.equals(serviceName)) ||
                        (serviceInfo.connector != null) ||
                        (serviceInfo.session != null)) {
                    serviceInfo.nsdServiceInfo = null;
                } else
                    it.remove();
            }

            if (resolveListener == null)
                stopLatch.countDown();
        } finally {
            reentrantLock.unlock();
        }
    }

    public NsdServiceInfo getChosenServiceInfo()
    {
        return serviceInfo;
    }

    public void tearDown()
    {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
        }
        if (connection != null) {
            connection.tearDown();
        }
    }

    public NsdManager.ResolveListener getResolveListener()
    {
        return resolveListener;
    }

    public NsdManager.DiscoveryListener getDiscoveryListener()
    {
        return discoveryListener;
    }

    public NsdManager.RegistrationListener getRegistrationListener()
    {
        return registrationListener;
    }

    public void setServiceResolveListener(ServiceResolveListener serviceResolveListener)
    {
        this.serviceResolveListener = serviceResolveListener;
    }

    public void setConnectionUpdateListener(ConnectionUpdateListener connectionUpdateListener)
    {
        this.connectionUpdateListener = connectionUpdateListener;
    }

    private static class ServiceInfo
    {
        public NsdServiceInfo nsdServiceInfo;
        public int nsdUpdates;
//        public Connector connector;
//        public Session session;
        public String stationName;
        public String address;
        public int state;
        public long ping;
    }

    private static class SessionInfo
    {
        public String stationName;
        public String address;
        public int state;
        public long ping;
    }

}