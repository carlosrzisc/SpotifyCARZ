package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
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

import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlaybackFragment extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private TextView txtArtist;
    private TextView txtAlbum;
    private ImageView imgAlbum;
    private TextView txtTrack;
    private SeekBar seekBarMedia;
    private TextView txtTrackDuration;
    private TextView txtTrackProgress;
    private Button btnTogglePlayback;

    private boolean isPlaying = true;
    private int songIndex = 0;
    private List<USpotifyObject> topTracks;
    public static final String SONG_INDEX_KEY = "SONG_INDEX_KEY";
    public static final String TRACKS_KEY = "topTracks";
    public static final String POSITION_KEY = "position";
    public static final String BROADCAST_SEEKBAR = "com.owlbyte.spotifystreamer.MOVE_TRACK_POSITION";

    public PlaybackFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        initUIComponents(rootView);
        Bundle arguments = getArguments();
        if (arguments != null) {
            topTracks = arguments.getParcelableArrayList(TRACKS_KEY);
        }
        if (topTracks != null) {
            if (savedInstanceState == null) {
                if (arguments != null) {
                    songIndex = arguments.getInt(POSITION_KEY);
                }
                Intent intent = getActivity().getIntent();
                if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                    songIndex = Integer.parseInt(intent.getStringExtra(Intent.EXTRA_TEXT));
                }
                setPlaybackTrack(topTracks.get(songIndex).getPreviewUrl());
            }
            setCurrentSong(songIndex);
        }
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(SONG_INDEX_KEY)) {
            songIndex = savedInstanceState.getInt(SONG_INDEX_KEY);
        }
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
                AudioService.BROADCAST_ACTION));
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SONG_INDEX_KEY, songIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

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

    private void playNextTrack() {
        if (topTracks != null) {
            if (songIndex == topTracks.size() - 1) {
                setCurrentSong(0);
                setPlaybackTrack(topTracks.get(songIndex).getPreviewUrl());
            } else {
                setCurrentSong(songIndex + 1);
                setPlaybackTrack(topTracks.get(songIndex).getPreviewUrl());
            }
        }
        seekBarMedia.setProgress(0);
    }

    private void playPreviousTrack() {
        if (topTracks != null) {
            if (songIndex == 0) {
                setCurrentSong(topTracks.size() - 1);
                setPlaybackTrack(topTracks.get(songIndex).getPreviewUrl());
            } else {
                setCurrentSong(songIndex - 1);
                setPlaybackTrack(topTracks.get(songIndex).getPreviewUrl());
            }
        }
        seekBarMedia.setProgress(0);
    }

    private void setCurrentSong(int index) {
        txtArtist.setText(topTracks.get(index).getArtistName());
        txtAlbum.setText(topTracks.get(index).getAlbum());
        Picasso.with(getActivity().getApplicationContext()).load(
                topTracks.get(index).getLargeImage()).into(imgAlbum);
        txtTrack.setText(topTracks.get(index).getTrackName());
        txtTrackDuration.setText("");
        txtTrackProgress.setText("0:00");
        songIndex = index;
    }

    private void setPlaybackTrack(String track) {
        Intent intent = new Intent(getActivity(), AudioService.class);
        intent.setAction(AudioService.ACTION_ADD_TRACK);
        Uri uri = Uri.parse(track);
        intent.setData(uri);
        getActivity().startService(intent);
    }

    private void processTogglePlayback() {
        togglePlaybackButton();
        Intent audioIntent = new Intent(getActivity(), AudioService.class);
        audioIntent.setAction(AudioService.ACTION_TOGGLE_PLAYBACK);
        getActivity().startService(audioIntent);
    }

    private void togglePlaybackButton() {
        if (isPlaying) {
            btnTogglePlayback.setBackgroundResource(android.R.drawable.ic_media_play);
            isPlaying = false;
        } else {
            btnTogglePlayback.setBackgroundResource(android.R.drawable.ic_media_pause);
            isPlaying = true;
        }
    }

    // -- Broadcast Receiver to update position of seekbar from service --
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateSeekBar(serviceIntent);
        }
    };

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
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
