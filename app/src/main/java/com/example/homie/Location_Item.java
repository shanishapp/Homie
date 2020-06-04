package com.example.homie;

import android.location.Location;
import android.widget.TextView;

public class Location_Item {

    private String latitude;
    private String longtitude;
    private String accuracy;

    Location_Item(Location location){
        if (location != null) {
            latitude = String.valueOf(location.getLatitude());
            longtitude = String.valueOf(location.getLongitude());
            accuracy= String.valueOf(location.getAccuracy());
        }
    }
}
