package com.example.player.repository;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;

import com.example.player.MainActivity;
import com.example.player.roomdatabase.DataBase;
import com.example.player.roomdatabase.DataUao;
import com.example.player.roomdatabase.MyAlbumData;
import com.example.player.roomdatabase.MyMetadata;
import com.example.player.util.Song;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class SongRepository {
    Context context;
    private static List<Song> songList;

    public SongRepository(Context context){
        this.context = context;
    }

    public List<Song> loadSongs() {
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
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
                "upper("+MediaStore.Audio.Media.TITLE+") ASC");

        if (cur == null) {
            cur.close();
            return new ArrayList<Song>();
        }

//        List<Song> ls = new ArrayList<>();
        Map<Long, Song> map = new HashMap<>();
        cur.moveToFirst();
        while(!cur.isAfterLast()) {
            try {
                long id = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                String title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                long duration = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                String artist = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String album = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                long albumId = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                String coverPath = getAlbumCoverPathFromAlbumId(cr, albumId);
                Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
//                Log.e("load songs", title+" "+artist);

                map.put(id, new Song(id, title, duration, artist, albumId, album, coverPath, contentUri));
            }catch(Exception e) {
                Log.e("fetching", "error occurred while fetching data from MediaStore.");
                e.printStackTrace();
            }
            cur.moveToNext();
        }

        DataUao dataUao = DataBase.getInstance(context).getDataUao();
        List<MyMetadata> metadataList = dataUao.selectAllSong();
//        Log.e("metadataList", "size = "+metadataList.size());
        for(MyMetadata metadata: metadataList) {
            if(map.containsKey(metadata.getId())) {
                Song tmp = map.get(metadata.getId());
                tmp.title = metadata.getTitle();
                tmp.artistName = metadata.getArtist();
                tmp.albumName = metadata.getAlbum();
                map.put(metadata.getId(), tmp);
            }
        }

        Map<String, String> albumDataMap = dataUao.selectAllAlbum().stream()
                                                .collect(Collectors.toMap(MyAlbumData::getAlbumName, MyAlbumData::getCover));
//        List<MyAlbumData> albumDataList = dataUao.selectAllAlbum();
        List<Song> ls = map.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
        for(int i = 0; i < ls.size(); i++) {
            if(albumDataMap.containsKey(ls.get(i).albumName)) {
                Song tmp = ls.get(i);
                tmp.coverPath = albumDataMap.get(tmp.albumName);
                ls.set(i, tmp);
            }
        }

        ls.sort(new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.title.toUpperCase().compareTo(o2.title.toUpperCase());
            }
        });

//        for(Song song: ls)
//            Log.e("Song repository", song.title+" / "+song.artistName+" / "+song.albumName);

        return ls;
    }

    public HashMap<String, List<Song>> loadPlaylist(List<Song> currentSongList) {
        HashMap<String, List<Song>> rt = new HashMap<>();
        HashMap<Long, Song> map = new HashMap<>();

        // check if the dir existed;
        File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "playLists");
        if(!dir.exists() || !dir.isDirectory()) {
            Log.e("loadPlaylist", "dir not found!");
            return rt;
        }

        // list to set
        for(Song song: currentSongList) {
            map.put(song.id, song);
        }

        File[] files = dir.listFiles();
        for(File file: files) {
            if(file.isFile() && isM3u(file.getName())) {
                List<Song> list = new ArrayList<>();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
//                    String str;
//                    while((str = br.readLine()) != null)
//                        Log.e("load", str);

                    String str = br.readLine();
                    if(str == null || !"#EXTM3U".equals(str)) {
                        Log.e("loadPlayLists", "file prefix wrong! ");
                        continue;
                    }
                    str = br.readLine();
                    String listName = str.split(":")[1];
                    while((str = br.readLine()) != null) {
                        if(str.startsWith("#EXTINF")) {
                            String tmp = str.substring(str.lastIndexOf(',')+1);
                            long songId = -1;
                            try {
                                songId = Long.parseLong(tmp);
                            }catch(Exception e) {
                                Log.e("loadPlayList", "Cannot parse song id from string to long!");
                                continue;
                            }
                            if(songId != -1 && map.containsKey(songId))
                                list.add(map.get(songId));
                        }
                    }
                    rt.put(listName, list);
                } catch (FileNotFoundException e) {
                    // no way to happen
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return rt;
    }

    private boolean isM3u(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.')+1);
        return extension.equals("m3u");
    }

    public void addPlayList(@NonNull String name) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory() + File.separator + "playLists");
            if(!dir.exists() && !dir.isDirectory()) {
                if(dir.mkdir())
                    Log.e("addPlayList", "create dir successfully!");
                else {
                    Log.e("addPlayList", "Unable to create dir!");
                    return;
                }
            }

            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "playLists", name+".m3u");
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("#EXTM3U\n");
            bw.write("#PLAYLIST:"+name+"\n");
//            for (String song : songList) {
//                bw.write(song);
//                bw.newLine();
//            }
            bw.close();
            fw.close();
//            Toast.makeText(this, "M3U file created successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e("addPlayList", "Error occurred on adding "+name+"!");
            e.printStackTrace();
