package com.owlbyte.spotifystreamer;

import android.content.Context;
import android.media.AudioManager;

/**
 * Created by carlos on 6/15/15.
 */
public class AudioFocusHelper implements AudioManager.OnAudioFocusChangeListener {
    AudioManager mAudioManager;
    MusicFocusable mFocusable;

    public AudioFocusHelper(Context applicationContext, MusicFocusable focusable) {
        mAudioManager = (AudioManager) applicationContext.getSystemService(Context.AUDIO_SERVICE);
        mFocusable = focusable;
    }

    public boolean requestFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
    }

    public boolean abandonFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.abandonAudioFocus(this);
    }

    /**
     * Called by AudioManager on audio focus changes. We implement this by calling our
     * MusicFocusable appropriately to relay the message.
     */
    public void onAudioFocusChange(int focusChange) {
        if (mFocusable == null) return;
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mFocusable.onGainedAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mFocusable.onLostAudioFocus(false);
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mFocusable.onLostAudioFocus(true);
                break;
            default:
        }
    }

    public interface MusicFocusable {
        void onGainedAudioFocus();
        void onLostAudioFocus(boolean canDuck);
    }
}
