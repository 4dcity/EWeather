package com.will.easyweather.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnPullEventListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.State;
import com.handmark.pulltorefresh.library.PullToRefreshScrollView;
import com.will.easyweather.R;
import com.will.easyweather.activity.App;
import com.will.easyweather.activity.BaseActivity;
import com.will.easyweather.adapter.WeatherListAdapter;
import com.will.easyweather.bean.AQI;
import com.will.easyweather.bean.City;
import com.will.easyweather.bean.Forecast;
import com.will.easyweather.bean.Index;
import com.will.easyweather.bean.RealTime;
import com.will.easyweather.bean.WeatherInfo;
import com.will.easyweather.db.CityProvider;
import com.will.easyweather.util.SystemUtils;
import com.will.easyweather.util.T;
import com.will.easyweather.util.TimeUtils;
import com.will.easyweather.weather.WeatherIconUtils;
import com.will.easyweather.weather.WeatherSpider;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
@SuppressLint("ValidFragment")
public class WeatherFragment extends Fragment implements OnRefreshListener,
		OnPullEventListener {
	public static final String ARG_CITY = "city";
	public static final String ARG_WEATHERINFO = "weatherInfo";
	public static final String REFRESH_TIME_KEY = "refreshTime";
	private GetDataTask mGetDataTask;
	private ListView mListView;
	private PullToRefreshScrollView mPullRefreshScrollView;
	private WeatherListAdapter mWeatherAdapter;
	private ImageView mNormalImageView;
	private ImageView mBlurredImageView;
	private View mListHeaderView;

	private int mLastDampedScroll;
	private int mHeaderHeight = -1;

	// 当前天气的View
	private ImageView mCurWeatherIV;
	private TextView mCurWeatherTV;
	private TextView mCurHighTempTV;
	private TextView mCurLowTempTV;
	private TextView mCurFeelsTempTV;
	private TextView mCurWeatherCopyTV;

	private ContentResolver mContentResolver;
	private BaseActivity mActivity;
	private City mCurCity;

	public WeatherFragment() {
	}

	public WeatherFragment(BaseActivity activity, City city) {
		mActivity = activity;
		mCurCity = city;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContentResolver = getActivity().getContentResolver();
	}

	private View mView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.weather_fragment, container, false);
		return mView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initViews(view);
	}

	/**
	 * 初始化所有的view
	 * 
	 * @param view
	 */
	private void initViews(View view) {
		mListView = (ListView) view.findViewById(R.id.drag_list);
		mNormalImageView = (ImageView) view
				.findViewById(R.id.weather_background);
		mBlurredImageView = (ImageView) view
				.findViewById(R.id.weather_background_blurred);
		mBlurredImageView.getDrawable().setAlpha(0 * 255);// 设置默认模糊背景为透明

		mPullRefreshScrollView = (PullToRefreshScrollView) view
				.findViewById(R.id.pull_refresh_scrollview);
		// 添加下拉刷新事件
		mPullRefreshScrollView.setOnRefreshListener(this);
		// 添加下拉刷新状态事件，以便及时显示刷新时间
		mPullRefreshScrollView.setOnPullEventListener(this);
		//Header 显示当前城市的基本信息
		mListHeaderView = LayoutInflater.from(getActivity()).inflate(
				R.layout.weather_current_condition, null);
		// 获取屏幕高度
		int displayHeight = SystemUtils.getDisplayHeight(getActivity());
		// HeaderView高度=屏幕高度-标题栏高度-状态栏高度
		mHeaderHeight = displayHeight
				- getResources().getDimensionPixelSize(
						R.dimen.abs__action_bar_default_height)
				- SystemUtils.getStatusBarHeight(getActivity());
		mListHeaderView.setLayoutParams(new LayoutParams(
				LayoutParams.MATCH_PARENT, mHeaderHeight));
		// 计算背景View的高度，适当比屏幕高度多一点，
		// 之所以多1/8是为了后面滑动ListView时背景能跟随滑动。
		int backgroundHeight = displayHeight
				- SystemUtils.getStatusBarHeight(getActivity()) + mHeaderHeight
				/ 8;
		mNormalImageView.setLayoutParams(new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, backgroundHeight));
		mBlurredImageView.setLayoutParams(new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT, backgroundHeight));

		mListView.addHeaderView(mListHeaderView, null, false);// 给ListView添加HeaderView
		mWeatherAdapter = new WeatherListAdapter(getActivity());
		mListView.setAdapter(mWeatherAdapter);
		mListView.setOnScrollListener(mOnScrollListener);// 监听滑动
		initCurWeatherViews(view);
		mListView.setSelection(0);// 默认选中最上面一个view
	}

	/**
	 * 初始化当前天气的view，即ListView的HeaderView
	 * 
	 * @param view
	 */
	private void initCurWeatherViews(View view) {
		mCurWeatherIV = (ImageView) view.findViewById(R.id.main_icon);
		mCurWeatherTV = (TextView) view.findViewById(R.id.weather_description);
		mCurHighTempTV = (TextView) view.findViewById(R.id.temp_high);
		mCurLowTempTV = (TextView) view.findViewById(R.id.temp_low);
		mCurFeelsTempTV = (TextView) view.findViewById(R.id.temperature);
		mCurWeatherCopyTV = (TextView) view.findViewById(R.id.copyright);

		//这里取出城市的详细预报信息，然后绑定UI
		updateWeatherView(App.mMainMap.get(mCurCity.getPostID()), false);
	}

	@Override
	public void onResume() {
		super.onResume();
		mListView.setSelection(0);// 选中第一个view，当fragment被回收后再重新创建时恢复状态
	}

	// ListView滑动监听，更新背景模糊度和移动距离
	private OnScrollListener mOnScrollListener = new OnScrollListener() {
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			View topChild = view.getChildAt(0);// 获取ListView的第一个View
			if (topChild == null) {
				onNewScroll(0);
			} else if (topChild != mListHeaderView) {
				//当 headerview 滚出屏幕，则不再更滑动距离
				onNewScroll(mListHeaderView.getHeight());
			} else {
				onNewScroll(-topChild.getTop());
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// do nothing
		}
	};

	/**
	 * 更新背景模糊度和移动距离
	 * 
	 * @param scrollPosition
	 */
	private void onNewScroll(int scrollPosition) {
		// 控制是否可以下拉刷新，只有当 ListView 滚动到顶部的时候才ScrollView 才可以下来刷新
		if (scrollPosition == 0) {
			mPullRefreshScrollView.setMode(Mode.PULL_FROM_START);// 可以下拉刷新
		} else {
			if (mPullRefreshScrollView.getState() == State.RESET) {
				mPullRefreshScrollView.setMode(Mode.DISABLED);
			} else if (scrollPosition > mHeaderHeight / 2) {
				// 上滑距离超过HeaderView高度一半时，取消刷新，同时要注意停止异步任务
				mPullRefreshScrollView.onRefreshComplete();
				if (mGetDataTask != null)
					mGetDataTask.cancel(true);
			}
		}

		// 控制模糊背景的alpha值，当滑动到Header高度的2/3时模糊背景变为完全不透明
		float ratio = Math.min(1.5f * (-mListHeaderView.getTop())
				/ mHeaderHeight, 1.0f);
		int newAlpha = Math.round(ratio * 255);
		mBlurredImageView.getDrawable().setAlpha(newAlpha);

		// 控制背景滑动距离
		int dampedScroll = Math.round(scrollPosition * 0.125f);
		int offset = mLastDampedScroll - dampedScroll;
		mBlurredImageView.offsetTopAndBottom(offset);
		mNormalImageView.offsetTopAndBottom(offset);
		mLastDampedScroll = dampedScroll;
	}

	/**
	 * 异步任务获取天气信息
	 * 
	 */
	private class GetDataTask extends AsyncTask<Void, Void, WeatherInfo> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Call setRefreshing when the list begin to refresh.
			mPullRefreshScrollView.setRefreshing(true);
		}

		@Override
		protected WeatherInfo doInBackground(Void... params) {
			// Simulates a background job.
			try {
				return mActivity.getWeatherInfo(mCurCity.getPostID(), true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(WeatherInfo result) {
			super.onPostExecute(result);
			mPullRefreshScrollView.onRefreshComplete();
			if (!WeatherSpider.isEmpty(result)) {
				if (getActivity() != null)
					T.showShort(getActivity(), "刷新成功:" + mCurCity.getName());
				updateWeatherView(result, true);
				App.mMainMap.put(mCurCity.getPostID(), result);// 保存到全局变量
			} else {
				if (getActivity() != null)
					T.showShort(getActivity(), "刷新失败:" + mCurCity.getName());
			}

		}

	}

	/**
	 * 更新天气信息界面
	 */
	private void updateWeatherView(WeatherInfo weatherInfo, boolean isFroce) {
		if (WeatherSpider.isEmpty(weatherInfo)) {
			if (getActivity() != null)
				T.showShort(getActivity(), R.string.get_weatherifo_fail);
			return;
		}

		RealTime realTime = weatherInfo.getRealTime();
		AQI aqi = weatherInfo.getAqi();
		Forecast forecast = weatherInfo.getForecast();
		Index index = weatherInfo.getIndex();

		int type = realTime.getAnimation_type();
		mNormalImageView.setImageBitmap(SystemUtils.readBitMap(mActivity,
				WeatherIconUtils.getRawNromalBg(type)));
		mBlurredImageView.setImageBitmap(SystemUtils.readBitMap(mActivity,
				WeatherIconUtils.getRawBlurBg(type)));
		mCurWeatherIV.setImageResource(WeatherIconUtils.getWeatherIcon(type));
		mCurWeatherTV.setText(realTime.getWeather_name());
		mCurFeelsTempTV.setText(realTime.getTemp() + "");
		mCurHighTempTV.setText(forecast.getTmpHigh(1) + "°");
		mCurLowTempTV.setText(forecast.getTmpLow(1) + "°");
		mCurWeatherCopyTV.setText(TimeUtils.getDay(realTime.getPub_time())
				+ "发布");

		mWeatherAdapter.setWeather(realTime, aqi, forecast, index);
	}

	@Override
	public void onPullEvent(PullToRefreshBase refreshView, State state,
			Mode direction) {
		// 开始下拉时更新上次刷新时间
		if (state == State.PULL_TO_REFRESH) {
			Cursor c = mContentResolver.query(CityProvider.TMPCITY_CONTENT_URI,
					new String[] { CityProvider.CityConstants.REFRESH_TIME },
					CityProvider.CityConstants.POST_ID + "=?",
					new String[] { mCurCity.getPostID() }, null);

			long time = 0L;
			if (c.moveToFirst())
				time = c.getLong(c.getColumnIndex(CityProvider.CityConstants.REFRESH_TIME));
			String label = String.format(
					getResources().getString(
							R.string.pull_to_refresh_pull_sub_label),
					getResources().getString(
							R.string.pull_to_refresh_pull_sub_label_none));
			if (time > 0)
				label = String.format(
						getResources().getString(
								R.string.pull_to_refresh_pull_sub_label),
						TimeUtils.getListTime(getResources(), time));
			// 更新下拉刷新时间显示
			mPullRefreshScrollView.getLoadingLayoutProxy().setLastUpdatedLabel(
					label);
		}
	}

	@Override
	public void onRefresh(PullToRefreshBase refreshView) {
		// 如果正在刷新，则返回
		if (mGetDataTask != null && mGetDataTask.getStatus() == Status.RUNNING)
			return;
		mGetDataTask = new GetDataTask();
		mGetDataTask.execute();
	}
}
