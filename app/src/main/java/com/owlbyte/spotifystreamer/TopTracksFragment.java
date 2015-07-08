package com.owlbyte.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksFragment extends Fragment {

    private static final String LOG_TAG = TopTracksFragment.class.getName();
    private CustomListAdapter mCustomAdapter;
    public static String TOP_TRACKS_KEY = "com.owlbyte.spotofiystreamer.TopTracksFragment.TOP_TRACKS_KEY";

    private List<USpotifyObject> listResult;

    public TopTracksFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView mRecyclerView;
        RecyclerView.LayoutManager mLayoutManager;

        Intent intent = getActivity().getIntent();
        View rootView = inflater.inflate(R.layout.fragment_tracks, container, false);

        mCustomAdapter = new CustomListAdapter(new ArrayList<USpotifyObject>());
        mCustomAdapter.SetOnItemClickListener(new CustomListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ((Callback) getActivity()).onTrackItemSelected(position, listResult);
            }
        });

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.track_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager)mLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mCustomAdapter);

        if (savedInstanceState == null) {
            String artistParameter;
            Bundle arguments = getArguments();
            if (arguments != null) {
                artistParameter = arguments.getString(TOP_TRACKS_KEY);
                new FetchTracksTask().execute(artistParameter);
            }
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                artistParameter = intent.getStringExtra(Intent.EXTRA_TEXT);
                new FetchTracksTask().execute(artistParameter);
            }
        }
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("artistsKey")) {
            listResult = savedInstanceState.getParcelableArrayList("artistsKey");
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        fillSpotifyObjList();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("artistsKey", (ArrayList<USpotifyObject>)listResult);
        super.onSaveInstanceState(outState);
    }

    public class FetchTracksTask extends AsyncTask<String, Void, List<USpotifyObject>> {

        @Override
        protected List<USpotifyObject> doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            List<USpotifyObject> trackList = null;
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            Map q = new HashMap<>();
            q.put("country", Utilities.getPreferredMarketCountry(getActivity().getApplicationContext()));
            Tracks result = null;
            try {
                result = spotify.getArtistTopTrack(params[0], q);
            } catch (Exception e) {
                Log.d(LOG_TAG, "Error fectching tracks");
            }

            if (result != null) {
                trackList = new ArrayList<>();
                for (Track track : result.tracks) {
                    String smallImage = "", largeImage = "";
                    if (track.album.images.size() > 0) {
                        smallImage = track.album.images.get(track.album.images.size() - 1).url;
                        largeImage = smallImage;
                        for (Image image : track.album.images) {
                            if (image.width >= 200 && image.width <= 300) {
                                smallImage = image.url;
                            } else if (image.width >= 600 && image.width <= 700) {
                                largeImage = image.url;
                            }
                        }
                    }
                    String trackArtist = "";
                    for (ArtistSimple artist: track.artists) {
                        trackArtist = trackArtist.isEmpty()? artist.name : trackArtist + "," + artist.name;
                    }
                    trackList.add(new USpotifyObject(
                            track.id,
                            trackArtist,
                            smallImage,
                            largeImage,
                            track.name,
                            track.album.name,
                            track.preview_url,
                            track.external_urls.get("spotify")
                    ));
                }
            }
            return trackList;
        }

        @Override
        protected void onPostExecute(List<USpotifyObject> result){
            if (result != null) {
                listResult = result;
                fillSpotifyObjList();
            } else {
                Toast.makeText(getActivity(), getString(R.string.no_connecion), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fillSpotifyObjList() {
        if (listResult != null) {
            mCustomAdapter.clear();
            if (!listResult.isEmpty()) {
                mCustomAdapter.addAll(listResult);
            } else {
                Toast.makeText(getActivity(), R.string.artist_notfound, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * Show top tracks when an item has been selected.
         */
        void onTrackItemSelected(int position, List<USpotifyObject> tracks);
    }
}
