package com.will.easyweather.activity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import com.will.easyweather.R;
import com.will.easyweather.bean.City;
import com.will.easyweather.bean.WeatherInfo;
import com.will.easyweather.db.CityProvider;
import com.will.easyweather.db.CityProvider.CityConstants;
import com.will.easyweather.util.LocationUtils;
import com.will.easyweather.util.NetUtil;
import com.will.easyweather.util.SystemUtils;
import com.will.easyweather.util.T;
import com.will.easyweather.weather.WeatherSpider;

import java.util.List;

public class BaseActivity extends Activity{
	public static final String AUTO_LOCATION_CITY_KEY = "auto_location";
	protected ContentResolver mContentResolver;
	protected Activity mActivity;
	protected LocationUtils mLocationUtils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initDatas();
	}

	private void initDatas() {
		mActivity = this;
		mContentResolver = getContentResolver();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	protected List<City> getTmpCities() {
		Cursor tmpCityCursor = mContentResolver.query(
				CityProvider.TMPCITY_CONTENT_URI, null, null, null, null);
		return SystemUtils.getTmpCities(tmpCityCursor);
	}

	protected void locate(LocationUtils.LocationListener locationListener) {
		if (NetUtil.getNetworkState(this) == NetUtil.NET_NONE) {
			T.showShort(this, R.string.net_error);
			return;
		}
		if (mLocationUtils == null)
			mLocationUtils = new LocationUtils(this, locationListener);
		if (!mLocationUtils.isStarted()) {
			mLocationUtils.startLocation();// 开始定位
		}
	}

	protected void stopLocation() {
		if (mLocationUtils != null && mLocationUtils.isStarted())
			mLocationUtils.stopLocation();
	}

	protected City getLocationCityFromDB(String name) {
		City city = new City();
		city.setName(name);
		Cursor c = mContentResolver.query(CityProvider.CITY_CONTENT_URI,
				new String[] { CityConstants.POST_ID }, CityConstants.NAME
						+ "=?", new String[] { name }, null);
		if (c != null && c.moveToNext())
			city.setPostID(c.getString(c.getColumnIndex(CityProvider.CityConstants.POST_ID)));
		return city;
	}

	protected void updateLocationCity(City city) {
		// 先删除已定位城市
		mContentResolver.delete(CityProvider.TMPCITY_CONTENT_URI,
				CityConstants.ISLOCATION + "=?", new String[] { "1" });

		// 存储
		ContentValues tmpContentValues = new ContentValues();
		tmpContentValues.put(CityConstants.NAME, city.getName());
		tmpContentValues.put(CityConstants.POST_ID, city.getPostID());
		tmpContentValues.put(CityConstants.REFRESH_TIME, 0L);// 无刷新时间
		tmpContentValues.put(CityConstants.ISLOCATION, 1);// 手动选择的城市存储为0
		mContentResolver.insert(CityProvider.TMPCITY_CONTENT_URI,
				tmpContentValues);
	}

	public WeatherInfo getWeatherInfo(String postID, boolean isForce) {
		try {
			WeatherInfo weatherInfo = WeatherSpider.getInstance()
					.getWeatherInfo(BaseActivity.this, postID, isForce);
			if (WeatherSpider.isEmpty(weatherInfo)) {
				Toast.makeText(this, R.string.get_weatherifo_fail,
						Toast.LENGTH_SHORT).show();
				return null;
			}
			// 将刷新时间存储到数据库
			if (weatherInfo.getIsNewDatas()) {
				ContentValues contentValues = new ContentValues();
				contentValues.put(CityConstants.REFRESH_TIME,
						System.currentTimeMillis());
				mContentResolver.update(CityProvider.TMPCITY_CONTENT_URI,
						contentValues, CityConstants.POST_ID + "=?",
						new String[] { postID });
			}
			App.mMainMap.put(postID, weatherInfo);// 保存到全局变量
			return weatherInfo;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
