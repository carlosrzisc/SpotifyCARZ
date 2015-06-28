package com.owlbyte.spotifystreamer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by carlos on 6/2/15.
 *
 * Used the following links to learn how to use recyclerView
 * https://developer.android.com/training/material/lists-cards.html
 * http://venomvendor.blogspot.sg/2014/07/setting-onitemclicklistener-for-recycler-view.html
 *
 */
public class CustomListAdapter extends RecyclerView.Adapter<CustomListAdapter.ViewHolder> {
    private List<USpotifyObject> artistList;
    private OnItemClickListener mItemClickListener;


    public CustomListAdapter(List<USpotifyObject> artistList) {
        this.artistList = artistList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.uspotify_item, viewGroup, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        USpotifyObject spotifyObject = artistList.get(i);
        if (!spotifyObject.getSmallImage().isEmpty()) {
            Context context = viewHolder.itemImage.getContext();
            Picasso.with(context).load(spotifyObject.getSmallImage()).into(viewHolder.itemImage);
        }
        viewHolder.txtAlbum.setText(spotifyObject.getAlbum());
        if (spotifyObject.getTrackName().isEmpty()) {
            viewHolder.itemText.setText(spotifyObject.getArtistName());
        } else {
            viewHolder.itemText.setText(spotifyObject.getTrackName());
        }

    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public void clear() {
        artistList.clear();
        notifyDataSetChanged();
    }

    public void add(USpotifyObject spotifyObject) {
        artistList.add(spotifyObject);
        notifyDataSetChanged();
    }

    public void addAll(List<USpotifyObject> spotifyObjects) {
        artistList.addAll(spotifyObjects);
        notifyDataSetChanged();
    }

    public USpotifyObject get(int position) {
        return artistList.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView itemText;
        public TextView txtAlbum;
        public ImageView itemImage;
        public ViewHolder(View view) {
            super(view);
            itemText = (TextView) view.findViewById(R.id.item_text);
            itemImage = (ImageView) view.findViewById(R.id.item_image);
            txtAlbum = (TextView) view.findViewById(R.id.txt_album);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if(mItemClickListener != null) {
                mItemClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }
    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }
}
