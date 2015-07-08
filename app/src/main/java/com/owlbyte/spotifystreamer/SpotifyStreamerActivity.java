package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;

import com.owlbyte.spotifystreamer.service.AudioService;

import java.util.ArrayList;
import java.util.List;

public class SpotifyStreamerActivity extends AppCompatActivity implements ArtistsFragment.Callback, TopTracksFragment.Callback {

    private static final String TOPTRACKSFRAGMENT_TAG = "TTF_TAG";
    private boolean mTwoPaneView;
    MenuItem menuNowPlaying;
    MenuItem menuShare;
    ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_streamer);

        if (findViewById(R.id.toptracks_container) != null) {
            mTwoPaneView = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.toptracks_container, new TopTracksFragment(), TOPTRACKSFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPaneView = false;
        }
        registerReceiver(playbackStateReceiver, new IntentFilter(
                AudioService.UPDATE_UI_BROADCAST_ACTION));
        Intent audioIntent = new Intent(this, AudioService.class);
        audioIntent.setAction(AudioService.ACTION_UPDATE_UI);
        startService(audioIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(playbackStateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spotify_streamer, menu);
        menuNowPlaying = menu.findItem(R.id.action_nowplaying);
        menuShare = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuShare);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_nowplaying) {
            if (mTwoPaneView) {
                PlaybackFragment dialogFragment = new PlaybackFragment();
                dialogFragment.show(getSupportFragmentManager(), "playbackDialog");
            } else {
                Intent playbackIntent = new Intent(this, PlaybackActivity.class);
                startActivity(playbackIntent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onArtistItemSelected(String itemId){
        if (mTwoPaneView) {
            Bundle args = new Bundle();
            args.putString(TopTracksFragment.TOP_TRACKS_KEY, itemId);
            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.toptracks_container, fragment, TOPTRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, TracksActivity.class);
            intent.putExtra(Intent.EXTRA_TEXT, "" + itemId);
            startActivity(intent);
        }
    }

    @Override
    public void onTrackItemSelected(int position, List<USpotifyObject> tracks) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(PlaybackFragment.TRACKS_KEY, (ArrayList<USpotifyObject>)tracks);
        args.putInt(PlaybackFragment.POSITION_KEY, position);

        PlaybackFragment dialogFragment = new PlaybackFragment();
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "playbackDialog");
    }

    /**
     * Broadcast Receiver to update playback info
     */
    private BroadcastReceiver playbackStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            USpotifyObject currentTrack = serviceIntent.getParcelableExtra(PlaybackFragment.CURRENT_TRACK);
            setMenuActionsVisible(currentTrack);
        }
    };

    private void setMenuActionsVisible(USpotifyObject currentTrack) {
        if (menuNowPlaying != null) {
            menuNowPlaying.setVisible(true);
        }
        if (menuShare != null && mTwoPaneView) {
            menuShare.setVisible(true);
            mShareActionProvider.setShareIntent(Utilities.createShareSongIntent(
                    currentTrack.getArtistName() + " - " +
                            currentTrack.getTrackName() + " " +
                            currentTrack.getExternalSpotify()));
        }
    }
}
