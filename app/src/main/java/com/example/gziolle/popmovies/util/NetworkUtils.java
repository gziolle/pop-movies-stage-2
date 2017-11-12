/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.gziolle.popmovies.BuildConfig;
import com.example.gziolle.popmovies.MovieItem;
import com.example.gziolle.popmovies.data.FavoritesContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/*
* This class has methods to handle data from the TheMovieDB Service and a
* the "Favorites" database.
* */
public class NetworkUtils {

    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    private static final String TMDB_AUTHORITY = "api.themoviedb.org";
    private static final String TMDB_API_VERSION = "3";
    private static final String TMDB_MOVIE_DIR = "movie";
    private static final String TMDB_API_KEY = "api_key";
    private static final String TMDB_LANGUAGE = "language";
    private static final String TMDB_PAGE = "page";

    private static String TMDB_RESULTS = "results";
    private static String TMDB_ID = "id";
    private static String TMDB_TITLE = "title";
    private static String TMDB_ORIGINAL_TITLE = "original_title";
    private static String TMDB_POSTER_PATH = "poster_path";
    private static String TMDB_OVERVIEW = "overview";
    private static String TMDB_VOTE_AVERAGE = "vote_average";
    private static String TMDB_RELEASE_DATE = "release_date";

    private static int MOVIE_ID = 1;
    private static int MOVIE_TITLE = 2;
    private static int ORIGINAL_MOVIE_TITLE = 3;
    private static int MOVIE_POSTER_PATH = 4;
    private static int MOVIE_OVERVIEW = 5;
    private static int MOVIE_AVERAGE = 6;
    private static int MOVIE_RELEASE_DATE = 7;

    public static final String POSTER_PATH_AUTHORITY = "http://image.tmdb.org/t/p/w342";

    /*
    * Fetches a list of movie fron the TheMovieDB API.
    * */
    public static ArrayList<MovieItem> fetchMoviesFromNetwork(String queryMode, int currentPage) {
        HttpURLConnection conn = null;
        InputStream is;
        BufferedReader reader = null;
        String moviesJSONString;
        ArrayList<MovieItem> movieItems = null;

        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http");
            builder.authority(TMDB_AUTHORITY);
            builder.appendPath(TMDB_API_VERSION).appendPath(TMDB_MOVIE_DIR).appendPath(queryMode);
            builder.appendQueryParameter(TMDB_API_KEY, BuildConfig.THE_MOVIE_DB_KEY);
            builder.appendQueryParameter(TMDB_LANGUAGE, "en-us");
            builder.appendQueryParameter(TMDB_PAGE, String.valueOf(currentPage));

            URL queryUrl = new URL(builder.build().toString());
            Log.i(LOG_TAG, queryUrl.toString());

            conn = (HttpURLConnection) queryUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            is = conn.getInputStream();

                /*Returns null if the connection could not get an InputStream*/
            if (is == null) {
                Log.i(LOG_TAG, "is == null");
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            StringBuilder inputStreamBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                inputStreamBuilder.append(line);
                inputStreamBuilder.append("\n");

            }
            /*Returns null if nothing comes from the server*/
            if (inputStreamBuilder.length() == 0) {
                Log.i(LOG_TAG, "inputStreamBuilder.length() == 0");
                return null;
            }

            moviesJSONString = inputStreamBuilder.toString();

            movieItems = getDataFromJSON(moviesJSONString);

        } catch (IOException | JSONException ex) {
            Log.e(LOG_TAG, ex.getMessage());
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
        return movieItems;
    }