//            Toast.makeText(this, "Error creating M3U file", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean deletePlayList(String name) {
        File target = new File(Environment.getExternalStorageDirectory()+File.separator+"playLists", name+".m3u");
        if(!target.exists() || !target.isFile()) {
            Log.e("deletePlayList", "Cannot find the target or the target is not a file");
            return false;
        }
        return target.delete();
    }

    public boolean renamePlayList(String oldName, String newName){
        File target = new File(Environment.getExternalStorageDirectory()+File.separator+"playLists", oldName+".m3u");
        if(!target.exists() || !target.isFile()) {
            Log.e("renamePlayList", "Cannot find the target or the target is not a file");
            return false;
        }

        addPlayList(newName);

        try {
            File newFile = new File(Environment.getExternalStorageDirectory()+File.separator+"playLists", oldName+".m3u");
            FileReader fr = new FileReader(target);
            FileWriter fw = new FileWriter(newFile);
            BufferedReader br = new BufferedReader(fr);
            BufferedWriter bw = new BufferedWriter(fw);

            br.readLine();
            br.readLine();
            String str;
            while((str = br.readLine()) != null) {
                bw.write(str);
            }

            br.close();
            bw.close();
            fr.close();
            fw.close();
        } catch(Exception e) {
            Log.e("renamePlayList", "Error occurred on renaming "+oldName+" to "+newName+"!");
            e.printStackTrace();

            deletePlayList(newName);
            return false;
        }

        return true;
    }

    public void updatePlayList(long id, Map<String, List<Song>> lists, List<String> toChange) {
        for (String name: toChange) {
            if(!lists.containsKey(name)) {
                Log.e("updatePlayList", "List "+name+" does not exist.");
                continue;
            }
            File target = new File(Environment.getExternalStorageDirectory()+File.separator+"playLists", name+".m3u");
            if(!target.exists() || !target.isFile()) {
                Log.e("updatePlayList", "Cannot find the target or the target is not a file");
                continue;
            }

            List<Song> targetList = lists.get(name);
            int ptr = -1;
            for(int i = 0; i < targetList.size(); i++)
                if(targetList.get(i).id == id) {
                    ptr = i;
                    break;
                }
            // not exist
            // write to the file
            if(ptr == -1) {
                List<String> tmp = new ArrayList<>();
                try {
                    FileReader fr = new FileReader(target);
                    BufferedReader br = new BufferedReader(fr);
                    String str;
                    while((str = br.readLine()) != null) {
                        tmp.add(str);
                    }
                    br.close();
                    fr.close();

                    FileWriter fw = new FileWriter(target);
                    BufferedWriter bw = new BufferedWriter(fw);
                    for(String s: tmp) {
                        bw.write(s);
                        bw.newLine();
                    }
                    bw.write("#EXTINF: -1,"+id);
                    bw.close();
                    fw.close();
                } catch (Exception e) {
                    Log.e("updatePlayList", "Error occurred "+name);
                    e.printStackTrace();
                }
            }
            // exist
            else {
                targetList.remove(ptr);
                try {
                    FileWriter fw = new FileWriter(target);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write("#EXTM3U\n");
                    bw.write("#PLAYLIST:"+name+"\n");
                    for(Song s: targetList) {
                        bw.write("#EXTINF: -1,"+s.id);
                        bw.newLine();
                    }
                    bw.close();
                    fw.close();
                } catch (Exception e) {
                    Log.e("updatePlayList", "Error occurred "+name);
                    e.printStackTrace();
                }
            }
        }
    }

    public void update(long id, String title, String artist, String album, String albumCover) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                DataUao dataUao = DataBase.getInstance(context).getDataUao();
                boolean songExist, albumExist;
                songExist = dataUao.songCount(id) > 0;
                albumExist = dataUao.albumCount(album) > 0;
                if(songExist)
                    dataUao.updateSong(id, title, artist, album);
                else
                    dataUao.insertMetadata(id, title, artist, album);

                MyMetadata tmp = dataUao.selectSong(id).get(0);
//                Log.e("artist", artist+" / "+tmp.getArtist());

                if(albumExist)
                    dataUao.updateAlbum(album, albumCover == null ? Uri.parse("android.resource://com.example.player/drawable/image_place_holder").toString() : albumCover);
                else
                    dataUao.insertAlbum(album, albumCover == null ? Uri.parse("android.resource://com.example.player/drawable/image_place_holder").toString() : albumCover);

//                Log.e("artist", artist);
//                Log.e("Update song metadata", "finished");
            }
        });
        executorService.shutdown();
    }

    public String getPathFromId(long id) {
        String[] projection = { MediaStore.Audio.Media.DATA };
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = { String.valueOf(id) };

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }

        return null;
    }

    private String getAlbumCoverPathFromAlbumId(ContentResolver cr, long albumId) {
        Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
        Uri coverUri = ContentUris.withAppendedId(albumArtUri, albumId);

        try {
            InputStream inputStream = cr.openInputStream(coverUri);
            if(inputStream != null)
                inputStream.close();
//            Log.e("uri", coverUri.toString());
            return coverUri.toString();
        } catch (Exception e) {
//            Log.e("getAlbumCoverPathFromAlbumId", "Error occurred!");
            return Uri.parse("android.resource://com.example.player/drawable/image_place_holder").toString();
            //            e.printStackTrace();
//            return "";
        }
    }

    public static List<Song> getSongList() {
        if(songList == null)
            songList = new ArrayList<>();
        return songList;
    }

    public static void setSongList (List<Song> list) {
        songList = list;
    }
}
