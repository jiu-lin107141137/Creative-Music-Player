package com.example.player;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.player.custominterface.ShowDialog;
import com.example.player.custominterface.ShowView;
import com.example.player.placeholder.PlaceholderContent.PlaceholderItem;
import com.example.player.databinding.FragmentPlaylistBinding;
import com.example.player.util.Song;

import java.util.List;
import java.util.Map;

/**
 * {@link RecyclerView.Adapter} that can display a {@link List<Map.Entry<String,List<Song>>>}.
 * TODO: Replace the implementation with code for your data type.
 */
public class PlayListRecyclerViewAdapter extends RecyclerView.Adapter<PlayListRecyclerViewAdapter.ViewHolder> {

    private final List<Map.Entry<String, List<Song>>> mValues;
    private ShowView showView;
    private ShowDialog showDialog;

    public PlayListRecyclerViewAdapter(List<Map.Entry<String, List<Song>>> items, ShowView showView, ShowDialog showDialog) {
        mValues = items;
        this.showView = showView;
        this.showDialog = showDialog;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentPlaylistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(holder.mItem.getKey());
        holder.mContentView.setText(Integer.toString(holder.mItem.getValue().size()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showView.showView(holder.mItem.getValue(), holder.mItem.getKey());
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDialog.showChoices(holder.mItem.getKey());
                return true;
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
        public Map.Entry<String, List<Song>> mItem;

        public ViewHolder(FragmentPlaylistBinding binding) {
            super(binding.getRoot());
            mIdView = binding.playListName;
            mContentView = binding.count3;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}