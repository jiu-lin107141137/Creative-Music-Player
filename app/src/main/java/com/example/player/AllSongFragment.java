package com.example.player;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class AllSongFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private SongViewModel songViewModel;
    private NowPlayingViewModel nowPlayingViewModel;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public AllSongFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static AllSongFragment newInstance(int columnCount) {
        AllSongFragment fragment = new AllSongFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song_list, container, false);

        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);
        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
        if(songViewModel != null)
            try {
                songViewModel.loadSongs();
            }catch (Exception e) {
                Log.e("AllSongFragment", "Error occurred while calling loadSongs on viewModel.");
                e.printStackTrace();
                Toast.makeText(getContext(), "Error occurred while loading songs", Toast.LENGTH_SHORT).show();
            }

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            List<Song> ls = songViewModel.getSongs().getValue();
            if(ls != null)
                recyclerView.setAdapter(new SongRecyclerViewAdapter(ls, (Song song) -> {
                    if(!Long.toString(song.id).equals(nowPlayingViewModel.mediaMetadata.getValue().id)) {
                        songViewModel.playMedia(song, true);
                        getParentFragmentManager().beginTransaction().add(R.id.frameLayout, NowPlayingFragment.newInstance(), "NowPlayingFragment").commit();
                        nowPlayingViewModel.nowPlayingShowed.setValue(true);
                    }
                }));
//
            songViewModel.getSongs().observe(getActivity(), new Observer<ArrayList<Song>>() {
                @Override
                public void onChanged(ArrayList<Song> songs) {
                    recyclerView.setAdapter(new SongRecyclerViewAdapter(songs, (Song song) -> {
                        NowPlayingViewModel.NowPlayingMetadata metadata = nowPlayingViewModel.mediaMetadata.getValue();
                        if(metadata == null || !Long.toString(song.id).equals(metadata.id)) {
                            songViewModel.playMedia(song, true);
                            getParentFragmentManager().beginTransaction().add(R.id.frameLayout, NowPlayingFragment.newInstance(), "NowPlayingFragment").commit();
                            nowPlayingViewModel.nowPlayingShowed.postValue(true);
                        }
                    }));
                }
            });
        }
        return view;
    }
}