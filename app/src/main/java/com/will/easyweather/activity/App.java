package com.will.easyweather.activity;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.will.easyweather.bean.WeatherInfo;
import com.will.easyweather.util.CrashHandler;
import com.will.easyweather.util.SystemUtils;

public class App extends Application {
	public static Map<String, WeatherInfo> mMainMap;

	@Override
	public void onCreate() {
		super.onCreate();
		SystemUtils.copyDB(this);// 程序第一次运行将数据库copy过去
		CrashHandler.getInstance().init(this);
		mMainMap = new HashMap<String, WeatherInfo>();
	}
}
