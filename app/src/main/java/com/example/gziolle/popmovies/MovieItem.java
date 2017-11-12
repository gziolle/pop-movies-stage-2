/*
 * Created by Guilherme Ziolle
 * Copyright (c) 2017. All rights reserved
 */

package com.example.gziolle.popmovies;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents an item displayed in the movies list.
 */

public class MovieItem implements Parcelable {

    private long mId;
    private String mTitle;
    private String originalTitle;
    private String mPosterPath;
    private String mOverview;
    private double mAverage;
    private String mReleaseDate;


    public MovieItem(long mId, String title, String originalTitle, String mPosterPath,
                     String mOverview, double vote_mAverage, String mReleaseDate) {
        this.mId = mId;
        this.mTitle = title;
        this.originalTitle = originalTitle;
        this.mPosterPath = mPosterPath;
        this.mOverview = mOverview;
        this.mAverage = vote_mAverage;
        this.mReleaseDate = mReleaseDate;
    }

    private MovieItem(Parcel in) {
        this.mId = in.readLong();
        this.mTitle = in.readString();
        this.originalTitle = in.readString();
        this.mPosterPath = in.readString();
        this.mOverview = in.readString();
        this.mAverage = in.readDouble();
        this.mReleaseDate = in.readString();
    }

    public long getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getPosterPath() {
        return mPosterPath;
    }

    public String getOverview() {
        return mOverview;
    }

    public double getAverage() {
        return mAverage;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mTitle);
        dest.writeString(mPosterPath);
        dest.writeString(mOverview);
        dest.writeDouble(mAverage);
        dest.writeString(mReleaseDate);
    }

    public static final Parcelable.Creator<MovieItem> CREATOR = new Parcelable.Creator<MovieItem>() {
        public MovieItem createFromParcel(Parcel in) {
            return new MovieItem(in);
        }

        public MovieItem[] newArray(int size) {
            return new MovieItem[size];
        }
    };
}
