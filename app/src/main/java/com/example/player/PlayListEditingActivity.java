package com.example.player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.example.player.databinding.PlaylistEditBinding;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PlayListEditingActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private Button cancel, save;
    private List<Map.Entry> playlistValue;
    private long id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_list_editing);

        Bundle bundle = getIntent().getExtras();

        if(bundle == null) {
            Log.e("Playlists update activity", "no bundle found, return.");
            finish();
        }
        else {
            id = bundle.getLong("id", -1);
            String[] names = bundle.getStringArray("names");
            boolean[] flags = bundle.getBooleanArray("flags");
            if(names == null || flags == null || id == -1) {
                Log.e("Playlists update activity", "no information found, return.");
                finish();
            }
            else if(names.length != flags.length) {
                Log.e("Playlists update activity", "incorrect information, return.");
                finish();
            }
            playlistValue = new ArrayList<>();
            for(int i = 0; i < names.length; i++) {
                Map.Entry entry = new AbstractMap.SimpleEntry<>(names[i], flags[i]);
                playlistValue.add(entry);
            }
            playlistValue.sort(new Comparator<Map.Entry>() {
                @Override
                public int compare(Map.Entry o1, Map.Entry o2) {
                    return ((String)o1.getKey()).toUpperCase().compareTo(((String)o2.getKey()).toUpperCase());
                }
            });
        }

        recyclerView = findViewById(R.id.playlistRecyclerView);
        cancel = findViewById(R.id.playlistCancelBtn);
        save = findViewById(R.id.playlistSaveBtn);

        cancel.setOnClickListener(v -> {
            this.finish();
        });

        save.setOnClickListener(v -> {
            ArrayList<String> arrayList = new ArrayList<>();
            for(int i = 0; i < playlistValue.size(); i++) {
                View view = recyclerView.getChildAt(i);
                if(view != null) {
                    CheckBox checkBox = view.findViewById(R.id.checkBox);
                    if(checkBox.isChecked() != (boolean) playlistValue.get(i).getValue()) {
                        arrayList.add((String)playlistValue.get(i).getKey());
                    }
                }
            }
            Intent returnIntent = new Intent();
            Bundle b = new Bundle();
            b.putBoolean("refresh", true);
            b.putStringArrayList("names", arrayList);
            b.putLong("id", id);
            returnIntent.putExtras(b);
            setResult(RESULT_OK, returnIntent);

            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MyPlaylistRecyclerViewAdapter(playlistValue));
    }


    private class MyPlaylistRecyclerViewAdapter extends RecyclerView.Adapter<MyPlaylistRecyclerViewAdapter.ViewHolder> {

        private final List<Map.Entry> playlistValue;

        private MyPlaylistRecyclerViewAdapter(List<Map.Entry> playlistValue) {
            this.playlistValue = playlistValue;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(PlaylistEditBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.name = (String) this.playlistValue.get(position).getKey();
            holder.flag = (boolean) this.playlistValue.get(position).getValue();

            holder.checkBox.setText(holder.name);
            holder.checkBox.setChecked(holder.flag);
        }

        @Override
        public int getItemCount() {
            return this.playlistValue.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final CheckBox checkBox;
            public String name;
            public boolean flag;

            public ViewHolder(PlaylistEditBinding binding) {
                super(binding.getRoot());
                checkBox = binding.checkBox;
            }

            @Override
            public String toString() {
                return super.toString() + " '" + name + "' "+flag;
            }
        }
    }
}