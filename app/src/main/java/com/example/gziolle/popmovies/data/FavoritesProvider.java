/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/*
* A Content Provider for the "Favorites" database.
* It handles all CRUD operations.
* */
public class FavoritesProvider extends ContentProvider {

    static final int FAVORITES = 101;
    private static UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sFavoritesQueryBuilder;
    private static final String LOG_TAG = FavoritesProvider.class.getSimpleName();

    static {
        sFavoritesQueryBuilder = new SQLiteQueryBuilder();
        sFavoritesQueryBuilder.setTables(FavoritesContract.FavoritesEntry.TABLE_NAME);
    }

    private FavoritesDbHelper mOpenHelper;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = FavoritesContract.CONTENT_AUTHORITY;
        matcher.addURI(authority, FavoritesContract.PATH_FAVORITES, FAVORITES);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new FavoritesDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FAVORITES:
                retCursor = sFavoritesQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Failed to query rows on " + uri);
        }
        try {
            ContentResolver resolver = getContext().getContentResolver();
            retCursor.setNotificationUri(resolver, uri);
            return retCursor;
        } catch (NullPointerException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case FAVORITES:
                return FavoritesContract.FavoritesEntry.CONTENT_DIR_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri = null;

        switch (match) {
            case FAVORITES: {
                long _id = db.insert(FavoritesContract.FavoritesEntry.TABLE_NAME, null, contentValues);
                if (_id > 0) {
                    returnUri = FavoritesContract.FavoritesEntry.buildFavoritesUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into: " + uri);
                }
            }
        }

        try {
            getContext().getContentResolver().notifyChange(uri, null);
        } catch (NullPointerException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
        db.close();
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsCount;

        switch (match) {
            case FAVORITES: {
                rowsCount = db.delete(FavoritesContract.FavoritesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Falied to delete rows from " + uri);
        }

        if (rowsCount != 0) {
            try {
                getContext().getContentResolver().notifyChange(uri, null);
            } catch (NullPointerException ex) {
                Log.e(LOG_TAG, ex.getMessage());
            }
        }

        return rowsCount;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsCount;

        switch (match) {
            case FAVORITES: {
                rowsCount = db.update(FavoritesContract.FavoritesEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Falied to update rows from " + uri);
        }

        if (rowsCount != 0) {

            try {
                getContext().getContentResolver().notifyChange(uri, null);
            } catch (NullPointerException ex) {
                Log.e(LOG_TAG, ex.getMessage());
            }
        }
        return rowsCount;
    }
}
