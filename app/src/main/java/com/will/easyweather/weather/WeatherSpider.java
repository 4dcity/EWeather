package com.will.easyweather.weather;

import android.content.Context;
import android.text.TextUtils;

import com.will.easyweather.bean.AQI;
import com.will.easyweather.bean.Alerts;
import com.will.easyweather.bean.Forecast;
import com.will.easyweather.bean.Index;
import com.will.easyweather.bean.RealTime;
import com.will.easyweather.bean.WeatherInfo;
import com.will.easyweather.util.ConfigCache;
import com.will.easyweather.util.HttpUtils;
import com.will.easyweather.util.NetUtil;
import com.will.easyweather.util.Tools;

import java.io.File;

/**
 * 实现三个功能：
 * 1.构造查询 Url
 * 2.获取 jason 返回数据
 * 3.构造天气的 bean 对象（具体的解析在{@link WeatherController}中实现）
 */
public class WeatherSpider {

	private static final String WEATHER_ALL = "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=%s";
	private static final int FORCAST_FROM_ALL = 0;
	private static final int REALTIME_FROM_ALL = 1;
	private static final int AQI_FROM_ALL = 2;
	private static final int ALERT_FROM_ALL = 3;

	private static WeatherSpider mInstance = null;

	private WeatherSpider() {
	}

	public synchronized static WeatherSpider getInstance() {
		if (mInstance == null)
			mInstance = new WeatherSpider();
		return mInstance;
	}

	private String generatePartWeatherInfo(String weatherResult, int type) {
		switch (type) {
		case FORCAST_FROM_ALL:
			return weatherResult.replace("forecast", "weatherinfo");
		case REALTIME_FROM_ALL:
			return weatherResult.replace("realtime", "weatherinfo");
		case AQI_FROM_ALL:
			return weatherResult;
		case ALERT_FROM_ALL:
			return weatherResult.replace("alert", "weatherinfo");
		default:
			return null;
		}

	}

	/**
	 * 分段解析 json 数据为 bean 对象
	 * @param context
	 * @param postID
	 * @param forecastInfo
	 * @param realTimeInfo
	 * @param aqiInfo
	 * @param alertInfo
	 * @return
	 */
	private WeatherInfo generateWeatherStruct(Context context, String postID,
			String forecastInfo, String realTimeInfo, String aqiInfo,
			String alertInfo) {

		String language = context.getResources().getConfiguration().locale
				.toString();
		Forecast forecast = WeatherController.convertToNewForecast(
				forecastInfo, language, postID);
		RealTime realTime = WeatherController.convertToNewRealTime(
				realTimeInfo, language, postID);
		Alerts alerts = WeatherController.convertToNewAlert(alertInfo, postID);
		Index index = WeatherController.convertToNewIndex(forecastInfo,
				language, postID);
		AQI aqi = WeatherController.convertToNewAQI(aqiInfo, language, postID);
		WeatherInfo weatherInfo = new WeatherInfo(realTime, forecast, aqi,
				index, alerts);
		return weatherInfo;
	}

	public WeatherInfo getWeatherInfo(Context context, String postID,
			boolean forceRefresh) {
		isNewDatas = false;
		String url = generateUrl(context, postID);
		String weatherResult = getResult(context, url, forceRefresh);
		WeatherInfo weatherInfo = generateWeatherStruct(context, postID,
				generatePartWeatherInfo(weatherResult, FORCAST_FROM_ALL),
				generatePartWeatherInfo(weatherResult, REALTIME_FROM_ALL),
				generatePartWeatherInfo(weatherResult, AQI_FROM_ALL),
				generatePartWeatherInfo(weatherResult, ALERT_FROM_ALL));
		weatherInfo.setIsNewDatas(isNewDatas ? 1 : 0);
		if (!isEmpty(weatherInfo))
			ConfigCache.setUrlCache(context, weatherResult, url);// 信息不为空时保存到文件
		return weatherInfo;
	}

	private boolean isNewDatas = false;

	/**
	 * 获取返回json数据
	 * 
	 * @param context
	 * @param url
	 * @param forceRefresh
	 * @return
	 */
	private String getResult(Context context, String url, boolean forceRefresh) {
		String result = ConfigCache.getUrlCache(context, url);
		// 如果没有网络，则返回文件缓存
		if (NetUtil.getNetworkState(context) == NetUtil.NET_NONE)
			return result;
		// 如果有网络，不是强制刷新，则先判断是否有缓存，有就直接返回文件缓存
		if (!forceRefresh && !TextUtils.isEmpty(result))
			return result;
		// 其他条件均不满足，从网上下载
		isNewDatas = true;
		return HttpUtils.getText(url);
	}

	public static boolean isEmpty(WeatherInfo info) {
		if (info == null)
			return true;
		if (info.getRealTime() == null
				|| info.getRealTime().getAnimation_type() < 0)
			return true;
		if (info.getForecast() == null || info.getForecast().getType(1) < 0)
			return true;
		// if (info.getAqi() == null || info.getAqi().getAqi() < 0)
		// return true;
		if (info.getIndex() == null || info.getIndex().getIndex() == null)
			return true;
		return false;
	}

	public static boolean isEmpty(RealTime info) {
		if (info == null || info.getAnimation_type() < 0)
			return true;
		return false;
	}

	public static boolean isEmpty(Forecast info) {
		if (info == null || info.getType(1) < 0)
			return true;
		return false;
	}

	public static boolean isEmpty(AQI info) {
		if (info == null || info.getAqi() < 0)
			return true;
		return false;
	}

	public static boolean isEmpty(Index info) {
		if (info == null || info.getIndex() == null
				|| info.getIndex().get(0) == null)
			return true;
		return false;
	}

	public static boolean isEmpty(Alerts info) {
		if (info == null || info.getArryAlert() == null
				|| info.getArryAlert().get(0) == null)
			return true;
		return false;
	}

	public static void deleteCacheFile(Context context, String postID) {
		String url = generateUrl(context, postID);
		File file = new File(ConfigCache.getCacheDir(context) + File.separator
				+ ConfigCache.replaceUrlWithPlus(url));
		ConfigCache.clearCache(context, file);
	}

	private static String generateUrl(Context context, String postID) {
		String url = String.format(WEATHER_ALL, new Object[]{postID});
		url += Tools.getDeviceInfo(context);
		return url;
	}

}