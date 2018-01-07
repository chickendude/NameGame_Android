package com.willowtreeapps.namegame.ui.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Score implements Parcelable {
	private String field;
	private String value;
	private int points;

	protected Score(Parcel in) {
		field = in.readString();
		value = in.readString();
		points = in.readInt();
	}

	public Score(String field, String value, int points) {
		this.field = field;
		this.value = value;
		this.points = points;
	}

	public String getField() {
		return field;
	}

	public String getValue() {
		return value;
	}

	public int getPoints() {
		return points;
	}

	public static final Creator<Score> CREATOR = new Creator<Score>() {
		@Override
		public Score createFromParcel(Parcel in) {
			return new Score(in);
		}

		@Override
		public Score[] newArray(int size) {
			return new Score[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(field);
		dest.writeString(value);
		dest.writeInt(points);
	}
}
