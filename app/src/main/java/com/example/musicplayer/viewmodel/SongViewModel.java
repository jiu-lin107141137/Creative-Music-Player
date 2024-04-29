package com.example.musicplayer.viewmodel;


import androidx.lifecycle.MutableLiveData;

import com.example.musicplayer.util.Song;

import java.util.ArrayList;

public class SongViewModel {
    MutableLiveData<ArrayList<Song>> songs;

    public SongViewModel() {

    }

    public MutableLiveData<ArrayList<Song>> getSongs() {
        if(this.songs == null)
            songs = new MutableLiveData<ArrayList<Song>>();
        return this.songs;
    }
}
