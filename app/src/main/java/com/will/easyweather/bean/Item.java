package com.will.easyweather.bean;

public class Item {
	public static final int INFINITE_ID = -1;
	public static final int FEEDBACK_ID = -2;
	public static final int ABOUT_ID = -3;

	public int mId;
	public String mTitleStr;
	public int mIconRes;

	public Item(int id, String title, int iconRes) {
		mId = id;
		mTitleStr = title;
		mIconRes = iconRes;
	}
}
