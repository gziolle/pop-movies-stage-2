/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.gziolle.popmovies.data.FavoritesContract;

/**
 * Hosts the "Details" screen (DetailFragment.java)
 * for the movie selected by the user.
 */

public class DetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();

        Bundle bundle = new Bundle();
        Log.i(LOG_TAG, "item.getId() = " +
                intent.getLongExtra(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID, 0));

        if (intent != null) {
            bundle.putLong(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID,
                    intent.getLongExtra(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID, 0));
            bundle.putString(FavoritesContract.FavoritesEntry.COLUMN_TITLE,
                    intent.getStringExtra(FavoritesContract.FavoritesEntry.COLUMN_TITLE));
            bundle.putString(FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE,
                    intent.getStringExtra(FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE));
            bundle.putString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH,
                    intent.getStringExtra(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH));
            bundle.putString(FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE,
                    intent.getStringExtra(FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE));
            bundle.putString(FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW,
                    intent.getStringExtra(FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW));
            bundle.putDouble(FavoritesContract.FavoritesEntry.COLUMN_AVERAGE,
                    intent.getDoubleExtra(FavoritesContract.FavoritesEntry.COLUMN_AVERAGE, 0));
        }

        if (savedInstanceState == null) {
            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
        }
    }
}
