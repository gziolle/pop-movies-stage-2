/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gziolle.popmovies.util.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Creates the list of movies displayed to the user.
 * Handles item selection as well.
 */

public class MovieListFragment extends Fragment implements MovieAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<ArrayList<MovieItem>> {

    public static final String LOG_TAG = MovieListFragment.class.getSimpleName();

    private static final int MOVIES_LOADER_ID = 101;

    public static String TMDB_ID = "movie_id";
    public static String TMDB_TITLE = "title";
    public static String TMDB_ORIGINAL_TITLE = "original_title";
    public static String TMDB_POSTER_PATH = "poster_path";
    public static String TMDB_OVERVIEW = "overview";
    public static String TMDB_VOTE_AVERAGE = "avg";
    public static String TMDB_RELEASE_DATE = "release_date";

    String MOVIE_LIST_EXTRA = "movielist";

    public RecyclerView mRecyclerView;
    public GridLayoutManager mGridLayoutManager;
    public MovieAdapter mMovieAdapter;
    public ArrayList<MovieItem> mMovieItems;
    public ProgressDialog mProgressDialog;

    private int mCurrentPage = 1;
    private String mLastQueryMode = "";
    private boolean mIsFetching = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        int spanCount = calculateSpanCount(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_movie_list, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.rv_movie_list);
        mGridLayoutManager = new GridLayoutManager(getActivity(), spanCount);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mGridLayoutManager);

        if (savedInstanceState != null) {
            mMovieItems = savedInstanceState.getParcelableArrayList(MOVIE_LIST_EXTRA);
        } else {
            mMovieItems = new ArrayList<>();
        }

        mMovieAdapter = new MovieAdapter(getActivity(), this, mMovieItems);
        mRecyclerView.setAdapter(mMovieAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = mGridLayoutManager.getChildCount();
                    int totalItemCount = mGridLayoutManager.getItemCount();
                    int pastVisibleItems = mGridLayoutManager.findFirstVisibleItemPosition();

                    if (((pastVisibleItems + visibleItemCount) == totalItemCount) && !mIsFetching) {
                        updateMovieList();
                    }
                }
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Loader loader = getActivity().getSupportLoaderManager().getLoader(MOVIES_LOADER_ID);
        if (loader == null) {
            Log.i(LOG_TAG, "loader == null");
            mLastQueryMode = PreferenceManager
                    .getDefaultSharedPreferences(getContext())
                    .getString(getString(R.string.query_mode_key), getString(R.string.query_mode_default));
            getActivity().getSupportLoaderManager().initLoader(MOVIES_LOADER_ID, null, this);
        } else {
            updateMovieList();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(MOVIE_LIST_EXTRA, mMovieItems);
        super.onSaveInstanceState(outState);
    }

    /**
     * Updates the movies list based on its current page.
     * It also stores the user's preference for future usage.
     */
    public void updateMovieList() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String queryMode = prefs.getString(getString(R.string.query_mode_key), getString(R.string.query_mode_default));

        if (queryMode.equals(getActivity().getString(R.string.query_mode_favorites))) {
            mMovieItems.clear();
            mCurrentPage = 1;
            mLastQueryMode = getActivity().getString(R.string.query_mode_favorites);
        } else {
            if (NetworkUtils.isConnected(getActivity())) {
                if (mLastQueryMode.equals("")) {
                    mLastQueryMode = queryMode;
                    mCurrentPage = 1;
                } else if (!mLastQueryMode.equals(queryMode)) {
                    mMovieItems.clear();
                    mCurrentPage = 1;
                    mLastQueryMode = queryMode;
                } else {
                    mCurrentPage++;
                }
            } else {
                Toast.makeText(getActivity(), getActivity().getString(R.string.offline_status), Toast.LENGTH_SHORT).show();
            }
        }
        getActivity().getSupportLoaderManager().restartLoader(MOVIES_LOADER_ID, null, this);
    }

    /**
     * Callback method to handle item selection
     */
    @Override
    public void onListItemClicked(int position) {
        MovieItem item = mMovieAdapter.getItem(position);

        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra(TMDB_ID, item.getId());
        intent.putExtra(TMDB_TITLE, item.getTitle());
        intent.putExtra(TMDB_ORIGINAL_TITLE, item.getOriginalTitle());
        intent.putExtra(TMDB_POSTER_PATH, item.getPosterPath());
        intent.putExtra(TMDB_RELEASE_DATE, item.getReleaseDate());
        intent.putExtra(TMDB_OVERVIEW, item.getOverview());
        intent.putExtra(TMDB_VOTE_AVERAGE, item.getAverage());

        startActivity(intent);
    }

    /*
    * Determines how many columns will be displayed in the RecycleView.
    * */
    public static int calculateSpanCount(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float dpWidth = metrics.widthPixels / metrics.density;
        return (int) (dpWidth / 180);
    }

    /*
    * Displays the progress dialog to inform the user that movies are being loaded.
    * */
    private void showProgressDialog(){
        if(mProgressDialog == null){
            mProgressDialog = new ProgressDialog(getActivity());
        }
        mProgressDialog.setMessage(getString(R.string.progress_dialog_loading));
        mProgressDialog.show();
    }

    /*
    * Displays a message when there are no movies to display.
    * */
    private void displayErrorMessages(){
        TextView textView = (TextView) getActivity().findViewById(R.id.tv_error_message);
        textView.setVisibility(View.VISIBLE);
        textView.setText(getString(R.string.no_movies_found));
    }

    /*Loader-related methods*/
    @Override
    public Loader<ArrayList<MovieItem>> onCreateLoader(int id, Bundle args) {
        Log.i(LOG_TAG, "Loader.onCreateLoader()");
        return new AsyncTaskLoader<ArrayList<MovieItem>>(getActivity()) {
            @Override
            protected void onStartLoading() {
                showProgressDialog();
                forceLoad();
            }

            @Override
            public ArrayList<MovieItem> loadInBackground() {
                Log.i(LOG_TAG, "loadInBackground");
                mIsFetching = true;
                Log.i(LOG_TAG, "queryMode = " + mLastQueryMode);
                Log.i(LOG_TAG, "currentPage = " + mCurrentPage);

                if (!getString(R.string.query_mode_favorites).equals(mLastQueryMode)) {
                    Log.i(LOG_TAG, "return NetworkUtils.fetchMoviesFromNetwork");
                    return NetworkUtils.fetchMoviesFromNetwork(mLastQueryMode, mCurrentPage);
                } else {
                    Log.i(LOG_TAG, "NetworkUtils.fetchMoviesFromDatabase");
                    return NetworkUtils.fetchMoviesFromDatabase(getActivity());
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<MovieItem>> loader, ArrayList<MovieItem> data) {
        mIsFetching = false;

        if(mProgressDialog != null){
            mProgressDialog.hide();
        }

        if (data != null) {
            TextView textView = (TextView) getActivity().findViewById(R.id.tv_error_message);
            if(data.size() != 0) {
                textView.setVisibility(View.GONE);
                mMovieItems.addAll(data);
                mMovieAdapter.notifyDataSetChanged();
            } else{
                mMovieItems.clear();
                mMovieAdapter.notifyDataSetChanged();
                displayErrorMessages();
            }
        } else{
            mMovieItems.clear();
            mMovieAdapter.notifyDataSetChanged();
            displayErrorMessages();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<MovieItem>> loader) {}
}
