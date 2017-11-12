/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */
package com.example.gziolle.popmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class TrailerAdapter extends RecyclerView.Adapter<TrailerAdapter.ViewHolder> {

    private static RecyclerViewClickListener mListener;
    private ArrayList<TrailerItem> mTrailerList;
    private Context mContext;


    TrailerAdapter(Context context, ArrayList<TrailerItem> mMovieTrailers,
                   RecyclerViewClickListener listener) {
        mContext = context;
        mTrailerList = mMovieTrailers;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.trailer_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mSummary.setText(mContext.getString(R.string.trailer_name) + " " + (position + 1));
    }

    @Override
    public int getItemCount() {
        return mTrailerList.size();
    }

    interface RecyclerViewClickListener {
        void recyclerViewListClicked(View v, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mIcon;
        TextView mSummary;

        ViewHolder(View itemView) {
            super(itemView);
            mIcon = (ImageView) itemView.findViewById(R.id.trailer_image);
            mSummary = (TextView) itemView.findViewById(R.id.trailer_summary);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.recyclerViewListClicked(view, this.getLayoutPosition());
        }
    }
}
