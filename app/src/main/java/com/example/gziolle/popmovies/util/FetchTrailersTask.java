/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies.util;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.gziolle.popmovies.BuildConfig;
import com.example.gziolle.popmovies.TrailerItem;

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


public class FetchTrailersTask extends AsyncTask<String, Void, ArrayList<TrailerItem>> {
    public static final String TMDB_AUTHORITY = "api.themoviedb.org";
    public static final String TMDB_API_VERSION = "3";
    public static final String TMDB_MOVIE_DIR = "movie";
    public static final String TMDB_MOVIE_VIDEOS = "videos";
    public static final String TMDB_API_KEY = "api_key";
    public static final String TMDB_LANGUAGE = "language";

    private static final String RESULTS = "results";
    private static final String KEY = "key";
    private static final String NAME = "name";
    private final String LOG_TAG = FetchTrailersTask.class.getSimpleName();

    private Context mContext = null;
    private AsyncResponse response = null;

    public FetchTrailersTask(Context context, AsyncResponse response) {
        mContext = context;
        this.response = response;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        /*mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.progress_message));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();*/
    }

    @Override
    protected ArrayList<TrailerItem> doInBackground(String... params) {

        HttpURLConnection conn = null;
        InputStream is;
        BufferedReader reader = null;
        String jResult;
        ArrayList<TrailerItem> trailerList = new ArrayList<>();

        if (params[0] == null) {
            Log.e(LOG_TAG, "params[0] == null");
            return null;
        }

        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http");
            builder.authority(TMDB_AUTHORITY);
            builder.appendPath(TMDB_API_VERSION).appendPath(TMDB_MOVIE_DIR)
                    .appendPath(params[0]).appendPath(TMDB_MOVIE_VIDEOS);
            builder.appendQueryParameter(TMDB_API_KEY, BuildConfig.THE_MOVIE_DB_KEY);
            builder.appendQueryParameter(TMDB_LANGUAGE,
                    Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry());

            URL queryUrl = new URL(builder.build().toString());

            Log.d("Ziolle", "review url = " + builder.build().toString());

            conn = (HttpURLConnection) queryUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            is = conn.getInputStream();

            //Error handling
            if (is == null) {
                Log.e(LOG_TAG, "is == null");
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
                Log.e(LOG_TAG, "buffer.length() == 0");
                return null;
            }

            jResult = buffer.toString();

            trailerList = getDataFromJSON(jResult);
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
        return trailerList;
    }

    @Override
    protected void onPostExecute(ArrayList<TrailerItem> trailerList) {
        super.onPostExecute(trailerList);
//        mProgressDialog.dismiss();
        if (trailerList != null) {
            response.updateTrailers(trailerList);
        }
    }

    private ArrayList<TrailerItem> getDataFromJSON(String jString) throws JSONException {
        ArrayList<TrailerItem> trailerList = new ArrayList<>();

        JSONObject mainObject = new JSONObject(jString);
        JSONArray trailerArray = mainObject.getJSONArray(RESULTS);

        for (int i = 0; i < trailerArray.length(); i++) {
            JSONObject trailer = trailerArray.getJSONObject(i);
            TrailerItem item = new TrailerItem(trailer.getString(KEY), trailer.getString(NAME));
            trailerList.add(item);
        }
        return trailerList;
    }

    public interface AsyncResponse {
        void updateTrailers(ArrayList<TrailerItem> trailers);
    }
}
