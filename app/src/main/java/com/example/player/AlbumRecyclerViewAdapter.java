package com.example.player;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.player.custominterface.ShowView;
import com.example.player.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.player.databinding.FragmentAlbumBinding;
import com.example.player.util.Song;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link List<List< Song >>}.
 * TODO: Replace the implementation with code for your data type.
 */
public class AlbumRecyclerViewAdapter extends RecyclerView.Adapter<AlbumRecyclerViewAdapter.ViewHolder> {

    private final List<List<Song>> mValues;
    private ShowView showView;

    public AlbumRecyclerViewAdapter(List<List<Song>> items, ShowView showView) {
        mValues = items;
        this.showView = showView;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentAlbumBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).get(0).albumName);
        holder.mContentView.setText(Integer.toString(mValues.get(position).size()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView.showView(holder.mItem, holder.mItem.get(0).albumName);
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

        public ViewHolder(FragmentAlbumBinding binding) {
            super(binding.getRoot());
            mIdView = binding.albumName;
            mContentView = binding.count2;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}