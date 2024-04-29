package com.example.player.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.player.R;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MusicNotificationManager {
    private final String CHANNEL_ID = "com.example.player.service.NOW_PLAYING";
    private final int NOTIFICATION_ID = 3216;
    private final int NOTIFICATION_ICON_SIZE = 144;

    private Context context;
    private MediaSessionCompat.Token sessionToken;
    private PlayerNotificationManager.NotificationListener notificationListener;
    private PlayerNotificationManager notificationManager;

    private RequestOptions glideOptions;

    public MusicNotificationManager(Context context, MediaSessionCompat.Token sessionToken, PlayerNotificationManager.NotificationListener notificationListener) {
        this.context = context;
        this.sessionToken = sessionToken;
        this.notificationListener = notificationListener;

        glideOptions = new RequestOptions()
                            .fallback(R.drawable.image_place_holder)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE);

        MediaControllerCompat mediaController = new MediaControllerCompat(this.context, this.sessionToken);
        notificationManager = new PlayerNotificationManager.Builder(context, NOTIFICATION_ID, CHANNEL_ID)
                                    .setChannelNameResourceId(R.string.notification_channel)
                                    .setChannelDescriptionResourceId(R.string.notification_channel_description)
                                    .setMediaDescriptionAdapter(new DescriptionAdapter(mediaController))
                                    .setNotificationListener(notificationListener)
                                    .setSmallIconResourceId(R.drawable.ic_music_note_circle)
                                    .build();

        notificationManager.setMediaSessionToken(sessionToken);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);
//        notificationManager.setColor(Color.YELLOW);
//        notificationManager.setColorized(true);
        notificationManager.invalidate();
    }

    public void hindNotification() {
        notificationManager.setPlayer(null);
    }

    public void showNotificationForPlayer(Player player) {
        notificationManager.setPlayer(player);
    }

    private class DescriptionAdapter implements PlayerNotificationManager.MediaDescriptionAdapter {
        MediaControllerCompat mediaControllerCompat;
        Uri currentIconUri = null;
        Bitmap currentBitMap = null;

        public DescriptionAdapter(MediaControllerCompat mediaControllerCompat) {
            this.mediaControllerCompat = mediaControllerCompat;
        }

        @Override
        public CharSequence getCurrentContentTitle(Player player) {
            return mediaControllerCompat.getMetadata().getDescription().getTitle();
        }

        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            return mediaControllerCompat.getSessionActivity();
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            return mediaControllerCompat.getMetadata().getDescription().getSubtitle();
        }

        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            Uri iconUri = mediaControllerCompat.getMetadata().getDescription().getIconUri();
            if(currentBitMap == null || !currentIconUri.equals(iconUri)) {
                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri;
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future future = executorService.submit(new Runnable() {
                    @Override
                    public void run() {
//                        Log.e("MusicNotificationManager", "getLargeIcon");
                        try {
                            currentBitMap = iconUri == null ? null : resolveUriAsBitmap(iconUri);
                        } catch (Exception e) {
                            Log.e("MusicNotificationManager", "Error occurred while resolveUriAsBitmap!");
                            e.printStackTrace();
                        }
                        if(currentBitMap != null){
                            callback.onBitmap(currentBitMap);
//                            Log.e("MusicNotificationManager", "callback.onBitmap");
                        }
                    }
                });
                executorService.shutdown();
            }
            return currentBitMap;
        }

        private Bitmap resolveUriAsBitmap(Uri uri) throws ExecutionException, InterruptedException {
//            Log.e("iconUri", uri.toString());
            return Glide.with(context)
                        .applyDefaultRequestOptions(glideOptions)
                        .asBitmap()
                        .load(uri)
                        .submit(NOTIFICATION_ICON_SIZE, NOTIFICATION_ICON_SIZE)
                        .get();
        }
    }
}
