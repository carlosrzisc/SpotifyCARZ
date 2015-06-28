package com.owlbyte.spotifystreamer.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.owlbyte.spotifystreamer.AudioFocusHelper;
import com.owlbyte.spotifystreamer.PlaybackFragment;

import java.io.IOException;

/**
 * Service to handle playing music
 * Created by carlos on 6/15/15.
 */
public class AudioService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener,
        AudioFocusHelper.MusicFocusable,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {

    private static String LOG_TAG = AudioService.class.getName();
    MediaPlayer mMediaPlayer;
    WifiManager.WifiLock mWifiLock;
    NotificationManager mNotificationManager;
//    Notification mNotification = null;
    private AudioFocusHelper mAudioFocusHelper;
//    final int NOTIFICATION_ID = 1;
    private boolean mIsStreaming;

    public static final String ACTION_TOGGLE_PLAYBACK = "com.owlbyte.spotifystreamer.action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "com.owlbyte.spotifystreamer.action.PLAY";
    public static final String ACTION_PAUSE = "com.owlbyte.spotifystreamer.action.PAUSE";
    public static final String ACTION_ADD_TRACK = "com.owlbyte.spotifystreamer.action.ADD_TRACK";

    // The volume we set the media player to when we lose audio focus, but are allowed to reduce
    // the volume instead of stopping playback.
    public static final float DUCK_VOLUME = 0.1f;

    // Seekbar handling
    private final Handler handler = new Handler();
    public static final String BROADCAST_ACTION = "com.owlbyte.spotifystreamer.action.SEEKBAR_POSITION";

    // indicates the state our service:
    enum State {
        Stopped,    // media player is stopped and not prepared to play
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    }

    State mState = State.Stopped;

    // do we have audio focus?
    enum AudioFocus {
        NoFocusNoDuck,    // we don't have audio focus, and can't duck
        NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
        Focused           // we have full audio focus
    }
    AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            Log.d(LOG_TAG, "Initializing media player");
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Creating audio service");
        mWifiLock = ((WifiManager)getSystemService(Context.WIFI_SERVICE)).
                createWifiLock(WifiManager.WIFI_MODE_FULL, "wifilock");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)

        if (android.os.Build.VERSION.SDK_INT >= 8) {
            mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
        } else {
            mAudioFocusHelper = null; // no focus feature, so we always "have" audio focus
        }
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        switch (action) {
            case ACTION_TOGGLE_PLAYBACK: processTogglePlaybackRequest(); break;
            case ACTION_PLAY: processPlayRequest(); break;
            case ACTION_PAUSE: processPauseRequest(); break;
            case ACTION_ADD_TRACK: processAddTrack(intent); break;
        }

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }

    void processPlayRequest() {
        tryToGetAudioFocus();
        /*if (mState == State.Stopped) {
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            //playNextSong(null);
        } else*/ if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            //setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        }
    }

    void processPauseRequest() {
        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mMediaPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }
    }

    void processAddTrack(Intent intent) {
        registerReceiver(broadcastReceiver, new IntentFilter(
                PlaybackFragment.BROADCAST_SEEKBAR));
        tryToGetAudioFocus();
        mState = State.Playing;
        //setUpAsForeground(mSongTitle + " (playing)");
        playNextSong(intent.getData().toString());
        initTrackProgressHandler();
    }

    private void initTrackProgressHandler() {
        handler.removeCallbacks(updateMediaPlayerUI);
        handler.postDelayed(updateMediaPlayerUI, 1000);
    }

    private Runnable updateMediaPlayerUI = new Runnable() {
        public void run() {
            reportMediaPosition();
            handler.postDelayed(this, 1000); // repeat every second
        }
    };

    private void reportMediaPosition() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int mediaPosition = mMediaPlayer.getCurrentPosition();
            int mediaMax = mMediaPlayer.getDuration();
            Intent seekIntent = new Intent(BROADCAST_ACTION);
            seekIntent.putExtra("position", String.valueOf(mediaPosition));
            seekIntent.putExtra("duration", String.valueOf(mediaMax));
            seekIntent.putExtra("has_ended", "0");
            sendBroadcast(seekIntent);
        }
    }

    // --Receive seekbar position if it has been changed by the user in the
    // activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    // Update seek position from Activity
    public void updateSeekPos(Intent intent) {
        int seekPos = intent.getIntExtra("seekpos", 0);
        if (mMediaPlayer.isPlaying()) {
            handler.removeCallbacks(updateMediaPlayerUI);
            mMediaPlayer.seekTo(seekPos);
            initTrackProgressHandler();
        }
    }

    void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.requestFocus())
            mAudioFocus = AudioFocus.Focused;
    }

