package com.example.player.roomdatabase;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "CustomAlbum")
public class MyAlbumData {
    @PrimaryKey
    @NonNull
    private String albumName;
    private String cover;

    public MyAlbumData(String albumName, String cover) {
        this.albumName = albumName;
        this.cover = cover;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }
}
