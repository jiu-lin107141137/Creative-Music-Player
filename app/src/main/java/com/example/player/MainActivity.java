package com.example.player;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.player.repository.SongRepository;
import com.example.player.util.Injector;
import com.example.player.util.Song;
import com.example.player.viewmodel.NowPlayingViewModel;
import com.example.player.viewmodel.SongViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    NowPlayingViewModel nowPlayingViewModel;
    SongViewModel songViewModel;
    SongRepository songRepository;
    FrameLayout frameLayout, bottomSheet;
    AllSongFragment songFragment;
    TabLayout tab;
    ViewPager2 pager;
    EditText searchTxt;
    Button searchBtn;
    String[] tabName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nowPlayingViewModel = new ViewModelProvider(this, new Injector().provideNowPlayingViewModel(getApplicationContext())).get(NowPlayingViewModel.class);
        songViewModel = new ViewModelProvider(this, new Injector().provideSongViewModel(getApplicationContext())).get(SongViewModel.class);
        songRepository = new Injector().provideSongRepository(getApplicationContext());

        requestPermissions();
        checkPlayList();

        searchTxt = findViewById(R.id.searchContent);
        searchBtn = findViewById(R.id.searchBtn);

        frameLayout = findViewById(R.id.frameLayout);
//        songFragment = new AllSongFragment();
//        String currentFragmentTag = "allSongsFragment";
//        getSupportFragmentManager().beginTransaction().replace(R.id.frameLayout, songFragment, currentFragmentTag).commit();

        tabName = new String[] {
                "Songs",
                "Artists",
                "Albums",
                "Lists"
        };
        tab = findViewById(R.id.tab);
        pager = findViewById(R.id.pager);
        MyPageAdapter pageAdapter = new MyPageAdapter(getSupportFragmentManager(), getLifecycle());
        pager.setAdapter(pageAdapter);
        pager.setId(R.id.pager);
        new TabLayoutMediator(tab, pager, ((tab1, position) -> {
            tab1.setText(tabName[position]);
        })).attach();

        Resources r = getResources();
        int PX_68 = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                68,
                r.getDisplayMetrics()
        );

        nowPlayingViewModel.nowPlayingShowed.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    pager.setPadding(0, 0, 0, PX_68);
                } else {
                    pager.setPadding(0, 0, 0, 0);
                }
            }
        });

        searchTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    searchTxt.setText("");
            }
        });

        searchTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
//                    Log.e("searchTxt", "Search");
//                    searchTxt.clearFocus();
                    searchBtn.callOnClick();
                    return true;
                }
                return false;
            }
        });

        searchTxt.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if(!insets.isVisible(WindowInsets.Type.ime())) {
//                    Log.e("searchTxt", "keyboard dismissed");
                    searchTxt.clearFocus();
                }
                return insets;
            }
        });

        searchBtn.setOnClickListener(v -> {
            String target = searchTxt.getText().toString();
            if(target == null || target.trim().length() == 0) {
                Toast.makeText(getApplicationContext(), "Fill in the search bar!", Toast.LENGTH_SHORT).show();
                return;
            }

            searchTxt.clearFocus();

            List<Song> result = getSearchList(target.trim().toUpperCase());

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frameLayout, InnerListContainer.newInstance(0, "Search", result), "Song Fragment")
                    .addToBackStack("Song Fragment")
                    .commit();


            Fragment tmp = getSupportFragmentManager()
                            .findFragmentByTag("NowPlayingFragment");

            if(tmp != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(tmp)
                        .add(R.id.frameLayout, tmp, "NowPlayingFragment")
                        .commit();

                nowPlayingViewModel.nowPlayingShowed.postValue(true);
            }
        });
    }

    private void checkPlayList() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        try {
            boolean first = sharedPref.getBoolean("first", true);
            if(first) {
                createPlayList();
                sharedPref.edit().putBoolean("first", false).apply();
            }
        } catch(Exception e) {
            Log.e("checkPlayList", "Error occurred!");
            e.printStackTrace();
        }
    }

    private void createPlayList() {
        songRepository.addPlayList("Favorite");
    }

    public List<Song> getSearchList(String token) {
        List<Song> currentSongList = songViewModel.getSongs().getValue();
        List<Song> rt = new ArrayList<>();

        for(int i = 0; i < currentSongList.size(); i++) {
            Song tmp = currentSongList.get(i);
            if(contains(tmp.title.toUpperCase(), token) || contains(tmp.artistName.toUpperCase(), token))
                rt.add(tmp);
        }

        return rt;
    }

    public boolean contains(String target, String needle) {
        return indexOf(target, needle) > -1;
    }

    public int indexOf(String target, String needle) {
        int si = 0, pi = 0;
        int[] next = getNext(needle);

        while(si < target.length() && pi < needle.length()) {
            if(target.charAt(si) == needle.charAt(pi)) {
                ++ si;
                ++ pi;
            }
            else if(pi <= 0) {
                ++ si;
            }
            else {
                pi = next[pi - 1];
            }
        }

        if(pi >= needle.length()) {
            return si - needle.length();
        }
        else {
            return -1;
        }
    }

    public int[] getNext(String target) {
        int[] next = new int[target.length()];
        next[0] = 0;
        int pi = 1, w = pi-1;

        while(pi < target.length()) {
            if(target.charAt(pi) == target.charAt(w)) {
                ++ pi;
                ++ w;
                next[pi - 1] = w;
            }
            else if(w <= 0) {
                pi ++;
                next[pi - 1] = 0;
            }
            else {
                w = next[w - 1];
            }
        }

        return next;
    }

//    public

    @Override
    public void onBackPressed() {
        if(nowPlayingViewModel.nowPlayingShowedExpanding.getValue()){
            nowPlayingViewModel.nowPlayingShowedExpanding.setValue(false);
        }
        else if(getSupportFragmentManager().getBackStackEntryCount() > 0){
            Fragment tmp = getSupportFragmentManager()
                    .findFragmentByTag("Song Fragment");
            if(tmp != null)
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(tmp)
                        .commit();
            else
                super.onBackPressed();
//            FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
//            if(entry.getName().equals("Song Fragment")){
//                getSupportFragmentManager().popBackStackImmediate();
//            }
        }
        else
            super.onBackPressed();
    }

    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + this.getPackageName()));

                startActivityForResult(intent, 123, null);
            }
        }

        if(Build.VERSION.SDK_INT >= 31){
            checkPermission(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                    android.Manifest.permission.FOREGROUND_SERVICE,
            }, 1);
        }
        else if (Build.VERSION.SDK_INT == 30){
            checkPermission(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                    android.Manifest.permission.FOREGROUND_SERVICE,
            },1);
        }
        else{
            checkPermission(new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
            }, 1);
        }
    }

    public void checkPermission(String permissions[], int requestCode) {
        // Checking if permission is not granted
        ArrayList<String> arrayList = new ArrayList <String> ();
        for (String p: permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, p) == PackageManager.PERMISSION_DENIED)
                arrayList.add(p);
        }
        if (arrayList.size() == 0) {
//            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            permissions = new String[arrayList.size()];
            for (int i = 0; i < arrayList.size(); i++) {
                permissions[i] = arrayList.get(i);
                Log.e("Permissions", permissions[i]);
            }
            ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
        }
    }

    public class MyPageAdapter extends FragmentStateAdapter {
        public MyPageAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if(position == 0)
                return new AllSongFragment();
            else if(position == 1)
                return new ArtistFragmentContainer();
            else if(position == 2)
                return new AlbumFragmentContainer();
            else if(position == 3)
                return new PlayListFragmentContainer();
            return null;
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}