package com.owlbyte.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class TracksActivity extends AppCompatActivity implements TopTracksFragment.Callback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.toptracks_container, new TopTracksFragment())
                    .commit();
        }
    }

    @Override
    public void onTrackItemSelected(int position, List<USpotifyObject> tracks) {
        Intent playbackIntent = new Intent(this, PlaybackActivity.class);
        playbackIntent.putParcelableArrayListExtra(PlaybackFragment.TRACKS_KEY, (ArrayList)tracks);
        playbackIntent.putExtra(Intent.EXTRA_TEXT, "" + position);
        startActivity(playbackIntent);
    }


    // NOTE: THE FOLLOWING TWO METHODS ARE COMMENTED OUT BECAUSE MENUS ARE NOT NEEDED FOR STAGE 1
    // WILL UN-COMMENT IF THESE ARE REQUIRED IN STAGE 2 OF THIS APP

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracks, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
