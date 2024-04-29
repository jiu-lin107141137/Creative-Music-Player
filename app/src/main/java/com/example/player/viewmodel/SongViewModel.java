package com.example.player.viewmodel;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.player.repository.SongRepository;
import com.example.player.service.MusicServiceConnection;
import com.example.player.util.Song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kotlin.Suppress;

public class SongViewModel extends ViewModel {
    MutableLiveData<ArrayList<Song>> songs;
    MutableLiveData<HashMap<String, List<Song>>> playlists;
    MutableLiveData<ArrayList<Song>> favorite;
    ContentResolver cr;
    SongRepository sr;
    MusicServiceConnection msc;

    public SongViewModel(ContentResolver cr, SongRepository sr, MusicServiceConnection msc) {
        this.cr = cr;
        this.sr = sr;
        this.msc = msc;
    }

    public MutableLiveData<ArrayList<Song>> getSongs() {
        if(this.songs == null)
            songs = new MutableLiveData<ArrayList<Song>>();
        return this.songs;
    }

    public MutableLiveData<HashMap<String, List<Song>>> getPlayLists() {
        if(this.playlists == null)
            playlists = new MutableLiveData<>();
        return this.playlists;
    }

    public MutableLiveData<ArrayList<Song>> getFavorite() {
        if(this.favorite == null) {
            this.favorite = new MutableLiveData<>();
        }
        return this.favorite;
    }

    public void loadSongs()throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future future = executorService.submit(new Callable() {
            @Override
            public List<Song> call() {
                return querySongs();

            }
        });

        getSongs().setValue((ArrayList<Song>) future.get());
        loadPlayLists();
    }

    public void loadPlayLists()throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future future = executorService.submit(new Callable() {
            @Override
            public HashMap<String, List<Song>> call() {
                return queryPlayLists();
            }
        });

        HashMap<String, List<Song>> tmp = (HashMap<String, List<Song>>) future.get();

        getPlayLists().setValue(tmp);
        getFavorite().setValue((ArrayList<Song>) tmp.getOrDefault("Favorite", new ArrayList<>()));
    }

    public void playMedia(Song mediaItem, boolean pauseAllowed) {
        MediaMetadataCompat nowPlaying = msc.nowPlaying.getValue();
        MediaControllerCompat.TransportControls transportControls = msc.getTransportControls();

        boolean isPrepared = isPrepared(msc.playbackState.getValue());

        if(isPrepared && mediaItem != null && Long.toString(mediaItem.id).equals(nowPlaying.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
            PlaybackStateCompat playbackState = msc.playbackState.getValue();
            if(playbackState != null) {
                if(isPlaying(playbackState)) {
                    if(pauseAllowed){
                        transportControls.pause();
                    }
                }
                else if(isPlayEnabled(playbackState)) {
                    transportControls.play();
                }
                else {
                    Log.w("playMedia (viewModel)", "Playable item clicked but neither play nor pause are enabled! mediaId = "+mediaItem.id);
                }
            }
        }else {
            transportControls.playFromMediaId(Long.toString(mediaItem.id), null);
        }
    }

    public void playMediaId(String mediaId) {
        MediaMetadataCompat nowPlaying = msc.nowPlaying.getValue();
        MediaControllerCompat.TransportControls transportControls = msc.getTransportControls();

        boolean isPrepared = isPrepared(msc.playbackState.getValue());

        if(isPrepared && nowPlaying != null && mediaId.equals(nowPlaying.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
            PlaybackStateCompat playbackState = msc.playbackState.getValue();
            if(playbackState != null) {
                if(isPlaying(playbackState)) {
//                    if(pauseAllowed){
                        transportControls.pause();
//                    }
                }
                else if(isPlayEnabled(playbackState)) {
                    transportControls.play();
                }
                else {
                    Log.w("playMedia (viewModel)", "Playable item clicked but neither play nor pause are enabled! mediaId = "+mediaId);
                }
            }
        }else {
            transportControls.playFromMediaId(mediaId, null);
        }
    }

    public void playMediaId(String mediaId, boolean force) {
        MediaControllerCompat.TransportControls transportControls = msc.getTransportControls();
        if(force)
            transportControls.playFromMediaId(mediaId, null);
    }


//    public void playMediaId(String mediaId, boolean forcePause) {
//        MediaMetadataCompat nowPlaying = msc.nowPlaying.getValue();
//        MediaControllerCompat.TransportControls transportControls = msc.getTransportControls();
//
//        boolean isPrepared = isPrepared(msc.playbackState.getValue());
//
//        if(isPrepared && nowPlaying != null && mediaId.equals(nowPlaying.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))) {
//            PlaybackStateCompat playbackState = msc.playbackState.getValue();
//            if(playbackState != null) {
//                if(isPlaying(playbackState) || forcePause) {
////                    if(pauseAllowed){
//                    transportControls.pause();
////                    }
//                }
//                else if(isPlayEnabled(playbackState)) {
//                    transportControls.play();
//                }
//                else {
//                    Log.w("playMedia (viewModel)", "Playable item clicked but neither play nor pause are enabled! mediaId = "+mediaId);
//                }
//            }
//        }else {
//            transportControls.playFromMediaId(mediaId, null);
//        }
//    }

    private boolean isPrepared(PlaybackStateCompat playbackStateCompat) {
        if(playbackStateCompat != null){
            int state = playbackStateCompat.getState();
            return (state == PlaybackStateCompat.STATE_BUFFERING) ||
                    (state == PlaybackStateCompat.STATE_PLAYING) ||
                    (state == PlaybackStateCompat.STATE_PAUSED);
        }
        return false;
    }

    private boolean isPlaying(PlaybackStateCompat playbackStateCompat) {
        if(playbackStateCompat != null){
            int state = playbackStateCompat.getState();
            return (state == PlaybackStateCompat.STATE_BUFFERING) ||
                    (state == PlaybackStateCompat.STATE_PLAYING);
        }
        return false;
    }

    private boolean isPlayEnabled(PlaybackStateCompat playbackStateCompat) {
        if(playbackStateCompat != null){
            int state = playbackStateCompat.getState();
            long actions = playbackStateCompat.getActions();
            return (actions > 0 && PlaybackStateCompat.ACTION_PLAY != 0L) ||
                    ((actions > 0 && PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) &&
                    (state == PlaybackStateCompat.STATE_PAUSED));
        }
        return false;
    }

    private List<Song> querySongs() {
        return sr.loadSongs();
    }

    private HashMap<String, List<Song>> queryPlayLists() {
        return sr.loadPlaylist(songs.getValue());
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private ContentResolver fcr;
        private SongRepository fsr;
        private MusicServiceConnection fmsc;

        public Factory(ContentResolver fcr, SongRepository fsr, MusicServiceConnection fmsc) {
            this.fcr = fcr;
            this.fsr = fsr;
            this.fmsc = fmsc;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new SongViewModel(fcr, fsr, fmsc);
        }
    }
}
