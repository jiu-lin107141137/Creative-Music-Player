package com.example.player;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.player.repository.SongRepository;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PlayListFragmentContainer extends Fragment {
    private SongViewModel songViewModel;
    private NowPlayingViewModel nowPlayingViewModel;
    private SongRepository songRepository;
    private List<Song> favoriteList;

    public PlayListFragmentContainer() {

    }

    public static PlayListFragmentContainer newInstance() {
        PlayListFragmentContainer fragmentContainer = new PlayListFragmentContainer();
        return fragmentContainer;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_fragment_container, container, false);

        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);
        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
        songRepository = new Injector().provideSongRepository(requireContext());

        Map<String, List<Song>> lists = songViewModel.getPlayLists().getValue();

        LinearLayout favoriteBar = view.findViewById(R.id.favoritePlayList);
        TextView size = view.findViewById(R.id.favoritePlayListSize);
        TextView addPlayListBtn = view.findViewById(R.id.addPlayListBtn);
        if(lists != null) {
            favoriteList = lists.get("Favorite");
            if(favoriteList != null)
                size.setText(Long.toString(favoriteList.size()));
            else
                favoriteList = new ArrayList<>();
        }

        addPlayListBtn.setOnClickListener(v -> {
            addPlayList();
        });

        favoriteBar.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, InnerListContainer.newInstance(0, "Favorite", favoriteList), "Song Fragment")
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
        });

        songViewModel.getPlayLists().observe(getViewLifecycleOwner(), new Observer<HashMap<String, List<Song>>>() {
            @Override
            public void onChanged(HashMap<String, List<Song>> stringListHashMap) {
                List<Song> tmp = stringListHashMap.get("Favorite");
                if(tmp != null)
                    favoriteList = tmp;
                else
                    favoriteList = new ArrayList<>();

                size.setText(Long.toString(favoriteList.size()));
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.playlistFragmentContainer, new PlayListFragment())
                .commit();
    }

    private void addPlayList() {
        EditText input = new EditText(requireContext());
        input.setHint("Playlist Name");

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Playlist")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String name = input.getText().toString();
                    if(name != null && name.trim().length() > 0) {
                        songRepository.addPlayList(name.trim());
                        Toast.makeText(requireContext(), "Create finished!", Toast.LENGTH_SHORT).show();
                        try {
                            songViewModel.loadPlayLists();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        Toast.makeText(requireContext(), "Fill in the playlist name!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .show();
    }
}