    public static ArrayList<MovieItem> fetchMoviesFromDatabase(Context context) {
        ArrayList<MovieItem> movieList = new ArrayList<>();
        String[] projection = {FavoritesContract.FavoritesEntry._ID,
                FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID,
                FavoritesContract.FavoritesEntry.COLUMN_TITLE,
                FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE,
                FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH,
                FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW,
                FavoritesContract.FavoritesEntry.COLUMN_AVERAGE,
                FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE};


        Cursor favoriteMovies =
                context.getContentResolver()
                        .query(FavoritesContract.FavoritesEntry.CONTENT_URI,
                                projection,
                                null,
                                null,
                                null);

        if (favoriteMovies != null) {
            while (favoriteMovies.moveToNext()) {
                MovieItem item = new MovieItem(favoriteMovies.getLong(MOVIE_ID),
                        favoriteMovies.getString(MOVIE_TITLE),
                        favoriteMovies.getString(ORIGINAL_MOVIE_TITLE),
                        favoriteMovies.getString(MOVIE_POSTER_PATH),
                        favoriteMovies.getString(MOVIE_OVERVIEW),
                        favoriteMovies.getDouble(MOVIE_AVERAGE),
                        favoriteMovies.getString(MOVIE_RELEASE_DATE));
                movieList.add(item);
            }
        }

        if (favoriteMovies != null && !favoriteMovies.isClosed()) {
            favoriteMovies.close();
        }
        return movieList;

    }

    /**
     * Converts the data from the JSON Object into a MovieItem list
     */
    private static ArrayList<MovieItem> getDataFromJSON(String JSONString) throws JSONException {
        ArrayList<MovieItem> movieItems = new ArrayList<>();

        JSONObject mainObject = new JSONObject(JSONString);

        JSONArray moviesArray = mainObject.getJSONArray(TMDB_RESULTS);

        for (int i = 0; i < moviesArray.length(); i++) {
            JSONObject movie = moviesArray.getJSONObject(i);
            MovieItem item = new MovieItem(movie.getLong(TMDB_ID), movie.getString(TMDB_TITLE),
                    movie.getString(TMDB_ORIGINAL_TITLE),
                    movie.getString(TMDB_POSTER_PATH), movie.getString(TMDB_OVERVIEW),
                    movie.getDouble(TMDB_VOTE_AVERAGE), movie.getString(TMDB_RELEASE_DATE));
            Log.i("Ziolle", "item.getId() = " + item.getId());
            movieItems.add(item);
        }
        return movieItems;
    }

    /*
    * Saves the movie poster into internal storage for future use.
    */
    public static String savePosterIntoStorage(String fileName, Context context, Bitmap bitmap) {

        ContextWrapper cw = new ContextWrapper(context);

        //path to /data/data/com.example.gziolle.popmovies/app/data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);

        //Creates the file
        File filePath = new File(directory, fileName);

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(filePath);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException | NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        return filePath.getAbsolutePath();
    }

    /*
    * Gets a image from a web address using HTTPURLConnection
    * */
    public static Bitmap getImageFromUrl(Context context, String source) {
        Bitmap bitmapPoster = null;
        try {
            if (isConnected(context)) {
                URL url = new URL(source);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream is = connection.getInputStream();
                bitmapPoster = BitmapFactory.decodeStream(is);
            }
            return bitmapPoster;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }
    /*
    * Stores a movie selected as favorite into the database.
    * */
    public static boolean insertMovieIntoDB(Bundle bundle, Context context) {
        ContentValues values = new ContentValues();
        values.put(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID,
                String.valueOf(bundle.getLong(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID)));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_TITLE,
                bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_TITLE));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE,
                bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_ORIGINAL_MOVIE_TITLE));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH,
                bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_POSTER_PATH));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW,
                bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_OVERVIEW));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_AVERAGE,
                String.valueOf(bundle.getDouble(FavoritesContract.FavoritesEntry.COLUMN_AVERAGE)));
        values.put(FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE,
                bundle.getString(FavoritesContract.FavoritesEntry.COLUMN_RELEASE_DATE));

        Uri rowUri = context.getContentResolver()
                .insert(FavoritesContract.FavoritesEntry.CONTENT_URI, values);

        return rowUri != null;

    }

    /*
    * Removes a movie from the database.
    * */
    public static boolean deleteMovieFromDB(Bundle bundle, Context context) {
        String selection = FavoritesContract.FavoritesEntry.TABLE_NAME + "." +
                FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID + " = ?";
        String[] selectionArgs =
                {String.valueOf(bundle.getLong(FavoritesContract.FavoritesEntry.COLUMN_MOVIE_ID))};

        int rowsCount = context.getContentResolver()
                .delete(FavoritesContract.FavoritesEntry.CONTENT_URI, selection, selectionArgs);

        return (rowsCount != -1);
    }

    /*
    * Checks if the device is connected to the internet.
    * */
    public static boolean isConnected(Context context) {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

}
