package com.example.player.roomdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DataUao {
    String song_table_name = "CustomMetadata";
    String album_table_name = "CustomAlbum";
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    void insertMetadata(MyMetadata myMetadata);

    @Query("INSERT INTO "+song_table_name+" (id, title, artist, album) VALUES (:id, :title, :artist, :album)")
    void insertMetadata(long id, String title, String artist, String album);

    @Query("INSERT INTO "+album_table_name+" (albumName, cover) VALUES (:albumName, :cover)")
    void insertAlbum(String albumName, String cover);

    @Query("SELECT A.id AS id, A.title AS title, A.artist AS artist, A.album AS album, B.cover AS cover " +
            "FROM "+song_table_name+" AS A " +
            "INNER JOIN "+album_table_name+" AS B " +
            "ON A.album = B.albumName")
    List<JoinedData> selectAll();

    @Query("SELECT A.id, A.title, A.artist, A.album " +
            "FROM "+song_table_name+" AS A")
    List<MyMetadata> selectAllSong();

    @Query("SELECT A.albumName, A.cover " +
            "FROM "+album_table_name+" AS A")
    List<MyAlbumData> selectAllAlbum();

    @Query("SELECT A.id, A.title, A.artist, A.album " +
            "FROM "+song_table_name+" AS A " +
            "WHERE A.id = :id")
    List<MyMetadata> selectSong(long id);

    @Query("SELECT COUNT(*)" +
            "FROM "+song_table_name+" AS A " +
            "WHERE A.id=:id")
    int songCount(long id);

    @Query("SELECT COUNT(*)" +
            "FROM "+album_table_name+" AS A " +
            "WHERE A.albumName=:album")
    int albumCount(String album);

    @Query("UPDATE "+song_table_name +
            " SET " +
                "title = :title," +
                "artist = :artist," +
                "album = :album " +
            "WHERE id=:id")
    void updateSong(long id, String title, String artist, String album);

    @Query("UPDATE "+album_table_name+" " +
            "SET cover = :cover " +
            "WHERE albumName = :albumName")
    void updateAlbum(String  albumName, String cover);

    static class JoinedData {
        public long id;
        public String title;
        public String artist;
        public String album;
        public String cover;
    }
}
