package com.example.chat;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    ArrayList<ItemContract> dataList;
    Context context;
    int resource;

    public ListAdapter(ArrayList<ItemContract> dataList, Context context, int resource) {
        this.dataList = dataList;
        this.context = context;

        this.resource = resource;
    }

    @Override
    public int getItemCount() {
        Log.d("kkang", "ListAdapter / getItemCount() / " + dataList.size());
        Log.d("kkang", " ");

        return dataList.size();
    }

    @NonNull
    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(resource, parent, false);

        Log.d("kkang", "ListAdapter / onCreateViewHolder()");
        Log.d("kkang", " ");

        return new ViewHolder(view);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contentView;

        public ViewHolder(View view) {
            super(view);

            contentView = view.findViewById(R.id.list_content);

            Log.d("kkang", "ListAdapter ) ViewHolder");
            Log.d("kkang", " ");
        }

        public TextView getContentView() {
            return contentView;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapter.ViewHolder holder, int position) {
        Log.d("kkang", "ListAdapter / onBindViewHolder() / int position: " + position);
        Log.d("kkang", " ");

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) holder.getContentView().getLayoutParams();

        TextView contentView = holder.getContentView();
        ItemContract mContract = dataList.get(position);

        if (mContract.getUsername().equals("me")) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

            contentView.setTextColor(Color.WHITE);
            contentView.setBackgroundResource(R.drawable.chat_right);

        } else if (mContract.getUsername().equals("you")) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

            contentView.setBackgroundResource(R.drawable.chat_left);
        }
        Log.d("kkang", "ListAdapter / onBindViewHolder() / mContract.getContent(): " + mContract.getContent());
        Log.d("kkang", " ");

        contentView.setText(mContract.getContent());
    }
}
