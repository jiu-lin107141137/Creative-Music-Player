package com.example.player;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.player.custominterface.PlayMedia;
import com.example.player.custominterface.ShowView;
import com.example.player.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.player.databinding.FragmentArtistBinding;
import com.example.player.util.Song;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link List<List<Song>>}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ArtistRecyclerViewAdapter extends RecyclerView.Adapter<ArtistRecyclerViewAdapter.ViewHolder> {

    private final List<List<Song>> mValues;
    private ShowView showView;

    public ArtistRecyclerViewAdapter(List<List<Song>> items, ShowView showView) {
        mValues = items;
        this.showView = showView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentArtistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).get(0).artistName);
        holder.mContentView.setText(Integer.toString(mValues.get(position).size()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView.showView(holder.mItem, holder.mItem.get(0).artistName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public List<Song> mItem;

        public ViewHolder(FragmentArtistBinding binding) {
            super(binding.getRoot());
            mIdView = binding.atristName;
            mContentView = binding.count;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mIdView.getText() + "'";
        }
    }
}