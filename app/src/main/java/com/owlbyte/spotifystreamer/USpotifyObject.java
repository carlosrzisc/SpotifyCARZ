package com.owlbyte.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Object to be used as Artist as well as Track
 * Created by carlos on 6/2/15.
 */
public class USpotifyObject implements Parcelable{
    private String id;
    private String artistName;
    private String smallImage;
    private String largeImage;
    private String trackName;
    private String album;
    private String previewUrl;

    public USpotifyObject(String id, String name, String smallImage) {
        this(id, name, smallImage, "","", "", "");
    }

    public USpotifyObject(String id, String artistName, String smallImage, String largeImage, String trackName, String album, String previewUrl ) {
        this.id = id;
        this.artistName = artistName;
        this.smallImage = smallImage;
        this.largeImage = largeImage;
        this.trackName = trackName;
        this.album = album;
        this.previewUrl = previewUrl;
    }

    public USpotifyObject(Parcel in) {
        this.id = in.readString();
        this.artistName = in.readString();
        this.smallImage = in.readString();
        this.largeImage = in.readString();
        this.trackName = in.readString();
        this.album = in.readString();
        this.previewUrl = in.readString();
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String name) {
        this.artistName = artistName;
    }

    public String getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(String image) {
        this.smallImage = smallImage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrackName() { return trackName; }

    public void setTrackName(String trackName) { this.trackName = trackName; }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(String largeImage) {
        this.largeImage = largeImage;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(artistName);
        parcel.writeString(smallImage);
        parcel.writeString(largeImage);
        parcel.writeString(trackName);
        parcel.writeString(album);
        parcel.writeString(previewUrl);
    }

    public static final Parcelable.Creator<USpotifyObject> CREATOR = new Parcelable.Creator<USpotifyObject>() {
        public USpotifyObject createFromParcel(Parcel in) {
            return new USpotifyObject(in);
        }

        public USpotifyObject[] newArray(int size) {
            return new USpotifyObject[size];
        }
    };
}
