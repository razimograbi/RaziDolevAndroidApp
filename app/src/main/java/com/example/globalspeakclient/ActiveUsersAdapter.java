package com.example.globalspeakclient;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.globalspeakclient.ActiveUser;

import java.util.List;

public class ActiveUsersAdapter extends RecyclerView.Adapter<ActiveUsersAdapter.ViewHolder> {
    private List<ActiveUser> userList;
    private OnUserClickListener onUserClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION; // Keeps track of selected user

    public ActiveUsersAdapter(List<ActiveUser> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.onUserClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        }

        public void bind(ActiveUser user, boolean isSelected) {
            tvUserName.setText(user.getProfileName());
            tvUserEmail.setText(user.getEmail());
            itemView.setBackgroundColor(isSelected ? 0xFFDDDDDD : 0xFFFFFFFF); // Highlight selection
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActiveUser user = userList.get(position);
        boolean isSelected = (position == selectedPosition);
        holder.bind(user, isSelected);

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;

            if (selectedPosition == holder.getAdapterPosition()) {
                //  If clicking the same user again, deselect them
                selectedPosition = -1;
                Log.d("ActiveUsersAdapter", "User deselected.");
            } else {
                //  Otherwise, select the new user
                selectedPosition = holder.getAdapterPosition();
                Log.d("ActiveUsersAdapter", "User selected: " + user.getProfileName());
            }

            // Ensure UI refreshes correctly
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Pass null if deselected, otherwise pass selected user
            onUserClickListener.onUserSelected(selectedPosition == -1 ? null : user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public interface OnUserClickListener {
        void onUserSelected(ActiveUser user);
    }

    public void updateUserList(List<ActiveUser> newList) {
        userList = newList;
        notifyDataSetChanged();
    }
}