//    /**
//     * Configures service as a foreground service. A foreground service is a service that's doing
//     * something the user is actively aware of (such as playing music), and must appear to the
//     * user as a notification. That's why we create the notification here.
//     */
//    void setUpAsForeground(String text) {
//        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
//                new Intent(getApplicationContext(), PlaybackActivity.class),
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        mNotification = new Notification();
//        mNotification.tickerText = text;
//        // TODO mNotification.icon = R.drawable.ic_stat_playing;
//        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
//        mNotification.setLatestEventInfo(getApplicationContext(), "Spotify Streamer",
//                text, pi);
//        startForeground(NOTIFICATION_ID, mNotification);
//    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(String manualUrl) {
//        if (null == manualUrl || manualUrl.isEmpty()) {
//            manualUrl = previewUrl;
//        }

        //manualUrl = "http://d318706lgtcm8e.cloudfront.net/mp3-preview/f454c8224828e21fa146af84916fd22cb89cedc6";
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer
        try {
            if (manualUrl != null) {
                // set the source of the media player to a manual URL or path
                createMediaPlayerIfNeeded();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(manualUrl);
                mIsStreaming = true;
            }
            // starts preparing the media player in the background. When it's done, it will call
            // our OnPreparedListener (that is, the onPrepared() method on this class, since we set
            // the listener to 'this').
            //
            // Until the media player is prepared, we *cannot* call start() on it!
            mMediaPlayer.prepareAsync();
            // If we are streaming from the internet, we want to hold a Wifi lock, which prevents
            // the Wifi radio from going to sleep while the song is playing. If, on the other hand,
            // we are *not* streaming, we want to release the lock if we were holding it before.
            if (mIsStreaming) mWifiLock.acquire();
            else if (mWifiLock.isHeld()) mWifiLock.release();
        }
        catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
//        updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
    }

//    /** Updates the notification. */   TODO
//    void updateNotification(String text) {
//        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
//                new Intent(getApplicationContext(), PlaybackActivity.class),
//                PendingIntent.FLAG_UPDATE_CURRENT);
//        mNotification.setLatestEventInfo(getApplicationContext(), "Spotify streamer ", text, pi);
//        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
                Toast.LENGTH_SHORT).show();
        Log.e(LOG_TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
        return true; // true indicates we handled the error
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopSelf();
        Intent seekIntent = new Intent(BROADCAST_ACTION);
        seekIntent.putExtra("has_ended", "1");
        sendBroadcast(seekIntent);
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) { }

    @Override
    public void onDestroy() {
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);
        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            unregisterReceiver(broadcastReceiver);
        }
        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();
    }

    void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
                && mAudioFocusHelper.abandonFocus())
            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    @Override
    public void onGainedAudioFocus() {
        mAudioFocus = AudioFocus.Focused;
        // restart media player with new focus settings
        if (mState == State.Playing)
            configAndStartMediaPlayer();
    }

    @Override
    public void onLostAudioFocus(boolean canDuck) {
        mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;
        // start/restart/pause media player with new focus settings
        if (mMediaPlayer != null && mMediaPlayer.isPlaying())
            configAndStartMediaPlayer();
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
     * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
     * we have focus, it will play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
     * current focus settings. This method assumes mPlayer != null, so if you are calling it,
     * you have to do so from a context where you are sure this is the case.
     */
    void configAndStartMediaPlayer() {
        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
            // If we don't have audio focus and can't duck, we have to pause, even if mState
            // is State.Playing. But we stay in the Playing state so that we know we have to resume
            // playback once we get the focus back.
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            return;
        }
        else if (mAudioFocus == AudioFocus.NoFocusCanDuck) {
            mMediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
        } else {
            mMediaPlayer.setVolume(1.0f, 1.0f); // we can be loud
        }
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }
}
