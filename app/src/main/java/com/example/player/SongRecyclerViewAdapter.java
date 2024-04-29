package com.example.player;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.player.custominterface.PlayMedia;
import com.example.player.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.player.databinding.FragmentSongBinding;
import com.example.player.repository.SongRepository;
import com.example.player.util.Song;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link Song}.
 * TODO: Replace the implementation with code for your data type.
 */
public class SongRecyclerViewAdapter extends RecyclerView.Adapter<SongRecyclerViewAdapter.ViewHolder> {

    private final List<Song> mValues;
    private PlayMedia playMedia;

    public SongRecyclerViewAdapter(List<Song> items, PlayMedia playMedia) {
        mValues = items;
        this.playMedia = playMedia;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentSongBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        long d = mValues.get(position).duration / 1000;

        holder.mSongNameView.setText(mValues.get(position).title);
        holder.mSongDurationView.setText(d / 60 / 60 > 0 ?
                String.format("%d:%2d:%02d", d / 60 / 60, d / 60 % 60, d % 60) :
                String.format("%2d:%02d", d / 60 % 60, d % 60));
        holder.mSongArtistView.setText(mValues.get(position).artistName);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SongRepository.setSongList(mValues);
                playMedia.playMedia(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mSongNameView;
        public final TextView mSongDurationView;
        public final TextView mSongArtistView;
        public Song mItem;

        public ViewHolder(FragmentSongBinding binding) {
            super(binding.getRoot());
            mSongNameView = binding.songNameTxt;
            mSongDurationView = binding.songDurationTxt;
            mSongArtistView = binding.songArtistTxt;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mItem.id + "'";
        }
    }
}