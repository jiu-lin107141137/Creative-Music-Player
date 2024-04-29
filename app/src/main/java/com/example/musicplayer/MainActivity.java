package com.example.musicplayer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
        loadSongs();

        // listen to any change in MediaStore
//        HandlerThread mediaStoreOnChangeListenerThread = new HandlerThread("mediaStoreOnChangeListenerThread");
//        mediaStoreOnChangeListenerThread.start();
//        Handler mediaStoreOnChangeListenerHandler = new Handler(mediaStoreOnChangeListenerThread.getLooper());
//        ContentObserver mediaStoreOnChangeListener = new ContentObserver(mediaStoreOnChangeListenerHandler) {
//            @Override
//            public void onChange(boolean selfChange, @Nullable Uri uri) {
//                if (uri.equals(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI))
//                    loadSongs();
//            }
//        };
//        ContentResolver resolver = getContentResolver();
//        resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreOnChangeListener);
    }

    public void loadSongs() {
        HandlerThread initHandlerThread = new HandlerThread("initHandlerThread");
        initHandlerThread.start();
        Handler initHandler = new Handler(initHandlerThread.getLooper());
        initHandler.post(new Runnable() {
            @Override
            public void run() {
                loadSongsFromDb();
                initHandlerThread.interrupt();
            }
        });
    }

    private void loadSongsFromDb() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID
                },
                (
                    MediaStore.Audio.Media.IS_ALARM + " == 0 AND " +
                    MediaStore.Audio.Media.IS_TRASHED + " == 0 AND " +
                    MediaStore.Audio.Media.IS_RINGTONE + " == 0 AND " +
                    MediaStore.Audio.Media.IS_DOWNLOAD + " == 0 AND " +
                    MediaStore.Audio.Media.IS_RECORDING + " == 0 AND " +
                    MediaStore.Audio.Media.DATA + " NOT LIKE '%ringtone%' AND " +
                    MediaStore.Audio.Media.DATA + " NOT LIKE '%LOST.DIR%' AND " +
                    MediaStore.Audio.Media.DATA + " NOT LIKE '%Recording%' "
                ),
                null,
                MediaStore.Audio.Media.TITLE+" DESC");

        if (cur == null) {
            cur.close();
            return;
        }

        while(!cur.isAfterLast()) {
            try {
                long id = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                long albumId = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
//                String coverPath = getAlbumCoverPathFromAlbumId(cr, albumId);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            }catch(Exception e) {
                Log.e("fetching", "error occurred while fetching data from MediaStore.");
                e.printStackTrace();
            }
        }
    }

//    private String getAlbumCoverPathFromAlbumId(ContentResolver cr, long albumId) {
//
//    }

    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));

                startActivityForResult(intent, 123, null);
            }
        }

        if(Build.VERSION.SDK_INT >= 31){
            checkPermission(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
            }, 1);
        }
        else if (Build.VERSION.SDK_INT == 30){
            checkPermission(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
            },1);
        }
        else{
            checkPermission(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
            }, 1);
        }
    }

    public void checkPermission(String permissions[], int requestCode) {
        // Checking if permission is not granted
        ArrayList<String> arrayList = new ArrayList <String> ();
        for (String p: permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, p) == PackageManager.PERMISSION_DENIED)
                arrayList.add(p);
        }
        if (arrayList.size() == 0)
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        else {
            permissions = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                permissions[i] = arrayList.get(i);
                Log.e("Permissions", permissions[i]);
            }
            ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
        }
    }
}