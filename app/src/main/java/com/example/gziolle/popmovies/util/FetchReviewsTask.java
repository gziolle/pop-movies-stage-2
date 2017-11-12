/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies.util;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.gziolle.popmovies.BuildConfig;
import com.example.gziolle.popmovies.ReviewItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;


public class FetchReviewsTask extends AsyncTask<String, Void, ArrayList<ReviewItem>> {
    public static final String TMDB_AUTHORITY = "api.themoviedb.org";
    public static final String TMDB_API_VERSION = "3";
    public static final String TMDB_MOVIE_DIR = "movie";
    public static final String TMDB_MOVIE_REVIEWS = "reviews";
    public static final String TMDB_API_KEY = "api_key";
    public static final String TMDB_LANGUAGE = "language";
    public static final String TMDB_PAGE = "page";

    private static final String RESULTS = "results";
    private static final String TMDB_AUTHOR = "author";
    private static final String TMDB_ID = "id";
    private static final String TMDB_URL = "url";
    private static final String TMDB_CONTENT = "content";
    private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();

    public AsyncResponse response = null;

    public FetchReviewsTask(AsyncResponse response) {
        this.response = response;
    }

    @Override
    protected ArrayList<ReviewItem> doInBackground(String... params) {

        HttpURLConnection conn = null;
        InputStream is;
        BufferedReader reader = null;
        String jResult;
        ArrayList<ReviewItem> reviewList = new ArrayList<>();

        if (params[0] == null) {
            return null;
        }

        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http");
            builder.authority(TMDB_AUTHORITY);
            builder.appendPath(TMDB_API_VERSION).appendPath(TMDB_MOVIE_DIR).appendPath(params[0])
                    .appendPath(TMDB_MOVIE_REVIEWS);
            builder.appendQueryParameter(TMDB_API_KEY, BuildConfig.THE_MOVIE_DB_KEY);
            builder.appendQueryParameter(TMDB_LANGUAGE, Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());
            builder.appendQueryParameter(TMDB_PAGE, "1");

            URL queryUrl = new URL(builder.build().toString());

            conn = (HttpURLConnection) queryUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            is = conn.getInputStream();

            //Error handling
            if (is == null) {
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            StringBuffer buffer = new StringBuffer();

            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            //Error handling
            if (buffer.length() == 0) {
                return null;
            }

            jResult = buffer.toString();

            reviewList = getDataFromJSON(jResult);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());

        } finally {
            if (conn != null) {
                conn.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
        return reviewList;
    }

    @Override
    protected void onPostExecute(ArrayList<ReviewItem> reviewList) {
        super.onPostExecute(reviewList);
        if (reviewList != null) {
            response.updateReviews(reviewList);
        }
    }

    private ArrayList<ReviewItem> getDataFromJSON(String jString) throws JSONException {
        ArrayList<ReviewItem> reviewList = new ArrayList<>();

        JSONObject mainObject = new JSONObject(jString);
        JSONArray reviewArray = mainObject.getJSONArray(RESULTS);

        for (int i = 0; i < reviewArray.length(); i++) {
            JSONObject trailer = reviewArray.getJSONObject(i);
            reviewList.add(new ReviewItem(trailer.getString(TMDB_ID), trailer.getString(
                    TMDB_AUTHOR), trailer.getString(TMDB_CONTENT),
                    trailer.getString(TMDB_URL)));
        }
        return reviewList;
    }

    public interface AsyncResponse {
        void updateReviews(ArrayList<ReviewItem> reviews);
    }
}