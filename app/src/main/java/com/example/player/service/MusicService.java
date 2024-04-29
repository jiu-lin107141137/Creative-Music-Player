package com.example.player.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.example.player.repository.SongRepository;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MusicService extends MediaBrowserServiceCompat {
    private static final String MY_MEDIA_ROOT_ID = "/";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "@empty@";
    private static final String MUSIC_USER_AGENT = "music.agent";

    private MediaSessionCompat mediaSession;
//    private PlaybackStateCompat.Builder stateBuilder;
    private MediaSessionConnector mediaSessionConnector;
//    private SongRepository songRepository;
    private MusicNotificationManager notificationManager;
    private boolean isForegroundService = false;

    private List<MediaMetadataCompat> currentPlayListItems = new ArrayList<MediaMetadataCompat>();

    private Player currentPlayer;
    private ExoPlayer exoPlayer;
    private AudioAttributes audioAttributes;
    private PlayEventListener playEventListener;
    private DefaultDataSource.Factory dataSourceFactory;

    @Override
    public void onCreate() {
        super.onCreate();

//        songRepository = new Injector().provideSongRepository(this);


        audioAttributes = new AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build();

        playEventListener = new PlayEventListener();

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setAudioAttributes(audioAttributes, true);
        exoPlayer.setHandleAudioBecomingNoisy(true);
        exoPlayer.addListener(playEventListener);

//        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, MUSIC_USER_AGENT), null);
        dataSourceFactory = new DefaultDataSource.Factory(this);

        Intent sessionIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0, new Intent[]{sessionIntent}, PendingIntent.FLAG_IMMUTABLE);

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, "Music Service");
        mediaSession.setActive(true);
        mediaSession.setSessionActivity(pendingIntent);

        MediaSessionCompat.Token sessionToken = mediaSession.getSessionToken();

        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        mediaSessionConnector.setPlaybackPreparer(new MusicPlaybackPreparer());
        mediaSessionConnector.setQueueNavigator(new MusicNavigator(mediaSession));

        switchToPlayer(null, exoPlayer);

        notificationManager = new MusicNotificationManager(this, mediaSession.getSessionToken(), new PlayerNotificationListener());

        notificationManager.showNotificationForPlayer(currentPlayer);

        setSessionToken(sessionToken);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // nothing
    }

    private class MusicPlaybackPreparer implements MediaSessionConnector.PlaybackPreparer {

        @Override
        public long getSupportedPrepareActions() {
            return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID |
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                    PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH |
                    PlaybackStateCompat.ACTION_PLAY_FROM_URI;
        }

        @Override
        public void onPrepare(boolean playWhenReady) {
            // nothing
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, boolean playWhenReady, @Nullable Bundle extras) {
            List<Song> songList = SongRepository.getSongList();
            Song find =
                songList
                    .stream().filter(s -> Long.toString(s.id).equals(mediaId)).findFirst().get();
            if(find != null) {
                MediaMetadataCompat itemToPlay = song2MediaMetadataCompat(find);
                List<MediaMetadataCompat> songMetadataList = songList2MediaMetadataCompat(songList);

                preparePlayList(songMetadataList, itemToPlay, playWhenReady, 0);
            }
            else {
                Log.e("Service", "Can not find media to play.");
            }
        }

        @Override
        public void onPrepareFromSearch(String query, boolean playWhenReady, @Nullable Bundle extras) {
            // nothing
        }

        @Override
        public void onPrepareFromUri(Uri uri, boolean playWhenReady, @Nullable Bundle extras) {
            // nothing
        }

        @Override
        public boolean onCommand(Player player, String command, @Nullable Bundle extras, @Nullable ResultReceiver cb) {
            return false;
        }
    }

    private class MusicNavigator extends TimelineQueueNavigator {
        public MusicNavigator(MediaSessionCompat mediaSession) {
            super(mediaSession);
        }

        @Override
        public MediaDescriptionCompat getMediaDescription(Player player, int windowIndex) {
            return currentPlayListItems.get(windowIndex).getDescription();
        }
    }

    private class PlayEventListener implements Player.Listener {
        @Override
        public void onPlaybackStateChanged(@Player.State int state) {
            if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
                notificationManager.showNotificationForPlayer(currentPlayer);

//                if(state == Player.STATE_READY) {
//                    if(@)
//                }
            } else {
                notificationManager.hindNotification();
                // hide notification
            }
        }

        @Override
        public void onPlayerError(PlaybackException error){
            String errorMsg = error.getMessage();
            Log.e("Service", "onPlayerError");
            error.printStackTrace();
            Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void switchToPlayer(Player previousPlayer, @NonNull Player newPlayer) {
        if(previousPlayer != null && previousPlayer.equals(newPlayer))
            return;

        currentPlayer = newPlayer;
        if(previousPlayer != null) {
            int playbackState = previousPlayer.getPlaybackState();
            if(currentPlayListItems == null || currentPlayListItems.size() == 0) {
                currentPlayer.stop();
            }
            else if(playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
                preparePlayList(currentPlayListItems,
                                currentPlayListItems.get(previousPlayer.getCurrentMediaItemIndex()),
                                previousPlayer.getPlayWhenReady(),
                                previousPlayer.getCurrentPosition());
            }
            previousPlayer.stop();
        }
        mediaSessionConnector.setPlayer(currentPlayer);
    }

    private void preparePlayList(List<MediaMetadataCompat> metadataList, MediaMetadataCompat itemToPlay, boolean playWhenReady, long playbackStartPositionMs) {
        int initWindowIndex = itemToPlay == null ? 0 : -1;
        for (int i = 0; i < metadataList.size(); i++) {
            if(metadataList.get(i).getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).equals(itemToPlay.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))){
                initWindowIndex = i;
                break;
            }
        }

        currentPlayListItems = metadataList;
        Log.e("currentPlayListItems", "currentPlayListItemsOnChanged");

        currentPlayer.setPlayWhenReady(playWhenReady);
        currentPlayer.stop();
        if(currentPlayer.equals(exoPlayer)) {
            MediaSource mediaSource = metadataList2ConcatenatingMediaSource(metadataList, dataSourceFactory);
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();
            exoPlayer.seekTo(initWindowIndex, playbackStartPositionMs);
        }
    }

    private class PlayerNotificationListener implements PlayerNotificationManager.NotificationListener {
        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
//            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);
            if(ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), MusicService.class));
                startForeground(notificationId, notification);
                isForegroundService = true;
            }
        }

        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
