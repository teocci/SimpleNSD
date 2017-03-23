package com.github.teocci.simplensd.model;

public class StationInfo
{
    public final String name;
    public final String address;
    public final int transmission;
    public final long ping;

    public StationInfo(String name, String address, int transmission, long ping)
    {
        this.name = name;
        this.address = address;
        this.transmission = transmission;
        this.ping = ping;
    }
}
