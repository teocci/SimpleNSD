package com.github.teocci.simplensd.utils;

import android.media.AudioManager;

/**
 * Created by teocci on 3/17/17.
 */

public class Config
{
    public static final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
    public static int PING_INTERVAL = 5;
    public static final int SAMPLE_INTERVAL = 20; // Milliseconds
    public static final int SAMPLE_SIZE = 2; // Bytes

    public final static String EXTRA_CONTACT = "com.github.teocci.simplensd.CONTACT";
    public final static String EXTRA_IP = "com.github.teocci.simplensd.IP";
    public final static String EXTRA_DISPLAY_NAME = "com.github.teocci.simplensd.DISPLAY_NAME";
    public final static String EXTRA_OPERATION_MODE = "com.github.teocci.simplensd.OPERATION_MODE";

    public static final String KEY_STATION_NAME = "station_name";
    public static final String KEY_VOLUME = "volume";
    private static final String KEY_CHECK_WIFI_STATUS = "check-wifi-status";
    private static final String KEY_USE_VOLUME_BUTTONS_TO_TALK = "use-volume-buttons-to-talk";
    private static final String KEY_STATION_NAME_LIST = "station_name_list";


    public static final String SERVER_MODE = "server_mode";
    public static final String CLIENT_MODE = "client_mode";
    public static final String SERVICE_TYPE = "_simplensd._tcp"; // Smart Mixer
    public static final String SERVICE_NAME = "NSDService";
    public static final String SERVICE_NAME_SEPARATOR = ":";
}
