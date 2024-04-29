package com.example.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.player.repository.SongRepository;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.transform.Result;

public class NowPlayingFragment extends Fragment {
    private BottomSheetBehavior<ConstraintLayout> bottomSheetBehavior;
    private NowPlayingViewModel nowPlayingViewModel;
    private SongViewModel songViewModel;
    private SongRepository songRepository;

    private ConstraintLayout smallLayout, mainLayout;
    private ImageView smallCover, mainCover;
    private SeekBar smallSeekBar, mainSeekBar;
    private TextView smallTitle, smallSubTitle, mainSongName, mainCurrentProgress, mainDuration;
    private ImageButton smallPauseBtn, mainLeaveBtn, mainPauseBtn,
            mainBackwardBtn, mainForwardBtn, mainPlayModeBtn, mainModifyBtn,
            mainDeleteBtn, mainEditPlayListBtn, mainAddFavoriteBtn, mainShareBtn;

    private long idTmp;
    private long durationInSec;
    private boolean isSeeking;
    private int playMode;
    private String[] playModeHint;
    private int[] playModeImage;

    private ActivityResultLauncher<Intent> infoModifierLauncher, playlistModifierLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.now_playing_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        idTmp = -1;
        durationInSec = 0;
        isSeeking = false;
        playMode = 0;
        playModeHint = new String[] {
                "Shuffle all songs",
                "Repeat one",
                "Repeat all"
        };
        playModeImage = new int[] {
                R.drawable.ic_shuffle,
                R.drawable.ic_repeat_one,
                R.drawable.ic_repeat
        };

        nowPlayingViewModel = new ViewModelProvider(getActivity(), new Injector().provideNowPlayingViewModel(requireContext())).get(NowPlayingViewModel.class);
        songViewModel = new ViewModelProvider(getActivity(), new Injector().provideSongViewModel(requireContext())).get(SongViewModel.class);
        songRepository = new Injector().provideSongRepository(requireContext());

        infoModifierLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Bundle bundle = data.getExtras();
                    boolean refresh = bundle.getBoolean("refresh", false);
                    if(refresh){
                        nowPlayingViewModel.playStop();
                        try {
                            songViewModel.loadSongs();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        playlistModifierLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Bundle bundle = data.getExtras();
                    boolean refresh = bundle.getBoolean("refresh", false);
                    if(refresh){
                        ArrayList<String> names = bundle.getStringArrayList("names");
                        long idToUpdate = bundle.getLong("id", -1);
                        if(names != null && idToUpdate != -1) {
                            ExecutorService executorService = Executors.newSingleThreadExecutor();
                            Future future = executorService.submit(new Callable() {
                                @Override
                                public Boolean call() {
                                    songRepository.updatePlayList(idToUpdate, songViewModel.getPlayLists().getValue(), names);
                                    return true;
                                }
                            });
                            executorService.shutdown();
                            try {
                                boolean f = (boolean) future.get();
                                songViewModel.loadSongs();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });

        smallLayout = view.findViewById(R.id.smallLayout);
        mainLayout = view.findViewById(R.id.mainLayout);

        smallCover = view.findViewById(R.id.smallCover);
        smallSeekBar = view.findViewById(R.id.smallSeekBar);
        smallTitle = view.findViewById(R.id.smallTitle);
        smallSubTitle = view.findViewById(R.id.smallSubTitle);
        smallPauseBtn = view.findViewById(R.id.smallPauseBtn);

        mainSongName = view.findViewById(R.id.songNameTxt);
        mainCover = view.findViewById(R.id.mainCover);
        mainLeaveBtn = view.findViewById(R.id.leaveBtn);
        mainSeekBar = view.findViewById(R.id.mainSeekBar);
        mainCurrentProgress = view.findViewById(R.id.songCurrentProgress);
        mainDuration = view.findViewById(R.id.songDuration);
        mainPauseBtn = view.findViewById(R.id.mainPauseBtn);
        mainBackwardBtn = view.findViewById(R.id.mainBackwardBtn);
        mainForwardBtn = view.findViewById(R.id.mainForwardBtn);
        mainPlayModeBtn = view.findViewById(R.id.mainPlayModeBtn);
        mainModifyBtn = view.findViewById(R.id.mainModifyBtn);
        mainDeleteBtn = view.findViewById(R.id.mainDeleteBtn);
        mainEditPlayListBtn = view.findViewById(R.id.mainEditPlayListBtn);
        mainAddFavoriteBtn = view.findViewById(R.id.mainAddFavoriteBtn);
        mainShareBtn = view.findViewById(R.id.mainShareBtn);
        mainLeaveBtn.setEnabled(false);

        smallLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        smallSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        smallPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nowPlayingViewModel.mediaMetadata.getValue() != null) {
                    songViewModel.playMediaId(nowPlayingViewModel.mediaMetadata.getValue().id);
                }
            }
        });

        mainPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nowPlayingViewModel.mediaMetadata.getValue() != null) {
                    songViewModel.playMediaId(nowPlayingViewModel.mediaMetadata.getValue().id);
                }
            }
        });

        mainBackwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nowPlayingViewModel.playBackward();
            }
        });

        mainForwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nowPlayingViewModel.playForward();
            }
        });

        mainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long position = seekBar.getProgress() * 10 * durationInSec;
                nowPlayingViewModel.seekTo(position);
                isSeeking = false;
            }
        });

        mainPlayModeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMode = (playMode+1) % 3;
                nowPlayingViewModel.setPlayMode(playMode);
                Toast.makeText(getContext(), playModeHint[playMode], Toast.LENGTH_SHORT).show();
                mainPlayModeBtn.setImageResource(playModeImage[playMode]);
            }
        });

        mainModifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(nowPlayingViewModel.mediaMetadata.getValue() != null) {
//                    songViewModel.playMediaId(nowPlayingViewModel.mediaMetadata.getValue().id, true);
//                }
//                nowPlayingViewModel.pl();
                NowPlayingViewModel.NowPlayingMetadata n = nowPlayingViewModel.mediaMetadata.getValue();
                Intent i = new Intent(getActivity(), MetadataActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", n.id);
                bundle.putString("title", n.title);
                bundle.putString("artist", n.subtitle);
                bundle.putString("album", n.albumName);
                bundle.putString("path", n.albumArtUri.toString());
                i.putExtras(bundle);
//                startActivity(i);
                infoModifierLauncher.launch(i);
            }
        });

        mainDeleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete")
                    .setMessage("Sure to delete this song?")
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        Uri u = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, nowPlayingViewModel.mediaMetadata.getValue().id);
                        getContext().getContentResolver().delete(u, null, null);
                        nowPlayingViewModel.playForward();
                        nowPlayingViewModel.playStop();
                        try {
                            songViewModel.loadSongs();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.cancel();
                    })
                    .setCancelable(false)
                    .show();
        });

        mainLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smallLayout.setVisibility(View.VISIBLE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        mainAddFavoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, List<Song>> listsRefTmp = songViewModel.getPlayLists().getValue();
                long id = idTmp;
                List<String> l = new ArrayList<>();
                l.add("Favorite");
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future future = executorService.submit(new Callable() {
                    @Override
                    public Boolean call() {
                        songRepository.updatePlayList(id, listsRefTmp, l);
                        return true;
                    }
                });
                executorService.shutdown();
                try {
                    boolean f = (boolean) future.get();
                    songViewModel.loadSongs();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mainEditPlayListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, List<Song>> listsRefTmp = songViewModel.getPlayLists().getValue();
                long id = idTmp;

                String[] names = new String[listsRefTmp.size()];
                boolean[] flags = new boolean[listsRefTmp.size()];
                int ptr = 0;
                for(Map.Entry<String, List<Song>> entry: listsRefTmp.entrySet()) {
                    names[ptr] = entry.getKey();
                    List<Song> tmp = entry.getValue();
                    for(int i = 0; i < tmp.size(); i++)
                        if(tmp.get(i).id == id) {
                            flags[ptr] =  true;
                            break;
                        }
                    ptr ++;
                }

                Bundle bundle = new Bundle();
                bundle.putStringArray("names", names);
                bundle.putBooleanArray("flags", flags);
                bundle.putLong("id", id);

                Intent intent = new Intent(getActivity(), PlayListEditingActivity.class);
                intent.putExtras(bundle);
                playlistModifierLauncher.launch(intent);
            }
        });

        mainShareBtn.setOnClickListener(v -> {
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
            long id = idTmp;
            String path = songRepository.getPathFromId(id);
            if(path == null) {
                Toast.makeText(getContext(), "Cannot find the path of the audio file selected!", Toast.LENGTH_SHORT).show();
                Log.e("ShareAudioFile", "Cannot find the path of the audio file selected! ID = "+id);
                return;
            }
            File target = new File(path);

            if(target.exists() && target.isFile()) {
                intentShareFile.setType("audio/*");
                Uri uri = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID, target.getAbsoluteFile());
                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intentShareFile, "Share file"));
            }
        });

        songViewModel.getFavorite().observe(getViewLifecycleOwner(), new Observer<ArrayList<Song>>() {
            @Override
            public void onChanged(ArrayList<Song> songs) {
                long id = idTmp;
                try {
                    Song tmp = songs.stream()
                            .filter(s -> s.id == id)
                            .findFirst()
                            .get();
                    if(tmp != null)
                        mainAddFavoriteBtn.setImageResource(R.drawable.ic_heart_colored);
                } catch (Exception e) {
                    mainAddFavoriteBtn.setImageResource(R.drawable.ic_heart);
                }
            }
        });

        nowPlayingViewModel.mediaMetadata.observe(getViewLifecycleOwner(), new Observer<NowPlayingViewModel.NowPlayingMetadata>() {
            @Override
            public void onChanged(NowPlayingViewModel.NowPlayingMetadata nowPlayingMetadata) {
                Log.e("OnChanged", nowPlayingMetadata.title+" / "+nowPlayingMetadata.subtitle);
                updateUi(view, nowPlayingMetadata);
            }
        });

        nowPlayingViewModel.mediaPosition.observe(getViewLifecycleOwner(), new Observer<Long>() {
            @Override
            public void onChanged(Long position) {
                updateProgressBar(position);
            }
        });

        nowPlayingViewModel.mediaBtnRef.observe(getViewLifecycleOwner(), new Observer<int[]>() {
            @Override
            public void onChanged(int[] integers) {
                smallPauseBtn.setImageResource(integers[0]);
                mainPauseBtn.setImageResource(integers[1]);
            }
        });


        setBottomSheetBehavior();

        nowPlayingViewModel.nowPlayingShowedExpanding.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(!aBoolean && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED){
//                    bottomSheetBehavior.setState(aBoolean ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
                    mainLeaveBtn.performClick();
                }
            }
        });

        nowPlayingViewModel.setPlayMode(0);
    }

    private void updateUi(View view, NowPlayingViewModel.NowPlayingMetadata metadata) {
        if(metadata.albumArtUri.equals(Uri.EMPTY)) {
            smallCover.setImageResource(R.drawable.ic_music_note_circle);
        }
        else {
            Glide.with(view)
                    .load(metadata.albumArtUri)
                    .into(smallCover);
            Glide.with(view)
                    .load(metadata.albumArtUri)
                    .into(mainCover);
        }

        smallTitle.setText(metadata.title);
        smallSubTitle.setText(metadata.subtitle);

        mainSongName.setText(metadata.title);

        durationInSec = (int)(metadata.duration / 1000.);
        mainDuration.setText(durationInSec / 60 / 60 > 0 ?
                            String.format("%d:%02d:%02d", durationInSec/60/60, durationInSec/60%60, durationInSec%60%60) :
                            String.format("%2d:%02d", durationInSec/60%60, durationInSec%60%60));

        if(Long.parseLong(metadata.id) != idTmp) {
            idTmp = Long.parseLong(metadata.id);
            updateFavoriteUi();
        }
    }

    private void updateFavoriteUi() {
        List<Song> songs = songViewModel.getFavorite().getValue();
        if(songs == null)
            return;
        long id = idTmp;
        try {
            Song tmp = songs.stream()
                    .filter(s -> s.id == id)
                    .findFirst()
                    .get();
            if(tmp != null)
                mainAddFavoriteBtn.setImageResource(R.drawable.ic_heart_colored);
        } catch (Exception e) {
            mainAddFavoriteBtn.setImageResource(R.drawable.ic_heart);
        }
    }

    private void updateProgressBar(long position) {
        if(durationInSec > 0) {
            int progress = (int)Math.round(position / 10. / durationInSec);
            smallSeekBar.setProgress(progress);
            if(!isSeeking)
                mainSeekBar.setProgress(progress);
            int currentTime = (int)(position / 1000.);
            mainCurrentProgress.setText(currentTime / 60 / 60 > 0 ?
                    String.format("%d:%02d:%02d", currentTime/60/60, currentTime/60%60, currentTime%60%60) :
                    String.format("%2d:%02d", currentTime/60%60, currentTime%60%60));
        }
        else {
            smallSeekBar.setProgress(0);
            if(!isSeeking)
                mainSeekBar.setProgress(0);
        }
    }

    private void setBottomSheetBehavior() {
        bottomSheetBehavior = BottomSheetBehavior.from(getView().findViewById(R.id.bottomSheetNowPlayingFragmentLayout));
        bottomSheetBehavior.setHideable(false);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        Log.d("NowPlayingFragment", "State collapsed");
                        smallLayout.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        Log.d("NowPlayingFragment", "State dragging");
                        smallLayout.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        Log.d("NowPlayingFragment", "State expanded");
                        smallLayout.setVisibility(View.GONE);
                        mainLeaveBtn.setEnabled(true);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        Log.d("NowPlayingFragment", "State hidden");
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        Log.d("NowPlayingFragment", "State half expanded");
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        Log.d("NowPlayingFragment", "State setting");
                        break;
                }
                if(newState == BottomSheetBehavior.STATE_EXPANDED){
                    nowPlayingViewModel.nowPlayingShowedExpanding.setValue(true);
                }
                else if(nowPlayingViewModel.nowPlayingShowedExpanding.getValue())
                    nowPlayingViewModel.nowPlayingShowedExpanding.setValue(false);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
//                Log.d("NowPlayingFragment", "slideOffset"+slideOffset);
                smallLayout.setAlpha(1 - slideOffset);
                mainLayout.setAlpha(slideOffset);
            }
        });
    }

    public static NowPlayingFragment newInstance() {
        return new NowPlayingFragment();
    }
}