//            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            stopForeground(true);
            isForegroundService = false;
            stopSelf();
        }
    }

    private MediaMetadataCompat song2MediaMetadataCompat(Song song) {
        MediaMetadataCompat.Builder mediaMetadataCompatBuilder = new MediaMetadataCompat.Builder();

        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, Long.toString(song.id));
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.coverPath);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.contentUri.toString());
        mediaMetadataCompatBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration);

        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artistName);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, song.albumName);
        mediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, song.coverPath);

        return mediaMetadataCompatBuilder.build();
    }

    private List<MediaMetadataCompat> songList2MediaMetadataCompat(List<Song> songList) {
        return songList.stream()
                .map(s -> song2MediaMetadataCompat(s))
                .collect(Collectors.toList());
    }

    private MediaSource metadataCompat2MediaSource(MediaItem mediaUri, DataSource.Factory dataSourceFactory) {
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaUri);
    }

    private ConcatenatingMediaSource metadataList2ConcatenatingMediaSource(List<MediaMetadataCompat> metadataList, DataSource.Factory dataSourceFactory){
        ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
        metadataList.forEach((metadata) -> {
            concatenatingMediaSource.addMediaSource(
                    metadataCompat2MediaSource(MediaItem.fromUri(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)), dataSourceFactory));
        });
        return concatenatingMediaSource;
    }
}
