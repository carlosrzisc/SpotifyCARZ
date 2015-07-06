package com.owlbyte.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SpotifyStreamerActivity extends AppCompatActivity implements ArtistsFragment.Callback, TopTracksFragment.Callback {

    private static final String TOPTRACKSFRAGMENT_TAG = "TTF_TAG";
    private boolean mTwoPaneView;

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
    }

    // NOTE: THE FOLLOWING TWO METHODS ARE COMMENTED OUT BECAUSE MENUS ARE NOT NEEDED FOR STAGE 1
    // WILL UN-COMMENT IF THESE ARE REQUIRED IN STAGE 2 OF THIS APP

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spotify_streamer, menu);
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
}
