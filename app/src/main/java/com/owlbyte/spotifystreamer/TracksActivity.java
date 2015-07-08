package com.owlbyte.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.owlbyte.spotifystreamer.service.AudioService;

import java.util.ArrayList;
import java.util.List;

public class TracksActivity extends AppCompatActivity implements TopTracksFragment.Callback {

    MenuItem menuNowPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.toptracks_container, new TopTracksFragment())
                    .commit();
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
    public void onTrackItemSelected(int position, List<USpotifyObject> tracks) {
        Intent playbackIntent = new Intent(this, PlaybackActivity.class);
        playbackIntent.putParcelableArrayListExtra(PlaybackFragment.TRACKS_KEY, (ArrayList<USpotifyObject>) tracks);

        playbackIntent.putExtra(Intent.EXTRA_TEXT, "" + position);
        startActivity(playbackIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
        menuNowPlaying = menu.findItem(R.id.action_nowplaying);
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
            Intent playbackIntent = new Intent(this, PlaybackActivity.class);
            startActivity(playbackIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Broadcast Receiver to update playback info
     */
    private BroadcastReceiver playbackStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            setNowPlayingButtonVisible();
        }
    };

    private void setNowPlayingButtonVisible() {
        menuNowPlaying.setVisible(true);
    }
}
