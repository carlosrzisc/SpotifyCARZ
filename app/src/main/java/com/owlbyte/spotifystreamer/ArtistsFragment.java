package com.owlbyte.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Image;

/**
 * A placeholder fragment containing a simple view.
 */
public class ArtistsFragment extends Fragment implements View.OnKeyListener {

    private static final String LOG_TAG = ArtistsFragment.class.getName();

    private CustomListAdapter mCustomAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    private List<USpotifyObject> listResult;

    public ArtistsFragment() {  }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_spotify_streamer, container, false);
        rootView.findViewById(R.id.txt_search_artist).setOnKeyListener(this);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.artist_list);
        mLayoutManager = new LinearLayoutManager(getActivity());
        ((LinearLayoutManager)mLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mCustomAdapter = new CustomListAdapter(new ArrayList<USpotifyObject>());
        mCustomAdapter.SetOnItemClickListener(new CustomListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(getActivity(), TracksActivity.class);
                USpotifyObject mSObject = mCustomAdapter.get(position);
                intent.putExtra(Intent.EXTRA_TEXT, "" + mSObject.getId());
                startActivity(intent);
            }
        });
        mRecyclerView.setAdapter(mCustomAdapter);

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
        outState.putParcelableArrayList("artistsKey", (ArrayList)listResult);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (view.getId() == R.id.txt_search_artist && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            String inputSearch = String.valueOf(((EditText) view).getText());
            Log.d(LOG_TAG, "Input search: " + inputSearch);
            if (!inputSearch.isEmpty()) {
                new FetchArtistsTask().execute(inputSearch);
            } else {
                mCustomAdapter.clear();
            }
        }
        return false;
    }

    public class FetchArtistsTask extends AsyncTask<String, Void, List<USpotifyObject>> {

        @Override
        protected List<USpotifyObject> doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }
            List<USpotifyObject> artistList = null;
            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            ArtistsPager results = null;
            try {
                results = spotify.searchArtists(params[0]);
            } catch(Exception e) {
                Log.d(LOG_TAG, "Error fetching artists");
            }
            if (results != null) {
                artistList = new ArrayList<>();
                for (Artist artist : results.artists.items) {
                    String smallImage = "";
                    if (artist.images.size() > 0) {
                        // Getting image with lowest resolution just to keep it lightweight and faster
                        smallImage = artist.images.get(artist.images.size() - 1).url;
                        for (Image image : artist.images) {
                            if (image.width >= 200 && image.width <= 300) {
                                smallImage = image.url;
                            }
                        }
                    }
                    artistList.add(new USpotifyObject(
                            artist.id,
                            artist.name,
                            smallImage
                    ));
                }
            }
            return artistList;
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
                Toast.makeText(getActivity(), getString(R.string.artist_notfound), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
