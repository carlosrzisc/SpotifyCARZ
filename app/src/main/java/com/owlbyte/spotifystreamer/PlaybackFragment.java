package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.owlbyte.spotifystreamer.service.AudioService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * Fragment for Playback UI, will behave as DialogFragment for tablet and
 * as a regular Fragment in phones
 */
public class PlaybackFragment extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    // Log tag
    private String LOG_TAG = PlaybackFragment.class.getName();

    // UI components
    private TextView txtArtist;
    private TextView txtAlbum;
    private ImageView imgAlbum;
    private TextView txtTrack;
    private SeekBar seekBarMedia;
    private TextView txtTrackDuration;
    private TextView txtTrackProgress;
    private Button btnTogglePlayback;

    // Variables
    private int songIndex = 0;
    private List<USpotifyObject> topTracks;

    // Constants
    public static final String TRACKS_KEY = "topTracks";
    public static final String POSITION_KEY = "position";
    public static final String CURRENT_TRACK = "current_track";
    public static final String IS_PLAYING = "is_playing";

    // Braodcast constants
    public static final String BROADCAST_SEEKBAR = "com.owlbyte.spotifystreamer.action.MOVE_TRACK_POSITION";

    public PlaybackFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "On create view..");
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        initUIComponents(rootView);
        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            if (arguments != null) {
                topTracks = arguments.getParcelableArrayList(TRACKS_KEY);
                songIndex = arguments.getInt(POSITION_KEY);
            }
            if (topTracks != null) {
                // Meaning a track selected manually from user, so we init service with playlist
                Intent intent = getActivity().getIntent();
                if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                    songIndex = Integer.parseInt(intent.getStringExtra(Intent.EXTRA_TEXT));
                }
                initMusicService(topTracks, songIndex);
            } else {
                // User might have launched this from either notification or the "now playing" button
                requestSongInfo();
            }
        } else {
            // User might have rotated view, now attempt to get current track info from service
            requestSongInfo();
        }
        return rootView;
    }

    private void requestSongInfo() {
        Intent audioIntent = new Intent(getActivity(), AudioService.class);
        audioIntent.setAction(AudioService.ACTION_UPDATE_UI);
        getActivity().startService(audioIntent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(updateSeekBarReceiver, new IntentFilter(
                AudioService.SEEK_BROADCAST_ACTION));
        getActivity().registerReceiver(updateUIReceiver, new IntentFilter(
                AudioService.UPDATE_UI_BROADCAST_ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(updateSeekBarReceiver);
        getActivity().unregisterReceiver(updateUIReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_media_next:
                playNextTrack();
                break;
            case R.id.btn_media_toggle_playback:
                processTogglePlayback();
                break;
            case R.id.btn_media_previous:
                playPreviousTrack();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int seekPos = seekBar.getProgress();
            Intent moveTrackPosIntent = new Intent(BROADCAST_SEEKBAR);
            moveTrackPosIntent.putExtra("seekpos", seekPos);
            getActivity().sendBroadcast(moveTrackPosIntent);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) { }

    /**
     * Initialize UI components in layout
     * @param view root view
     */
    private void initUIComponents(View view) {
        txtArtist = (TextView) view.findViewById(R.id.txt_playback_artist);
        txtAlbum = (TextView) view.findViewById(R.id.txt_playback_album);
        imgAlbum = (ImageView) view.findViewById(R.id.img_playback_album);
        txtTrack = (TextView) view.findViewById(R.id.txt_playback_track);
        txtTrackDuration = (TextView) view.findViewById(R.id.txt_track_duration);
        txtTrackProgress = (TextView) view.findViewById(R.id.txt_track_progress);
        seekBarMedia = (SeekBar) view.findViewById(R.id.seekBar_media_timeline);
        seekBarMedia.setOnSeekBarChangeListener(this);
        btnTogglePlayback = (Button) view.findViewById(R.id.btn_media_toggle_playback);

        btnTogglePlayback.setOnClickListener(this);
        Button nextButton = (Button) view.findViewById(R.id.btn_media_next);
        Button previousButton = (Button) view.findViewById(R.id.btn_media_previous);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);
    }

    /**
     * Sends broadcast to service to play next track in playlist
     */
    private void playNextTrack() {
        Intent audioIntent = new Intent(getActivity(), AudioService.class);
        audioIntent.setAction(AudioService.ACTION_PREVIOUS);
        getActivity().startService(audioIntent);
    }

    /**
     * Sends broadcast to service to play previous track in playlist
     */
    private void playPreviousTrack() {
        Intent audioIntent = new Intent(getActivity(), AudioService.class);
        audioIntent.setAction(AudioService.ACTION_NEXT);
        getActivity().startService(audioIntent);
    }

    /**
     * Starts Audio service sending top tracks as playlist
     * @param playList list of tracks
     * @param startSong song index to start playing
     */
    private void initMusicService(List<USpotifyObject> playList, int startSong) {
        if (playList != null && !playList.isEmpty()) {
            Intent intent = new Intent(getActivity(), AudioService.class);
            intent.setAction(AudioService.ACTION_SET_PLAYLIST);
            intent.putParcelableArrayListExtra(PlaybackFragment.TRACKS_KEY, (ArrayList<USpotifyObject>) playList);
            intent.putExtra(Intent.EXTRA_TEXT, "" + startSong);
            getActivity().startService(intent);
        }
    }

    /**
     * Send ACTION_TOGGLE_PLAYBACK command to service
     */
    private void processTogglePlayback() {
        Intent audioIntent = new Intent(getActivity(), AudioService.class);
        audioIntent.setAction(AudioService.ACTION_TOGGLE_PLAYBACK);
        getActivity().startService(audioIntent);
    }

    /**
     * Broadcast Receiver to update position of seekbar from service
     */
    private BroadcastReceiver updateSeekBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateSeekBar(serviceIntent);
        }
    };

    /**
     * Updates seekbar progress retrieved from service
     * @param serviceIntent Music service
     */
    private void updateSeekBar(Intent serviceIntent) {
        String progress = serviceIntent.getStringExtra("position");
        String trackDuration = serviceIntent.getStringExtra("duration");
        String hasSongEnded = serviceIntent.getStringExtra("has_ended");
        if (trackDuration != null) {
            int seekMax = Integer.parseInt(trackDuration);
            seekBarMedia.setMax(seekMax);
            txtTrackDuration.setText(Utilities.milliSecondsToTimer(seekMax));
        }
        if (progress != null) {
            int currentProgress = Integer.parseInt(progress);
            seekBarMedia.setProgress(currentProgress);
            txtTrackProgress.setText(Utilities.milliSecondsToTimer(currentProgress));
        }
        if (hasSongEnded != null && hasSongEnded.equalsIgnoreCase("1")) {
            playNextTrack();
        }
    }

    /**
     * Broadcast Receiver to update playback info
     */
    private BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            USpotifyObject currentTrack = serviceIntent.getParcelableExtra(PlaybackFragment.CURRENT_TRACK);
            setCurrentSong(currentTrack);
            boolean isPlaying = serviceIntent.getBooleanExtra(PlaybackFragment.IS_PLAYING, true);
            togglePlaybackButton(isPlaying);
        }
    };

    /**
     * Updates Playback UI in app
     * @param track USpotifyObject
     */
    private void setCurrentSong(USpotifyObject track) {
        if (track != null) {
            txtArtist.setText(track.getArtistName());
            txtAlbum.setText(track.getAlbum());
            Picasso.with(getActivity().getApplicationContext()).load(
                    track.getLargeImage()).into(imgAlbum);
            txtTrack.setText(track.getTrackName());
            //txtTrackDuration.setText("");
            //txtTrackProgress.setText("0:00");
        }
    }

    /**
     * Toggles playback button
     */
    private void togglePlaybackButton(boolean isPlaying) {
        if (isPlaying) {
            btnTogglePlayback.setBackgroundResource(android.R.drawable.ic_media_pause);
        } else {
            btnTogglePlayback.setBackgroundResource(android.R.drawable.ic_media_play);
        }
    }
}