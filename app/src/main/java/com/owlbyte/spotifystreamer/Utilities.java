package com.owlbyte.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utilities class
 */
public class Utilities {
    private static final String MSPOTIFY_SHARE_HASHTAG = "#SpotifyStreamer";
    /**
     * Converts milliseconds to time format
     * Hours:Minutes:Seconds
     * */
    public static String milliSecondsToTimer(long milliseconds){
        String finalTimerString = "";
        String secondsString;

        // Convert total duration into time
        int hours = (int)( milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int) ((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        // Add hours if there
        if(hours > 0){
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if(seconds < 10){
            secondsString = "0" + seconds;
        }else{
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    public static String getPreferredMarketCountry(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                return preferences.getString(context.getString(R.string.pref_country_key),
                        context.getString(R.string.pref_country_us));
    }

    public static boolean isNotificationControlsEnabled(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String displayControlsKey = context.getString(R.string.pref_enable_controls_key);
        return preferences.getBoolean(displayControlsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_controls_default)));
    }

    public static Intent createShareSongIntent(String shareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(android.os.Build.VERSION.SDK_INT >= 21 ?
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT:Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText + " " + MSPOTIFY_SHARE_HASHTAG);
        return shareIntent;
    }
}
