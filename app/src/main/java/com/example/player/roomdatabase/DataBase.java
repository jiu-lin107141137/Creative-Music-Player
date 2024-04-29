package com.example.player.roomdatabase;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MyMetadata.class, MyAlbumData.class}, version = 1, exportSchema = false)
public abstract class DataBase extends RoomDatabase {
    public static final String METADATA_DB_NAME= "MyMetadata.db";
    private static volatile DataBase instance;

    public static synchronized DataBase getInstance(Context context) {
        if(instance == null)
            instance = create(context);

        return instance;
    }

    private static DataBase create(final Context context) {
        return Room.databaseBuilder(context, DataBase.class, METADATA_DB_NAME).build();
    }

    public abstract DataUao getDataUao();
}
