package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.owlbyte.spotifystreamer.service.AudioService;

/**
 * Created by carlos on 6/15/15.
 */
public class MusicIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            context.startService(new Intent(AudioService.ACTION_PAUSE));
        }
    }
}
