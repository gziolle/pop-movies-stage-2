/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gziolle.popmovies.data.FavoritesContract;
import com.example.gziolle.popmovies.util.FetchReviewsTask;
import com.example.gziolle.popmovies.util.FetchTrailersTask;
import com.example.gziolle.popmovies.util.NetworkUtils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Hosts the "Details" layout.
 * It is inflated by DetailActivity.
 */

public class DetailFragment extends Fragment implements TrailerAdapter.RecyclerViewClickListener,
        FetchReviewsTask.AsyncResponse, FetchTrailersTask.AsyncResponse {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String POSTER_PATH_BIG_AUTHORITY = "http://image.tmdb.org/t/p/w500";

    private Toolbar mToolbar;
    private TrailerAdapter mTrailerAdapter;
    private ArrayList<TrailerItem> mTrailers = new ArrayList<>();
    private ViewGroup mReviewLayout;
    private FloatingActionButton mFavoriteFAB;
    private boolean mIsFavorite;
    private Bundle mBundle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        mToolbar = (Toolbar) rootView.findViewById(R.id.toolbar);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(mToolbar);

        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mToolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        }

        ActionBar bar = activity.getSupportActionBar();

        if (bar != null) {
            bar.setDisplayShowTitleEnabled(false);
            bar.setDisplayHomeAsUpEnabled(true);

        }

        RecyclerView trailerRecyclerView = (RecyclerView) rootView.findViewById(R.id.trailer_list);
        RecyclerView.LayoutManager trailerLayoutManager =
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        trailerRecyclerView.setLayoutManager(trailerLayoutManager);
        mTrailerAdapter = new TrailerAdapter(getActivity(), mTrailers, this);
        trailerRecyclerView.setAdapter(mTrailerAdapter);
        trailerRecyclerView.setNestedScrollingEnabled(false);

        mReviewLayout = (LinearLayout) rootView.findViewById(R.id.review_list);

        mFavoriteFAB = (FloatingActionButton) rootView.findViewById(R.id.favorite_fab);
        mFavoriteFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsFavorite) {
                    //delete movie from the database
                    if (NetworkUtils.deleteMovieFromDB(mBundle, getActivity())) {
                        Toast.makeText(getActivity(),
                                getString(R.string.remove_from_favorites), Toast.LENGTH_SHORT).show();
                        mIsFavorite = false;
                        mFavoriteFAB.setBackgroundTintList(
                                ColorStateList.valueOf(
                                        ContextCompat.getColor(getActivity(), R.color.grey)));
                    } else {
                        Toast.makeText(getActivity(),
                                getString(R.string.delete_movie_error), Toast.LENGTH_SHORT).show();
                    }

                } else {
                    String posterUrl =
                            mBundle.getString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH);
                    if (posterUrl != null) {
                        new DownloadImageTask(getActivity()).execute(posterUrl);
                    }
                }

            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBundle = this.getArguments();
        if (mBundle != null) {
            bindView(mBundle);
        }
    }


    /*Binds the data from the a bundle to the elements in the layout*/
    public void bindView(Bundle bundle) {

        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mToolbar.setTitle(bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_TITLE));
        }

        ImageView moviePoster = (ImageView) getActivity().findViewById(R.id.movie_image);
        Bitmap bitmap = bundle.getParcelable("bitmap");

        moviePoster.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
        String moviePosterPath = bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH);
        Log.i(LOG_TAG, "moviePosterPath = " + moviePosterPath);
        if (moviePosterPath != null) {
            if (!moviePosterPath.startsWith("/data")) {
                moviePosterPath = POSTER_PATH_BIG_AUTHORITY + moviePosterPath;
                //Download the image using Picasso API
                Picasso.with(getActivity()).load(moviePosterPath)
                        .error(R.mipmap.ic_launcher).resize(500, 750).centerCrop().into(moviePoster);
            } else {
                File posterFile = new File(moviePosterPath);
                Picasso.with(getActivity()).load(posterFile)
                        .error(R.mipmap.ic_launcher).resize(500, 750).centerCrop().into(moviePoster);
            }
        }

        TextView originalTitle = (TextView) getActivity().findViewById(R.id.original_title);
        originalTitle.setText(bundle.getString(
                FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE));

        TextView releaseDate = (TextView) getActivity().findViewById(R.id.release_date);
        String rawDate = bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE);
        releaseDate.setText(formatDate(rawDate));

        TextView voteAverage = (TextView) getActivity().findViewById(R.id.average);
        String average = String.format(getActivity().getResources().getString(R.string.average_score),
                bundle.getDouble(FavoritesContract.FavoritesEntry.COLUMN_AVERAGE));
        voteAverage.setText(average);

        TextView overview = (TextView) getActivity().findViewById(R.id.overview);
        overview.setText(bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW));

        try {
            updateTrailerAndReviewList(
                    bundle.getLong(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID));
        } catch (NullPointerException nex) {
            Log.e(LOG_TAG, "NullPointerException " + nex.getMessage());
        }

        String[] projection = {FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID};
        String[] selectionArgs = {String.valueOf(bundle
                .getLong(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID))};
        String selection = FavoritesContract.FavoritesEntry.TABLE_NAME
                + "." + FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID + " = ?";
        Cursor retCursor = getActivity().getContentResolver()
                .query(FavoritesContract.FavoritesEntry.CONTENT_URI, projection,
                        selection, selectionArgs, null);

        if (retCursor != null && retCursor.moveToFirst()) {
            mIsFavorite = true;
            mFavoriteFAB.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(getActivity(), R.color.colorAccent)));
        } else {
            mIsFavorite = false;
            mFavoriteFAB.setBackgroundTintList(
                    ColorStateList.valueOf(
                            ContextCompat.getColor(getActivity(), R.color.grey)));
        }
        if (retCursor != null) {
            retCursor.close();
        }
    }

    /*
    * Starts the thread that will retrieve trailers and reviews for a particular movie.
    * */
    private void updateTrailerAndReviewList(Long id) {
        ConnectivityManager manager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        if (networkInfo.isAvailable() && networkInfo.isConnected()) {
            new FetchTrailersTask(getActivity(), this).execute(String.valueOf(id));
            new FetchReviewsTask(this).execute(String.valueOf(id));
        }
    }

    /*
    * Displays the reviews retrieved from the web service.
    * */
    @Override
    public void updateTrailers(ArrayList<TrailerItem> trailers) {
        if (trailers.size() != 0) {
            TextView trailerListTitle = (TextView) getActivity().findViewById(R.id.trailers_title);
            trailerListTitle.setVisibility(View.VISIBLE);
        }

        if (mTrailers.size() != 0) {
            mTrailers.clear();
        }
        mTrailers.addAll(trailers);
        mTrailerAdapter.notifyDataSetChanged();
    }

    /*
    * Displays the reviews retrieved from the web service.
    * */
    @Override
    public void updateReviews(ArrayList<ReviewItem> reviews) {

        if (reviews.size() != 0) {
            TextView reviewListTitle = (TextView) mReviewLayout.findViewById(R.id.review_title);
            reviewListTitle.setVisibility(View.VISIBLE);
        }

        for (ReviewItem item : reviews) {
            View reviewItemLayout =
                    LayoutInflater.from(getActivity())
                            .inflate(R.layout.review_item, mReviewLayout, false);

            TextView reviewContent = (TextView) reviewItemLayout.findViewById(R.id.review_content);
            reviewContent.setText(item.getContent());

            TextView reviewAuthor = (TextView) reviewItemLayout.findViewById(R.id.review_author);
            String author = String.format(getActivity().getResources().getString(R.string.by_author), item.getAuthor());
            reviewAuthor.setText(author);

            if (mReviewLayout != null) {
                mReviewLayout.addView(reviewItemLayout);
            }
        }
    }

    /*
    * Redirects the user to Youtube or the web browser to watch a trailer.
    * */
    @Override
    public void recyclerViewListClicked(View v, int position) {
        TrailerItem trailer = mTrailers.get(position);
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailer.appUrl));
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailer.browserUrl));

        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(browserIntent);
        }
    }

    /*
    * A method to find height of the status bar
    * */
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detail_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                if (this.mTrailers.size() > 0) {
                    String shareText =
                            String.format(getString(R.string.share_text),
                                    mBundle.getString(FavoritesContract.FavoritesEntry.COLUMN_TITLE))
                                    + this.mTrailers.get(0).browserUrl;
                    Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, shareText);
                    startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.no_trailers_to_share),
                            Toast.LENGTH_SHORT).show();
                }

        }
        return super.onOptionsItemSelected(item);
    }

    /*
    * Formats the release date to a readable format.
    * */
    public String formatDate(String date) {

        String[] values = date.split("-");
        int[] intValues = new int[3];

        for (int i = 0; i < intValues.length; i++) {
            intValues[i] = Integer.valueOf(values[i]);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(intValues[0], intValues[1], intValues[2]);

        SimpleDateFormat formatter = new SimpleDateFormat(getActivity().getString(R.string.formatted_date));
        return formatter.format(calendar.getTime());

    }

    private class DownloadImageTask extends AsyncTask<String, Void, String> {

        private Context mContext;

        DownloadImageTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected String doInBackground(String... strings) {

            String imageSource = strings[0];
            String filePath = null;

            if (!imageSource.startsWith("/data")) {
                imageSource = NetworkUtils.POSTER_PATH_AUTHORITY + imageSource;
            }

            Bitmap poster = NetworkUtils.getImageFromUrl(getActivity(), imageSource);
            if (poster != null) {
                String fileName = mBundle.getString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH);
                if(fileName != null){
                    filePath = NetworkUtils.savePosterIntoStorage(fileName, mContext, poster);
                }
            }
            return filePath;
        }

        @Override
        protected void onPostExecute(String filePath) {
            super.onPostExecute(filePath);
            if (filePath != null) {
                mBundle.putString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH, filePath);
            }
            if (NetworkUtils.insertMovieIntoDB(mBundle, mContext)) {
                Toast.makeText(
                        mContext, getString(R.string.added_as_favorite), Toast.LENGTH_SHORT).show();
                mIsFavorite = true;
                mFavoriteFAB.setBackgroundTintList(
                        ColorStateList.valueOf(
                                ContextCompat.getColor(getActivity(), R.color.colorAccent)));
            } else {
                Toast.makeText(
                        mContext, getString(R.string.save_movie_error), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
