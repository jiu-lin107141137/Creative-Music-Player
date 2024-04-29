package com.example.player.service;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import kotlin.jvm.Volatile;

public class MusicServiceConnection {
    private Context context;
    private ComponentName serviceComponent;

    @Volatile
    private static MusicServiceConnection instance;

    private PlaybackStateCompat EMPTY_PLAYBACK_STATE;
    private MediaMetadataCompat NOTHING_PLAYING;

    public MutableLiveData<Boolean> isConnected;
    public MutableLiveData<PlaybackStateCompat> playbackState;
    public MutableLiveData<MediaMetadataCompat> nowPlaying;

    private MediaControllerCompat mediaController;
//    public MediaControllerCompat.TransportControls transportControls;

    private MediaBrowserConnectionCallback mediaBrowserConnectionCallback;
    private MediaBrowserCompat mediaBrowser;

    private MusicServiceConnection(Context context, ComponentName serviceComponent) {
        this.context = context;
        this.serviceComponent = serviceComponent;

        mediaBrowserConnectionCallback = new MediaBrowserConnectionCallback(this.context);
        mediaBrowser = new MediaBrowserCompat(this.context, serviceComponent, mediaBrowserConnectionCallback, null);
        mediaBrowser.connect();

        EMPTY_PLAYBACK_STATE = new PlaybackStateCompat.Builder()
                                        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                                        .build();
        NOTHING_PLAYING = new MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
                                    .build();

        if(isConnected == null)
            isConnected = new MutableLiveData<>();
        isConnected.postValue(false);
        if(playbackState == null)
            playbackState = new MutableLiveData<>();
        playbackState.postValue(EMPTY_PLAYBACK_STATE);
        if(nowPlaying == null)
            nowPlaying = new MutableLiveData<>();
        nowPlaying.postValue(NOTHING_PLAYING);

//        transportControls = mediaController.getTransportControls();
    }

    public MediaControllerCompat.TransportControls getTransportControls() {
//        if(mediaController == null)
//            return null;
        return mediaController.getTransportControls();
    }

    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        Context c;

        public MediaBrowserConnectionCallback(Context c) {
            this.c = c;
        }

        @Override
        public void onConnected() {
//            super.onConnected();
            Log.d("MediaBrowserConnectionCallback", "MediaBrowserConnectionCallback connected");
            mediaController = new MediaControllerCompat(c, mediaBrowser.getSessionToken());
            mediaController.registerCallback(new MediaControllerCallback());
            isConnected.postValue(true);
        }

        @Override
        public void onConnectionSuspended() {
//            super.onConnectionSuspended();
            Log.d("MediaBrowserConnectionCallback", "MediaBrowserConnectionCallback onConnectionSuspended");
            isConnected.postValue(false);
        }

        @Override
        public void onConnectionFailed() {
//            super.onConnectionFailed();
            Log.d("MediaBrowserConnectionCallback", "MediaBrowserConnectionCallback onConnectionFailed");
            isConnected.postValue(false);
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
//            super.onPlaybackStateChanged(state);
            Log.d("MediaControllerCallback", "MediaControllerCallback onPlaybackStateChanged: "+state.toString());
            playbackState.postValue(state == null ? EMPTY_PLAYBACK_STATE : state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
//            super.onMetadataChanged(metadata);
            Log.d("MediaControllerCallback", "MediaControllerCallback onMetadataChanged: "+metadata.toString());
            Log.e("onMetadataChanged", metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)+" / " +
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));
            nowPlaying.postValue(
                metadata == null ?
                    NOTHING_PLAYING :
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == null ?
                            NOTHING_PLAYING :
                            metadata
            );
        }
    }

    public static MusicServiceConnection getInstance(Context c, ComponentName cn) {
        if(instance == null) {
            synchronized (MusicServiceConnection.class) {
                if(instance == null) {
                    instance = new MusicServiceConnection(c, cn);
                }
            }
        }
        return instance;
    }
}
