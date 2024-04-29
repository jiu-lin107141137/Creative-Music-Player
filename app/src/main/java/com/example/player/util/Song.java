package com.example.player.util;

import android.net.Uri;

import java.io.Serializable;

public class Song implements Serializable {
    public long id;
    public String title;
    public long duration;
    public String artistName;
    public long albumId;
    public String albumName;
    public String coverPath;
    public transient Uri contentUri;

    public Song(long id, String title, long duration, String artistName, long albumId, String albumName, String coverPath, Uri contentUri) {
        this.id = id;
        this.title = title;
        this.duration = duration;
        this.artistName = artistName;
        this.albumId = albumId;
        this.albumName = albumName;
        this.coverPath = coverPath;
        this.contentUri = contentUri;
    }

    public String toString() {
        return id+" " +
                title+" "+
                duration+" "+
                artistName+" "+
                albumId+" "+
                albumName+" "+
                coverPath+" "+
                contentUri.toString();
    }
}
