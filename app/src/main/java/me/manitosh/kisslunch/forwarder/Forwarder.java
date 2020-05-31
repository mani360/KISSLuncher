package me.manitosh.kisslunch.forwarder;

import android.content.SharedPreferences;

import me.manitosh.kisslunch.MainActivity;

abstract class Forwarder {
    final MainActivity mainActivity;
    final SharedPreferences prefs;

    Forwarder(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.prefs = mainActivity.prefs;
    }
}