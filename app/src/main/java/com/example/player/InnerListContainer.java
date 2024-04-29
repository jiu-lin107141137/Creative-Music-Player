package com.example.player;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;

import java.util.ArrayList;
import java.util.List;

public class InnerListContainer extends Fragment {
    private static String ARG_TYPE = "container type";
    private static String ARG_SONG_LIST = "song list";
    private static String ARG_LIST_NAME = "list name";
    private List<Song> songList;
    private int containerType;
    private String listName;
    private NowPlayingViewModel nowPlayingViewModel;
//    private String[] headingTxt = new String[] {
//            "Artist",
//            "Album"
//    };

    public InnerListContainer() {
    }

    public static InnerListContainer newInstance(int containerType, String listName, List<Song>songList) {
        InnerListContainer ilc = new InnerListContainer();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, containerType);
        args.putSerializable(ARG_SONG_LIST, (ArrayList<Song>)songList);
        args.putString(ARG_LIST_NAME, listName);
        ilc.setArguments(args);
        return ilc;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            songList = (List<Song>) getArguments().getSerializable(ARG_SONG_LIST);
            listName = getArguments().getString(ARG_LIST_NAME);
            containerType = getArguments().getInt(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.inner_list_container, container, false);

        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);

        ImageButton finishBtn = view.findViewById(R.id.finishBtn);
        TextView heading = view.findViewById(R.id.heading);
        FrameLayout frame = view.findViewById(R.id.frame);

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment tmp = getActivity().getSupportFragmentManager()
                                            .findFragmentByTag("Song Fragment");
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .remove(tmp)
                        .commit();
//                getActivity().getSupportFragmentManager()
//                                .popBackStack();
            }
        });

        heading.setText(listName);
        getChildFragmentManager().beginTransaction()
                                .replace(R.id.frame, SongFragment.newInstance(songList), "Song Fragment")
                                .commit();

        Resources r = getResources();
        int PX_68 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                68,
                r.getDisplayMetrics()
        );
//        if(nowPlayingViewModel.nowPlayingShowed.getValue() != null && nowPlayingViewModel.nowPlayingShowed.getValue()) {
//            ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(frame.getLayoutParams());
//            p.setMargins(0, 0, 0, PX_68);
//            frame.setLayoutParams(p);
//        }

        ConstraintLayout innerListContainer = view.findViewById(R.id.innerListContainer);

        boolean flag = nowPlayingViewModel.nowPlayingShowed.getValue() != null && nowPlayingViewModel.nowPlayingShowed.getValue();
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(innerListContainer.getLayoutParams());
        if (flag) {
            p.setMargins(0, 0, 0, PX_68);
        } else {
            p.setMargins(0, 0, 0, 0);
        }
        innerListContainer.setLayoutParams(p);

        nowPlayingViewModel.nowPlayingShowed.observe(getActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(innerListContainer.getLayoutParams());
                if (aBoolean) {
                    p.setMargins(0, 0, 0, PX_68);
                } else {
                    p.setMargins(0, 0, 0, 0);
                }
                innerListContainer.setLayoutParams(p);
//                Log.e("tmp", "tmp");
            }
        });

        return view;
    }
}
