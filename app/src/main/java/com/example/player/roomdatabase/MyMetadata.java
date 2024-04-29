package com.example.player.roomdatabase;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "CustomMetadata")
public class MyMetadata {
    @PrimaryKey
    private long id;
    private String title;
    private String artist;
    private String album;
//    private String albumCover;

    public MyMetadata(long id, String title, String artist, String album/*, String albumCover*/) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
//        this.albumCover = albumCover;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

//    public String getAlbumCover() {
//        return albumCover;
//    }
//
//    public void setAlbumCover(String albumCover) {
//        this.albumCover = albumCover;
//    }
}
