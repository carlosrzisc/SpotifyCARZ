package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class PlaybackFragment extends Fragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    public static final String TRACKS_KEY = "topTracks";

    private TextView txtArtist;
    private TextView txtAlbum;
    private ImageView imgAlbum;
    private TextView txtTrack;
    private SeekBar seekBarMedia;
    private TextView txtTrackDuration;
    private Button btnTogglePlayback;

    private boolean isPlaying = false;
    private int songIndex = 0;

    private List<USpotifyObject> topTracks;

    public static final String BROADCAST_SEEKBAR = "com.owlbyte.spotifystreamer.MOVE_TRACK_POSITION";

    public PlaybackFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playback, container, false);
        initUIComponents(rootView);

        songIndex = 0;
        Intent intent = getActivity().getIntent();
        if (savedInstanceState == null) {
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                songIndex = Integer.parseInt(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }
        Bundle arguments = getArguments();
        if (arguments != null) {
            topTracks = arguments.getParcelableArrayList(TRACKS_KEY);
        }
        setCurrentSong(songIndex);
        Button togglePlaybackButton = (Button) rootView.findViewById(R.id.btn_media_toggle_playback);
        Button nextButton = (Button) rootView.findViewById(R.id.btn_media_next);
        Button previousButton = (Button) rootView.findViewById(R.id.btn_media_previous);
        togglePlaybackButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        previousButton.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
                AudioService.BROADCAST_ACTION));
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
        seekBarMedia = (SeekBar) view.findViewById(R.id.seekBar_media_timeline);
        seekBarMedia.setOnSeekBarChangeListener(this);
        btnTogglePlayback = (Button) view.findViewById(R.id.btn_media_toggle_playback);
        togglePlaybackButton();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_media_next: playNextTrack(); break;
            case R.id.btn_media_toggle_playback: processTogglePlayback(); break;
            case R.id.btn_media_previous: playPreviousTrack(); break;
        }
    }

    private void playNextTrack() {
        if (songIndex == topTracks.size() - 1) {
            setCurrentSong(0);
        } else {
            setCurrentSong(songIndex + 1);
        }
        seekBarMedia.setProgress(0);
    }

    private void playPreviousTrack() {
        if (songIndex == 0) {
            setCurrentSong(topTracks.size() - 1);
        } else {
            setCurrentSong(songIndex - 1);
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
        setPlaybackTrack(topTracks.get(index).getPreviewUrl());
        songIndex = index;
    }

    private void setPlaybackTrack(String track){
        Intent intent = new Intent(getActivity(), AudioService.class);
        intent.setAction(AudioService.ACTION_ADD_TRACK);
        Uri uri = Uri.parse(track);
        intent.setData(uri);
        getActivity().startService(intent);

//        getActivity().registerReceiver(broadcastReceiver, new IntentFilter(
//                AudioService.BROADCAST_ACTION));
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
            seekBarMedia.setProgress(Integer.parseInt(progress));
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
