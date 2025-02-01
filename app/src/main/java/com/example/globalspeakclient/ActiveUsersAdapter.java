package com.example.globalspeakclient;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.globalspeakclient.ActiveUser;

import java.util.List;


/**
 * Adapter for displaying a list of active users in a RecyclerView.
 * Handles user selection and long-press actions for removing friends.
 */
public class ActiveUsersAdapter extends RecyclerView.Adapter<ActiveUsersAdapter.ViewHolder> {
    private List<ActiveUser> userList;
    private OnUserClickListener onUserClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION; // Keeps track of selected user
    private OnUserLongPressListener onUserLongPressListener;


    /**
     * Constructs an adapter for active users.
     * @param userList List of active users.
     * @param listener Click listener for user selection.
     * @param longPressListener Listener for long-press actions (removing friends).
     */
    public ActiveUsersAdapter(List<ActiveUser> userList, OnUserClickListener listener, OnUserLongPressListener longPressListener) {
        this.userList = userList;
        this.onUserClickListener = listener;
        this.onUserLongPressListener = longPressListener;

    }



    /**
     * ViewHolder for displaying user details in a RecyclerView item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        }


        /**
         * Binds user details to the UI components.
         * @param user ActiveUser instance.
         * @param isSelected Whether the user is currently selected.
         */
        public void bind(ActiveUser user, boolean isSelected) {
            if (user.getEmail().isEmpty()) {
                // This is a placeholder message, not a real user
                tvUserName.setText(user.getProfileName()); // Show message
                tvUserEmail.setVisibility(View.GONE); // Hide email
                itemView.setOnClickListener(null); //Disable selection
                itemView.setOnLongClickListener(null); //  Disable long press
                itemView.setBackgroundColor(0xFFFFFFFF); // No highlight
            } else {
                tvUserName.setText(user.getProfileName());
                tvUserEmail.setText(user.getEmail());
                itemView.setBackgroundColor(isSelected ? 0xFFDDDDDD : 0xFFFFFFFF); // Highlight selection
            }
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
        if (!user.getEmail().isEmpty()) { //  Allow selection only for real user
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
            // Long press to remove friend
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Remove Friend")
                        .setMessage("Are you sure you want to remove " + user.getProfileName() + "?")
                        .setPositiveButton("Yes, Remove", (dialog, which) -> {
                            if (onUserLongPressListener != null) {
                                onUserLongPressListener.onUserLongPress(user);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        } else { //  Disable selection and long press for placeholder messages
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Interfaces to handle click and long press events
    public interface OnUserClickListener {
        void onUserSelected(ActiveUser user);
    }

    public interface OnUserLongPressListener {
        void onUserLongPress(ActiveUser user);
    }


    /**
     * Updates the list of active users in the RecyclerView.
     * @param newList The new list of users.
     */
    public void updateUserList(List<ActiveUser> newList) {
        userList = newList;
        notifyDataSetChanged();
    }
}
