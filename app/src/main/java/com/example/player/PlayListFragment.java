package com.example.player;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.Toast;

import com.example.player.custominterface.ShowDialog;
import com.example.player.placeholder.PlaceholderContent;
import com.example.player.repository.SongRepository;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * A fragment representing a list of Items.
 */
public class PlayListFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private SongViewModel songViewModel;
    private NowPlayingViewModel nowPlayingViewModel;
    private SongRepository songRepository;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlayListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static PlayListFragment newInstance(int columnCount) {
        PlayListFragment fragment = new PlayListFragment();
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
        View view = inflater.inflate(R.layout.fragment_playlist_list, container, false);

        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);
        songRepository = new Injector().provideSongRepository(requireContext());

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            ShowDialog showDialog = new ShowDialog() {
                @Override
                public void showChoices(String oldName) {
                    showChooser(oldName);
                }
            };


            Map<String, List<Song>> pl = songViewModel.getPlayLists().getValue();
            if(pl != null)
                recyclerView.setAdapter(new PlayListRecyclerViewAdapter(classify(pl),
                    (List<Song> songList, String listName) -> {
                        getParentFragment().getParentFragmentManager()
                                .beginTransaction()
                                .add(R.id.frameLayout, InnerListContainer.newInstance(0, listName, songList), "Song Fragment")
                                .addToBackStack("Song Fragment")
                                .commit();


                        Fragment tmp = getActivity().getSupportFragmentManager()
                                .findFragmentByTag("NowPlayingFragment");

                        if (tmp != null) {
                            getActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .remove(tmp)
                                    .add(R.id.frameLayout, tmp, "NowPlayingFragment")

                                    .commit();

                            nowPlayingViewModel.nowPlayingShowed.postValue(true);
                        }
                    }, showDialog));

            songViewModel.getPlayLists().observe(getActivity(), new Observer<HashMap<String, List<Song>>>() {
                @Override
                public void onChanged(HashMap<String, List<Song>> stringListHashMap) {
                    recyclerView.setAdapter(new PlayListRecyclerViewAdapter(classify(stringListHashMap),
                            (List<Song> songList, String listName) -> {
                                getParentFragment().getParentFragmentManager()
                                        .beginTransaction()
                                        .add(R.id.frameLayout, InnerListContainer.newInstance(0, listName, songList), "Song Fragment")
                                        .addToBackStack("Song Fragment")
                                        .commit();


                                Fragment tmp = getActivity().getSupportFragmentManager()
                                        .findFragmentByTag("NowPlayingFragment");

                                if (tmp != null) {
                                    getActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .remove(tmp)
                                            .add(R.id.frameLayout, tmp, "NowPlayingFragment")

                                            .commit();

                                    nowPlayingViewModel.nowPlayingShowed.postValue(true);
                                }
                            }, showDialog));
                }
            });
//            recyclerView.setAdapter(new PlayListRecyclerViewAdapter(PlaceholderContent.ITEMS));
        }
        return view;
    }

    private List<Map.Entry<String, List<Song>>> classify(Map<String, List<Song>> lists) {
        return lists.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("Favorite"))
                .sorted(Map.Entry.comparingByKey(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.toUpperCase().compareTo(o2.toUpperCase());
                    }
                }))
                .collect(Collectors.toList());
    }

    public boolean deletePlayList(String name) {
        return songRepository.deletePlayList(name);
    }

    public boolean renamePlayList(String oldName, String newName) {
        return songRepository.renamePlayList(oldName, newName);
    }

    public void showChooser(String oldName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Actions")
                .setMessage("Choose an action to perform.")
                .setPositiveButton("Rename", (dialog, which) -> {
                    EditText input = new EditText(requireContext());
                    input.setHint("New name");
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Rename")
                            .setView(input)
                            .setPositiveButton("Confirm", (dialog1, which1) -> {
                                String newName = input.getText().toString();
                                if(newName == null || newName.trim().length() == 0) {
                                    Toast.makeText(requireContext(), "Fill in the name section!", Toast.LENGTH_SHORT).show();
                                }
                                else if(newName.equals(oldName)) {
                                    Toast.makeText(requireContext(), "The new name cannot equal to the old name!", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    boolean res = renamePlayList(oldName, newName.trim());
                                    if(res) {
                                        Toast.makeText(requireContext(), "Rename successfully", Toast.LENGTH_SHORT).show();
                                        try {
                                            songViewModel.loadPlayLists();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    else
                                        Toast.makeText(requireContext(), "Rename failed", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", (dialog1, which1) -> {
                                dialog1.cancel();
                            })
                            .show();
                    dialog.cancel();
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete")
                            .setMessage("Sure to delete this playlist, this action cannot be recovered.")
                            .setPositiveButton("Confirm", (dialog1, which1) -> {
                                boolean res = deletePlayList(oldName);
                                if(res) {
                                    Toast.makeText(requireContext(), "Delete successfully", Toast.LENGTH_SHORT).show();
                                    try {
                                        songViewModel.loadPlayLists();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else
                                    Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", (dialog1, which1) -> {
                                dialog1.cancel();
                            })
                            .show();
                    dialog.cancel();
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    dialog.cancel();
                })
                .show();
    }
}