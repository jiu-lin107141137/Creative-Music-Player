package com.example.player;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.player.repository.SongRepository;
import com.example.player.roomdatabase.MyMetadata;
import com.example.player.service.MusicService;
import com.example.player.util.Injector;
import com.example.player.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.List;
//import com.arthenica.ffmpegkit.FFmpegKit;
//import com.arthenica.ffmpegkit.FFmpegKitConfig;
//import com.arthenica.ffmpegkit.ReturnCode;

public class MetadataActivity extends AppCompatActivity {
    EditText titleTxt, artistTxt, albumTxt;
    Button cancelBtn, saveBtn;
    ImageView albumCover;

    String id;
    Uri path;
    boolean coverChanged;
    ActivityResultLauncher<String> pickMedia;
//    ActivityResultLauncher updateInfo;

    SongRepository songRepository;
    SongViewModel songViewModel;
//    RecyclerView playListRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata);

        titleTxt = findViewById(R.id.titleTxt);
        artistTxt = findViewById(R.id.artistTxt);
        albumTxt = findViewById(R.id.albumTxt);
        cancelBtn = findViewById(R.id.cancelBtn);
        saveBtn = findViewById(R.id.saveBtn);
        albumCover = findViewById(R.id.albumCover);

        coverChanged = false;

        songViewModel = new ViewModelProvider(this, new Injector().provideSongViewModel(getApplicationContext())).get(SongViewModel.class);
//        songRepository = new SongRepository(getApplicationContext());
        songRepository = new Injector().provideSongRepository(getApplicationContext());

        Bundle bundle = getIntent().getExtras();

        if(bundle == null) {
            Log.e("Metadata update activity", "no bundle found, return.");
            finish();
        }
        else {
            id = bundle.getString("id");
            titleTxt.setText(bundle.getString("title"));
            albumTxt.setText(bundle.getString("album"));
            artistTxt.setText(bundle.getString("artist"));
            String path_tmp = bundle.getString("path", "none");
            if(!"none".equals(path_tmp))
                path = Uri.parse(path_tmp);
        }

        if(path != null)
            try {
                Glide.with(this)
                        .load(path)
                        .into(albumCover);
            }catch (Exception e) {
                Log.e("Metadata update activity", "load image failed!");
                e.printStackTrace();
            }


        pickMedia =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {
                            if(uri != null) {
//                                Log.e("photo picker", uri.getPath());
                                coverChanged = true;
                                long imageId = Long.parseLong(uri.getPath().split(":")[1]);
                                Uri u = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);
                                Glide.with(this)
                                        .load(u)
                                        .into(albumCover);
                                path = u;
                            }
                            else {
                                Log.d("photo picker", "no media selected.");
                            }
                        });
//        updateInfo =
//                registerForActivityResult(
//                        new ActivityResultContracts.StartIntentSenderForResult(),
//                        result -> {
//                            if(result != null) {
//                                updateMetadata();
//                                Intent returnIntent = new Intent();
//                                Bundle b = new Bundle();
//                                b.putBoolean("refresh", true);
//                                returnIntent.putExtras(b);
//                                setResult(Activity.RESULT_OK, returnIntent);
//                                finish();
//                            }
//                        }
//                );


        albumCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                askForPermission();
                updateMetadata();
                finish();
            }
        });
    }

    private void updateMetadata() {
//        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));

        String title = titleTxt.getText().toString();
        String artist = artistTxt.getText().toString();
        String album = albumTxt.getText().toString();

        songRepository.update(Long.parseLong(id), title, artist, album, coverChanged ? path.toString() : null);

        Intent returnIntent = new Intent();
        Bundle b = new Bundle();
        b.putBoolean("refresh", true);
        b.putString("title", title);
        b.putString("artist", artist);
        b.putString("album", album);
        b.putString("cover", path.toString());
        returnIntent.putExtras(b);
        setResult(Activity.RESULT_OK, returnIntent);
    }

//    private void test(Uri uri) {
//        String[] projections = new String[] {
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.ALBUM,
//                MediaStore.Audio.Media.ARTIST
//        };
//        Cursor cursor = getContentResolver().query(uri, projections, null, null);
//        if(cursor != null) {
//            cursor.moveToFirst();
//            while(!cursor.isAfterLast()) {
//                Log.e("test", "title = "+cursor.getString(cursor.getColumnIndexOrThrow(projections[0])));
//                Log.e("test", "album = "+cursor.getString(cursor.getColumnIndexOrThrow(projections[1])));
//                Log.e("test", "artist = "+cursor.getString(cursor.getColumnIndexOrThrow(projections[1])));
//                cursor.moveToNext();
//            }
//        }
//    }

    private void openGallery() {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        pickMedia.launch(Intent.createChooser(intent, "Select image from gallery."));
        pickMedia.launch("image/*");
    }

//    private void askForPermission() {
//        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
//
//        List<Uri> uris = new ArrayList<>();
//        uris.add(songUri);
//
//        try {
//            PendingIntent pendingIntent = MediaStore.createWriteRequest(getContentResolver(), uris);
//            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
////            startIntentSenderForResult();
//            updateInfo.launch(intentSenderRequest);
//        } catch (Exception e) {
//            Log.e("update", "error");
//            e.printStackTrace();
//        }
//    }

//    private void updateMetadata() {
//        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
//
//        String title = titleTxt.getText().toString();
//        String artist = artistTxt.getText().toString();
//        String album = albumTxt.getText().toString();
//        String selection = MediaStore.Audio.Media._ID+" = ?";
//        String[] selectionArgs = new String[] {
//                id
//        };
//
//        try {
//            ContentValues contentValues = new ContentValues();
//            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 1);
//            int n = getContentResolver().update(songUri, contentValues, null, null);
//            getContentResolver().notifyChange(songUri, null);
//            Log.e("metadataActivity", "n = "+n);
////            getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues, selection, selectionArgs);
////            contentValues.clear();
//            contentValues.put(MediaStore.Audio.Media.TITLE, title);
//            contentValues.put(MediaStore.Audio.Media.ALBUM, album);
//            contentValues.put(MediaStore.Audio.Media.ARTIST, artist);
//            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0);
////            getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues, selection, selectionArgs);
//            n = getContentResolver().update(songUri, contentValues, null, null);
//            getContentResolver().notifyChange(songUri, null);
//            Log.e("metadataActivity", "n = "+n);
//
//            MediaScannerConnection.scanFile(this, new String[]{songUri.toString()}, null, null);
//
//            test(songUri);
//
//
//        }catch(Exception e) {
//            if(!(e instanceof RecoverableSecurityException)){
//                Toast.makeText(getApplicationContext(), "Permissions may bot granted.", Toast.LENGTH_SHORT).show();
//                e.printStackTrace();
//            }
//            else {
//                try {
//                    ContentResolver cr = getContentResolver();
//                    ContentValues contentValues = new ContentValues();
//                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 1);
//                    getContentResolver().update(songUri, contentValues, null, null);
////                    getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues, selection, selectionArgs);
//                    contentValues.clear();
//                    contentValues.put(MediaStore.Audio.Media.TITLE, title);
//                    contentValues.put(MediaStore.Audio.Media.ALBUM, album);
//                    contentValues.put(MediaStore.Audio.Media.ARTIST, artist);
//                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0);
//                    getContentResolver().update(songUri, contentValues, null, null);
////                    getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues, selection, selectionArgs);
//                } catch (Exception e2) {
//                    Log.e("Update metadata", "Error occurred");
//                    e2.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Error occurred while updating metadata.", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    }

//    public void updateMetadata2() {
//        FFmpegKitConfig.
//    }

}