package com.willowtreeapps.namegame.ui.objects;

import android.os.Parcel;
import android.os.Parcelable;

public class Answer implements Parcelable {
	private boolean isCorrect;
	private int timeToAnswer;

	public Answer(boolean isCorrect, int timeToAnswer) {
		this.isCorrect = isCorrect;
		this.timeToAnswer = timeToAnswer;
	}

	protected Answer(Parcel in) {
		isCorrect = in.readByte() != 0;
		timeToAnswer = in.readInt();
	}

	public boolean isCorrect() {
		return isCorrect;
	}

	public int getTimeToAnswer() {
		return timeToAnswer;
	}

	public static final Creator<Answer> CREATOR = new Creator<Answer>() {
		@Override
		public Answer createFromParcel(Parcel in) {
			return new Answer(in);
		}

		@Override
		public Answer[] newArray(int size) {
			return new Answer[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte((byte) (isCorrect ? 1 : 0));
		dest.writeInt(timeToAnswer);
	}
}
