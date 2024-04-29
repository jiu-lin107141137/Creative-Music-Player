package com.example.player;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.player.placeholder.PlaceholderContent;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class AlbumFragment extends Fragment {

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
    public AlbumFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static AlbumFragment newInstance(int columnCount) {
        AlbumFragment fragment = new AlbumFragment();
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
        View view = inflater.inflate(R.layout.fragment_album_list, container, false);

        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);

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
                recyclerView.setAdapter(new AlbumRecyclerViewAdapter(classify(ls),
                        (List<Song> songList, String listName) -> {
                            getParentFragment().getParentFragmentManager()
                                    .beginTransaction()
                                    .add(R.id.frameLayout, InnerListContainer.newInstance(0, listName, songList), "Song Fragment")
                                    .addToBackStack("Song Fragment")
                                    .commit();

                            Fragment tmp = getActivity().getSupportFragmentManager()
                                    .findFragmentByTag("NowPlayingFragment");

                            if(tmp != null) {
                                getActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .remove(tmp)
                                        .add(R.id.frameLayout, tmp, "NowPlayingFragment")

                                        .commit();

                                nowPlayingViewModel.nowPlayingShowed.postValue(true);
                            }
                        }));

            songViewModel.getSongs().observe(getActivity(), new Observer<ArrayList<Song>>() {
                @Override
                public void onChanged(ArrayList<Song> songs) {
                    recyclerView.setAdapter(new AlbumRecyclerViewAdapter(classify(songs),
                            (List<Song> songList, String listName) -> {
                                getParentFragment().getParentFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.frameLayout, InnerListContainer.newInstance(0, listName, songList), "Song Fragment")
                                        .addToBackStack("Song Fragment")
                                        .commit();

                                Fragment tmp = getActivity().getSupportFragmentManager()
                                        .findFragmentByTag("NowPlayingFragment");

                                if(tmp != null) {
                                    getActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .remove(tmp)
                                            .add(R.id.frameLayout, tmp, "NowPlayingFragment")

                                            .commit();

                                    nowPlayingViewModel.nowPlayingShowed.postValue(true);
                                }
                            }));
                }
            });
        }
        return view;
    }

    public List<List<Song>> classify(List<Song> songList) {
        HashMap<String, Integer> indexTable = new HashMap<>();
        List<List<Song>> rt = new ArrayList<>();
        int ptr = 0;
        for (Song song: songList) {
            int index = indexTable.getOrDefault(song.albumName, -1);
            if(index == -1) {
                index = ptr;
                indexTable.put(song.albumName, ptr++);
                rt.add(new ArrayList<>());
            }
            List<Song> tmp = rt.get(index);
            tmp.add(song);
            rt.set(index, tmp);
        }

        rt.sort(new Comparator<List<Song>>() {
            @Override
            public int compare(List<Song> o1, List<Song> o2) {
                return o1.get(0).albumName.toUpperCase().compareTo(o2.get(0).albumName.toUpperCase());
            }
        });

        return rt;
    }
}