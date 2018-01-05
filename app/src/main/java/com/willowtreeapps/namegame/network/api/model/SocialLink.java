package com.willowtreeapps.namegame.network.api.model;

import android.os.Parcel;
import android.os.Parcelable;

public class SocialLink implements Parcelable {
	String type;
	String callToAction;
	String url;

	public SocialLink(String type, String callToAction, String url) {
		this.type = type;
		this.callToAction = callToAction;
		this.url = url;
	}

	protected SocialLink(Parcel in) {
		type = in.readString();
		callToAction = in.readString();
		url = in.readString();
	}

	public String getType() {
		return type;
	}

	public String getCallToAction() {
		return callToAction;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(type);
		dest.writeString(callToAction);
		dest.writeString(url);
	}

	public static final Creator<SocialLink> CREATOR = new Creator<SocialLink>() {
		@Override
		public SocialLink createFromParcel(Parcel in) {
			return new SocialLink(in);
		}

		@Override
		public SocialLink[] newArray(int size) {
			return new SocialLink[size];
		}
	};
}
