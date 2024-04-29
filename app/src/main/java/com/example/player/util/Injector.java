package com.example.player.util;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.example.player.repository.SongRepository;
import com.example.player.service.MusicService;
import com.example.player.service.MusicServiceConnection;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;

public class Injector {
    private MusicServiceConnection provideMusicServiceConnection(Context context) {
        return MusicServiceConnection.getInstance(context, new ComponentName(context, MusicService.class));
    }

    public SongRepository provideSongRepository(Context context) {
        return new SongRepository(context);
    }

    public SongViewModel.Factory provideSongViewModel(Context context) {
        return new SongViewModel.Factory(context.getContentResolver(), provideSongRepository(context), provideMusicServiceConnection(context));
    }

    public NowPlayingViewModel.Factory provideNowPlayingViewModel(Context context) {
        return new NowPlayingViewModel.Factory(context, provideMusicServiceConnection(context));
    }
}
