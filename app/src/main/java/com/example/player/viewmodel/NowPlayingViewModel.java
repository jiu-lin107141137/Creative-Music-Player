package com.example.player.viewmodel;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.player.R;
import com.example.player.service.MusicServiceConnection;


public class NowPlayingViewModel extends ViewModel {
    private Context context;
    private MusicServiceConnection musicServiceConnection;

    private PlaybackStateCompat EMPTY_PLAYBACK_STATE;
    private MediaMetadataCompat NOTHING_PLAYING;

    private PlaybackStateCompat playbackState;

    public MutableLiveData<NowPlayingMetadata> mediaMetadata;
    public MutableLiveData<Long> mediaPosition;
//    public MutableLiveData<Long> mediaPlayProgress;
    public MutableLiveData<int[]> mediaBtnRef;
    public MutableLiveData<Boolean> nowPlayingShowed;
    public MutableLiveData<Boolean> nowPlayingShowedExpanding;
    private boolean updatePosition;
    public long mediaDuration;
    private Handler handler;

    private Observer<PlaybackStateCompat> playbackStateObserver;
    private Observer<MediaMetadataCompat> mediaMetadataObserver;

    public NowPlayingViewModel(Context context, MusicServiceConnection musicServiceConnection) {
        this.context = context;
        this.musicServiceConnection = musicServiceConnection;

        EMPTY_PLAYBACK_STATE = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
                .build();
        NOTHING_PLAYING = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
                .build();

        playbackState = EMPTY_PLAYBACK_STATE;

        mediaMetadata = new MutableLiveData<>();
        mediaPosition = new MutableLiveData<>();
//        mediaPlayProgress = new MutableLiveData<>();
        mediaBtnRef = new MutableLiveData<>();
        nowPlayingShowed = new MutableLiveData<>();
        nowPlayingShowedExpanding = new MutableLiveData<>();
        mediaPosition.postValue(0l);
//        mediaPlayProgress.postValue(0l);
        mediaBtnRef.postValue(
                isPlaying(playbackState) ?
                        new int[]{
                                R.drawable.ic_pause,
                                R.drawable.ic_pause_bigger
                        }:
                        new int[]{
                                R.drawable.ic_play,
                                R.drawable.ic_play_bigger
                        }
        );
        nowPlayingShowed.postValue(false);
        nowPlayingShowedExpanding.postValue(false);

        updatePosition = true;
        mediaDuration = 0;
        handler = new Handler(Looper.myLooper());

        playbackStateObserver = new Observer<PlaybackStateCompat>() {
            @Override
            public void onChanged(PlaybackStateCompat playbackStateCompat) {
                playbackState = playbackStateCompat != null ? playbackStateCompat : EMPTY_PLAYBACK_STATE;
                MediaMetadataCompat metadata = musicServiceConnection.nowPlaying.getValue() != null ? musicServiceConnection.nowPlaying.getValue() : NOTHING_PLAYING;
                updateState(playbackState, metadata);
            }
        };
        mediaMetadataObserver = new Observer<MediaMetadataCompat>() {
            @Override
            public void onChanged(MediaMetadataCompat metadataCompat) {
                updateState(playbackState, metadataCompat);
                mediaDuration = metadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            }
        };

        musicServiceConnection.playbackState.observeForever(playbackStateObserver);
        musicServiceConnection.nowPlaying.observeForever(mediaMetadataObserver);
        checkPlaybackPosition();
    }

