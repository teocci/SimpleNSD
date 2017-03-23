package com.github.teocci.simplensd.utils;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

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

    private final ReentrantLock reentrantLock;
    private Condition condition;

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
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNSDServer()
    {
        initializeRegistrationListener();
    }

    public void initializeNSDClient()
    {
        initializeDiscoveryListener();
        discoverServices();
        initializeResolveListener();
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
                if (!service.getServiceType().equals(Config.SERVICE_TYPE + '.')) {
                    LogHelper.e(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(serviceName)) {
                    LogHelper.e(TAG, "Same machine: " + serviceName);
                } else if (service.getServiceName().contains(serviceName)) {
                    LogHelper.e(TAG, "resolveService: " + serviceName);
                    nsdManager.resolveService(service, resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service)
            {
                LogHelper.e(TAG, "service lost" + service);
                if (serviceInfo == service) {
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
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
                LogHelper.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo)
            {
                LogHelper.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(serviceName)) {
                    LogHelper.e(TAG, "Same IP.");
                    return;
                }
                NsdHelper.this.serviceInfo = serviceInfo;
            }
        };
    }

    public void initializeRegistrationListener()
    {
        registrationListener = new NsdManager.RegistrationListener()
        {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo)
            {
                serviceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1)
            {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0)
            {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode)
            {
            }
        };
    }

    public void registerService(int port)
    {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        final String serviceName = Config.SERVICE_NAME +
                Config.SERVICE_NAME_SEPARATOR +
                deviceID +
                Config.SERVICE_NAME_SEPARATOR;
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setServiceType(Config.SERVICE_TYPE);

        LogHelper.e(TAG, "registerService() | port: " + port);
        nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
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

    public NsdServiceInfo getChosenServiceInfo()
    {
        return serviceInfo;
    }

    public void tearDown()
    {
        if (registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
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
}