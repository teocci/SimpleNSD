package com.github.teocci.simplensd.interfaces;

import com.github.teocci.simplensd.model.StationInfo;

/**
 * Created by teocci on 3/23/17.
 */

public interface ConnectionUpdateListener
{
    void onStationListChanged(StationInfo[] stationInfo);

    void onMessageUpdate(String message);
}
