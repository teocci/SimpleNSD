package com.github.teocci.simplensd.interfaces;

import android.net.nsd.NsdServiceInfo;

/**
 * Created by teocci on 3/24/17.
 */

public interface ServiceResolveListener
{
    void onServiceResolved(NsdServiceInfo serviceInfo);

    void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);
}