    private boolean checkPlaybackPosition() {
        return handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currentPlaybackPosition = getCurrentPlaybackPosition(playbackState);
                if(mediaPosition.getValue() != currentPlaybackPosition) {
                    mediaPosition.postValue(currentPlaybackPosition);
//                    if(mediaDuration > 0) {
//                        int progress = (int)(currentPlaybackPosition * 100 / mediaDuration);
//                        mediaPlayProgress.postValue(currentPlaybackPosition);
//                    }
                }

                if(updatePosition)
                    checkPlaybackPosition();
            }
        }, 100l);
    }

    private void updateState(PlaybackStateCompat playbackStateCompat, MediaMetadataCompat mediaMetadataCompat) {
        if(mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) != 0 && mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            Uri albumUri = Uri.EMPTY;
            if(mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI) != null)
                albumUri = Uri.parse(mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI));
            NowPlayingMetadata nowPlayingMetadata = new NowPlayingMetadata(
                    mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                    albumUri,
                    mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE),
                    mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE),
                    mediaMetadataCompat.getLong(MediaMetadataCompat.METADATA_KEY_DURATION),
                    mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION)
            );
            this.mediaMetadata.postValue(nowPlayingMetadata);
        }
        mediaBtnRef.postValue(
                isPlaying(playbackState) ?
                        new int[]{
                                R.drawable.ic_pause,
                                R.drawable.ic_pause_bigger
                        }:
                        new int[]{
                                R.drawable.ic_play,
                                R.drawable.ic_play_bigger
                        }
        );
    }

    public void playBackward() {
        musicServiceConnection.getTransportControls().skipToPrevious();
    }

    public void playForward() {
        musicServiceConnection.getTransportControls().skipToNext();
    }

    public void seekTo(long pos) {
        musicServiceConnection.getTransportControls().seekTo(pos);
    }

    public void playStop() {
        musicServiceConnection.getTransportControls().stop();
    }

    public void forcePlay() {
        musicServiceConnection.getTransportControls().play();
    }

    public void setPlayMode(int type) {
        // 0 shuffle (default)
        // 1 repeat one
        // 2 repeat all
        MediaControllerCompat.TransportControls control = musicServiceConnection.getTransportControls();
        switch (type) {
            case 0:
                control.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                control.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
                break;
            case 1:
                control.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                control.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
                break;
            case 2:
                control.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                control.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
                break;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        musicServiceConnection.playbackState.removeObserver(playbackStateObserver);
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver);

        updatePosition = false;
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        Context c;
        MusicServiceConnection msc;

        public Factory(Context c, MusicServiceConnection msc) {
            this.c = c;
            this.msc = msc;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new NowPlayingViewModel(this.c, this.msc);
        }
    }

    private boolean isPlaying(PlaybackStateCompat playbackStateCompat) {
        if(playbackStateCompat != null){
            int state = playbackStateCompat.getState();
            return (state == PlaybackStateCompat.STATE_BUFFERING) ||
                    (state == PlaybackStateCompat.STATE_PLAYING);
        }
        return false;
    }

    private long getCurrentPlaybackPosition(PlaybackStateCompat playbackStateCompat) {
        if(playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
            long timeDelta = SystemClock.elapsedRealtime() - playbackStateCompat.getLastPositionUpdateTime();
            return (long)(playbackStateCompat.getPosition() + timeDelta * playbackStateCompat.getPlaybackSpeed());
        }
        return playbackStateCompat.getPosition();
    }

    public static class NowPlayingMetadata {
        public final String id;
        public final Uri albumArtUri;
        public final String title;
        public final String subtitle;
        public final long duration;
        public final String albumName;

        public NowPlayingMetadata(String id, Uri albumArtUri, String title, String subtitle, long duration, String albumName) {
            this.id = id;
            this.albumArtUri = albumArtUri;
            this.title = title;
            this.subtitle = subtitle;
            this.duration = duration;
            this.albumName = albumName;
        }

        public String timestampToMSS(Context context, long position) {
            int totalSeconds = (int) Math.floor(position / 1E3);
            int minutes = totalSeconds / 60;
            int remainingSeconds = totalSeconds - (minutes * 60);
            if (position < 0) {
                return context.getString(R.string.duration_unknown);
            } else {
                return context.getString(R.string.duration_format, minutes, remainingSeconds);
            }
        }
    }
}
