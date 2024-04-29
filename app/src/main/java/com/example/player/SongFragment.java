package com.example.player;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
public class SongFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_SONG_LIST = "songList";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private List<Song> thisSongList;
    private SongViewModel songViewModel;
    private NowPlayingViewModel nowPlayingViewModel;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SongFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static SongFragment newInstance(List<Song> songList) {
        SongFragment fragment = new SongFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG_LIST, (ArrayList<Song>)songList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            thisSongList = (ArrayList<Song>) getArguments().getSerializable(ARG_SONG_LIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song_list, container, false);

        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);
        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
//        if(thisSongList != null)
//            try {
//                songViewModel.loadSongs();
//            }catch (Exception e) {
//                Toast.makeText(getContext(), "Error occurred while loading songs", Toast.LENGTH_LONG).show();
//            }

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            if(thisSongList != null)
                recyclerView.setAdapter(new SongRecyclerViewAdapter(thisSongList, (Song song) -> {
                    songViewModel.playMedia(song, true);
                    getActivity().getSupportFragmentManager().beginTransaction().add(R.id.frameLayout, NowPlayingFragment.newInstance(), "NowPlayingFragment").commit();
                    nowPlayingViewModel.nowPlayingShowed.postValue(true);
                }));

//            Resources r = getResources();
//            int PX_68 = (int) TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    68,
//                    r.getDisplayMetrics()
//            );

//            Resources r = getResources();
//            int PX_68 = (int) TypedValue.applyDimension(
//                    TypedValue.COMPLEX_UNIT_DIP,
//                    68,
//                    r.getDisplayMetrics()
//            );
//            nowPlayingViewModel.nowPlayingShowed.observe(getActivity(), new Observer<Boolean>() {
//                @Override
//                public void onChanged(Boolean aBoolean) {
//                    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(recyclerView.getLayoutParams());
//                    if (aBoolean) {
//                        p.setMargins(0, 0, 0, PX_68);
//                    } else {
//                        p.setMargins(0, 0, 0, 0);
//                    }
//                    recyclerView.setLayoutParams(p);
//                }
//            });

//            songViewModel.getSongs().observe(getActivity(), new Observer<ArrayList<Song>>() {
//                @Override
//                public void onChanged(ArrayList<Song> songs) {
//                    recyclerView.setAdapter(new SongRecyclerViewAdapter(thisSongList, (Song song) -> {
//                        NowPlayingViewModel.NowPlayingMetadata metadata = nowPlayingViewModel.mediaMetadata.getValue();
//                        if(metadata == null || !Long.toString(song.id).equals(metadata.id)) {
//                            songViewModel.playMedia(song, true);
//                            getParentFragmentManager().beginTransaction().add(R.id.frameLayout, NowPlayingFragment.newInstance(), "NowPlayingFragment").commit();
//                            nowPlayingViewModel.nowPlayingShowed.postValue(true);
//                        }
//                    }));
//                }
//            });
        }
        return view;
    }
}