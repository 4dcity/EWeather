package com.will.easyweather.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.viewpagerindicator.CirclePageIndicator;
import com.will.easyweather.R;
import com.will.easyweather.adapter.DrawerListAdapter;
import com.will.easyweather.adapter.WeatherPagerAdapter;
import com.will.easyweather.bean.Category;
import com.will.easyweather.bean.City;
import com.will.easyweather.bean.Item;
import com.will.easyweather.bean.WeatherInfo;
import com.will.easyweather.util.L;
import com.will.easyweather.util.LocationUtils;
import com.will.easyweather.util.PreferenceUtils;
import com.will.easyweather.util.SystemUtils;
import com.will.easyweather.util.T;
import com.will.easyweather.util.TimeUtils;

@SuppressLint("NewApi")
public class MainActivity extends BaseActivity implements OnClickListener,
		OnPageChangeListener {
	private static final String INSTANCESTATE_TAB = "tab_index";
	private String mShareNormalStr = "#E天气#提醒您:今天%s,%s,%s,%s,";// 日期、城市、天气、温度
	private String mAqiShareStr = "空气质量指数(AQI):%s μg/m³,等级[%s];PM2.5浓度值:%s μg/m³。%s ";// aqi、等级、pm2.5、建议
	private String mShareEndStr = "（Github：https://github.com/4dcity/Will.EasyWeather）";
	private View mSplashView;
	private static final int SHOW_TIME_MIN = 3000;// 最小显示时间
	private long mStartTime;// 开始时间
	private Handler mHandler;
	private DrawerListAdapter mDrawerAdapter;
	private int mPagerOffsetPixels;
	private int mPagerPosition;
	private TextView mTitleTextView;
	private ImageView mBlurImageView;
	private ImageView mShareBtn;
	private ImageView mLocationIV;
	private Button mAddCityBtn;

	private ListView mDrawerListView;
	private FrameLayout mMainView;
	private ViewPager mMainViewPager;
	private CirclePageIndicator mCirclePageIndicator;
	private WeatherPagerAdapter mFragmentAdapter;
	private List<City> mTmpCities;

	private DrawerLayout mRootLayout;
	private LinearLayout mDrawerLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initDatas();
		initViews();
	}

	/**
	 * 连续按两次返回键就退出
	 */
	private long firstTime;

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstTime < 3000) {
			finish();
		} else {
			firstTime = System.currentTimeMillis();
			T.showShort(this, R.string.press_again_exit);
		}
	}

	private void initDatas() {
		mStartTime = System.currentTimeMillis();// 记录开始时间，
		mHandler = new Handler();
		// mTmpCities = getTmpCities();
		// 第一次进来无定位城市
		if (TextUtils.isEmpty(PreferenceUtils.getPrefString(this, AUTO_LOCATION_CITY_KEY, ""))) {

			locate(new LocationUtils.LocationListener() {
				@Override
				public void detecting() {
					// do nothing
				}

				@Override
				public void succeed(String name) {
					City city = getLocationCityFromDB(name);
					if (TextUtils.isEmpty(city.getPostID())) {
						T.showShort(MainActivity.this, R.string.no_this_city);
					} else {
						PreferenceUtils.setPrefString(MainActivity.this,
								AUTO_LOCATION_CITY_KEY, name);
						L.i("Will", "location" + city.toString());
						updateLocationCity(city);
						T.showShort(MainActivity.this, String.format(
								getResources().getString(R.string.get_location_scuess), name));
					}
				}

				@Override
				public void failed() {
					T.showShort(MainActivity.this, R.string.getlocation_fail);
				}

			});
		}

		mTmpCities = getTmpCities();
		new MyTask().execute(mTmpCities);
	}

	private void initViews() {

		mSplashView = findViewById(R.id.splash_view);
		mBlurImageView = (ImageView) findViewById(R.id.blur_overlay_img);
		mMainView = (FrameLayout) findViewById(R.id.main_view);
		mAddCityBtn = (Button) findViewById(R.id.add_city_btn);
		mAddCityBtn.setOnClickListener(this);
		mMainViewPager = (ViewPager) findViewById(R.id.city_pager);
		mTitleTextView = (TextView) findViewById(R.id.location_city_textview);
		mLocationIV = (ImageView) findViewById(R.id.curr_loc_icon);
		// 设置viewpager缓存view的个数，默认为1个，理论上是越多越好，但是比较耗内存，
		// 我这里设置两个，性能上有点改善
		// mMainViewPager.setOffscreenPageLimit(2);
		mFragmentAdapter = new WeatherPagerAdapter(this);
		mMainViewPager.setAdapter(mFragmentAdapter);

		mCirclePageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
		mCirclePageIndicator.setViewPager(mMainViewPager);
		mCirclePageIndicator.setOnPageChangeListener(this);
		mTitleTextView.setOnClickListener(this);
		findViewById(R.id.sidebarButton).setOnClickListener(this);
		mShareBtn = (ImageView) findViewById(R.id.shareButton);
		mShareBtn.setOnClickListener(this);

		mDrawerListView=(ListView)findViewById(R.id.drawer_listview);
		mDrawerAdapter=new DrawerListAdapter(this);
		mDrawerListView.setAdapter(mDrawerAdapter);
		mDrawerListView.setOnItemClickListener(mItemClickListener);

		mRootLayout =(DrawerLayout)findViewById(R.id.root_layout);
		mDrawerLayout=(LinearLayout)findViewById(R.id.drawer_layout);
	}

	private class MyTask extends AsyncTask<List<City>, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(List<City>... params) {
			List<City> cities = params[0];
			try {
				for (City city : cities) {
					WeatherInfo info = getWeatherInfo(city.getPostID(), false);
					App.mMainMap.put(city.getPostID(), info);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			updateUI();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		mTmpCities = getTmpCities();
		if (App.mMainMap.isEmpty() && !mTmpCities.isEmpty()) {
			new MyTask().execute(mTmpCities);
		} else if (!App.mMainMap.isEmpty()) {
			updateUI();
		} else {
			// 需要定位
			visibleAddCityBtn();
		}
		invisibleSplash();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// 保存默认选择页
		PreferenceUtils.setPrefInt(this, INSTANCESTATE_TAB,
				mMainViewPager.getCurrentItem());
	}

	private void visibleAddCityBtn() {
		mMainViewPager.removeAllViews();
		mTitleTextView.setText("--");
		mLocationIV.setVisibility(View.GONE);
		mAddCityBtn.setVisibility(View.VISIBLE);
		mShareBtn.setEnabled(false);
	}

	private void updateUI() {
		// 修复一个bug，当所有城市被删除之后，再进来不刷新界面
		if (mFragmentAdapter.getCount() == 0) {
			mFragmentAdapter = new WeatherPagerAdapter(this);
			mMainViewPager.setAdapter(mFragmentAdapter);
			mCirclePageIndicator.setViewPager(mMainViewPager);
			mCirclePageIndicator.setOnPageChangeListener(this);
		}
		mFragmentAdapter.addAllItems(mTmpCities);
		L.i("MainActivity updateUI...");
		mDrawerAdapter.setData(mTmpCities);
		mCirclePageIndicator.notifyDataSetChanged();
		// 第一次进来没有数据
		if (mTmpCities.isEmpty()) {
			visibleAddCityBtn();
			return;
		}
		if (mAddCityBtn.getVisibility() == View.VISIBLE)
			mAddCityBtn.setVisibility(View.GONE);
		if (mTmpCities.size() > 1)
			mCirclePageIndicator.setVisibility(View.VISIBLE);
		else
			mCirclePageIndicator.setVisibility(View.GONE);
		mShareBtn.setEnabled(true);

		int defaultTab = PreferenceUtils.getPrefInt(this, INSTANCESTATE_TAB, 0);
		if (defaultTab > (mTmpCities.size() - 1))// 防止手动删除城市之后出现数组越界
			defaultTab = 0;
		mMainViewPager.setCurrentItem(defaultTab, true);
		mTitleTextView.setText(mFragmentAdapter.getPageTitle(defaultTab));
		if (mTmpCities.get(defaultTab).getIsLocation())
			mLocationIV.setVisibility(View.VISIBLE);
		else
			mLocationIV.setVisibility(View.GONE);
	}

	private void invisibleSplash() {
		long loadingTime = System.currentTimeMillis() - mStartTime;// 计算一下总共花费的时间
		if (loadingTime < SHOW_TIME_MIN) {// 如果比最小显示时间还短，就延时进入MainActivity，否则直接进入
			mHandler.postDelayed(splashGone, SHOW_TIME_MIN - loadingTime);
		} else {
			mHandler.post(splashGone);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.sidebarButton:
			mRootLayout.openDrawer(mDrawerLayout);
			break;
		case R.id.shareButton:
			shareTo();
			break;
		case R.id.location_city_textview:
		case R.id.add_city_btn:
			startActivity(new Intent(MainActivity.this,
					ManagerCityActivity.class));
			break;
		default:
			break;
		}
	}

	private void shareTo() {
		new AsyncTask<Void, Void, File>() {
			Dialog dialog;

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				dialog = SystemUtils.getCustomeDialog(MainActivity.this,
						R.style.load_dialog, R.layout.custom_progress_dialog);
				TextView titleTxtv = (TextView) dialog
						.findViewById(R.id.dialogText);
				titleTxtv.setText(R.string.please_wait);
				dialog.show();
			}

			@Override
			protected File doInBackground(Void... params) {
				try {
					new File(getFilesDir(), "share.png").deleteOnExit();
					FileOutputStream fileOutputStream = openFileOutput(
							"share.png", 1);
					mMainView.setDrawingCacheEnabled(true);
					mMainView.getDrawingCache().compress(
							Bitmap.CompressFormat.PNG, 100, fileOutputStream);
					return new File(getFilesDir(), "share.png");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(File result) {
				super.onPostExecute(result);
				dialog.dismiss();
				if (result == null || App.mMainMap.isEmpty()) {
					Toast.makeText(MainActivity.this, R.string.share_fail,
							Toast.LENGTH_SHORT).show();
					return;
				}
				WeatherInfo info = App.mMainMap.get(mTmpCities.get(
						mMainViewPager.getCurrentItem()).getPostID());
				if (info == null || info.getRealTime() == null
						|| info.getRealTime().getAnimation_type() < 0) {
					Toast.makeText(MainActivity.this, R.string.share_fail,
							Toast.LENGTH_SHORT).show();
					return;
				}
				String time = TimeUtils.getDateTime(System.currentTimeMillis());
				String name = mFragmentAdapter.getPageTitle(
						mMainViewPager.getCurrentItem()).toString();
				String weather = info.getRealTime().getWeather_name();
				String temp = info.getRealTime().getTemp() + "°";

				String shareStr = mShareNormalStr + mAqiShareStr + mShareEndStr;
				if (info.getAqi() == null || info.getAqi().getAqi() < 0) {
					shareStr = mShareNormalStr + mShareEndStr;
					shareStr = String.format(shareStr, new Object[] { time,
							name, weather, temp });
				} else {
					shareStr = String.format(shareStr, new Object[] { time,
							name, weather, temp, info.getAqi().getAqi(),
							info.getAqi().getAqi_level(),
							info.getAqi().getPm25(),
							info.getAqi().getAqi_desc() });
				}

				Intent intent = new Intent("android.intent.action.SEND");
				intent.setType("image/*");
				intent.putExtra("sms_body", shareStr);
				intent.putExtra("android.intent.extra.TEXT", shareStr);
				intent.putExtra("android.intent.extra.STREAM",
						Uri.fromFile(result));
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(Intent.createChooser(intent, getResources()
						.getString(R.string.share_to)));
			}
		}.execute();
	}

	@Override
	public void onPageScrollStateChanged(int state) {
		// do nothing
	}

	@Override
	public void onPageScrolled(int position, float positionOffset,
			int positionOffsetPixels) {
		mPagerPosition = position;
		mPagerOffsetPixels = positionOffsetPixels;
	}

	@Override
	public void onPageSelected(int position) {
		if (position < mFragmentAdapter.getCount())
			mTitleTextView.setText(mFragmentAdapter.getPageTitle(position));
		if (position >= mTmpCities.size()) {
			mLocationIV.setVisibility(View.GONE);
			return;
		}

		City city = mTmpCities.get(position);
		if (city != null && city.getIsLocation())
			mLocationIV.setVisibility(View.VISIBLE);
		else
			mLocationIV.setVisibility(View.GONE);
	}


//	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
//	private void changeBlurImageViewAlpha(float slideOffset) {
//		if(slideOffset <= 0){
//			mBlurImageView.setImageBitmap(null);
//			mBlurImageView.setVisibility(View.GONE);
//			return;
//		}
//		if (mBlurImageView.getVisibility() != View.VISIBLE) {
//			setBlurImage();
//		}
//		mBlurImageView.setAlpha(slideOffset);
//	}

//	private void setBlurImage() {
//		mBlurImageView.setImageBitmap(null);
//		mBlurImageView.setVisibility(View.VISIBLE);
//		// do the downscaling for faster processing
//		long beginBlur = System.currentTimeMillis();
//		Bitmap downScaled = BitmapUtils.drawViewToBitmap(mMainView,
//				mMainView.getWidth(), mMainView.getHeight(), 10);
//		// apply the blur using the renderscript
//		FrostedGlassUtil.getInstance().stackBlur(downScaled, 4);
////		FrostedGlassUtil.getInstance().boxBlur(downScaled, 4);
////		FrostedGlassUtil.getInstance().colorWaterPaint(downScaled, 4);
////		FrostedGlassUtil.getInstance().oilPaint(downScaled, 4);
//		long engBlur = System.currentTimeMillis();
//		L.i("stackBlur cost " + (engBlur - beginBlur) + "ms");
//		mBlurImageView.setImageBitmap(downScaled);
//	}

	private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Object item = mDrawerAdapter.getItem(position);
			if (item instanceof Category) {
				return;
			}
			onMenuItemClicked(position, (Item) item);
		}
	};

	protected void onMenuItemClicked(int position, Item item) {
		mRootLayout.closeDrawer(mDrawerLayout); // 关闭drawer
		switch (item.mId) {
		case Item.INFINITE_ID:
			startActivity(new Intent(MainActivity.this,
					ManagerCityActivity.class));
			break;
		case Item.FEEDBACK_ID:
			startActivity(new Intent(MainActivity.this, FeedBackActivity.class));
			break;
		case Item.ABOUT_ID:
			startActivity(new Intent(MainActivity.this, AboutActivity.class));
			break;

		default:
			if (mMainViewPager.getCurrentItem() != item.mId)
				mMainViewPager.setCurrentItem(item.mId);
			break;
		}
	}

//	private ShowcaseView mShowcaseView;
//	private int counter = 0;

	// 进入下一个Activity
	Runnable splashGone = new Runnable() {

		@Override
		public void run() {
			if (mSplashView.getVisibility() != View.VISIBLE)
				return;
			Animation anim = AnimationUtils.loadAnimation(MainActivity.this,
					R.anim.push_right_out);
			anim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					mSplashView.setVisibility(View.GONE);
//					if (PreferenceUtils.getPrefBoolean(MainActivity.this,
//							"showcase", true)) {
//						mShowcaseView = new ShowcaseView.Builder(
//								MainActivity.this)
//								.setTarget(
//										new ViewTarget(
//												findViewById(R.id.sidebarButton)))
//								.setContentTitle("点击查看更多设置")
//								.setOnClickListener(showcaseOnClick).build();
//						mShowcaseView.setButtonText("下一个");
//						mMainViewPager.setAlpha(0.2f);
//					}
				}
			});
			mSplashView.startAnimation(anim);
		}
	};

//	OnClickListener showcaseOnClick = new OnClickListener() {
//
//		@Override
//		public void onClick(View v) {
//			switch (counter) {
//			case 0:
//				mShowcaseView.setContentTitle("点击进入城市管理");
//				mShowcaseView.setShowcase(new ViewTarget(mTitleTextView), true);
//				break;
//			case 1:
//				mShowcaseView.setContentTitle("点击分享天气信息");
//				mShowcaseView.setShowcase(new ViewTarget(mShareBtn), true);
//				break;
//			case 2:
//				mShowcaseView.setContentTitle("向上滑动查看更多");
//				mShowcaseView.setShowcase(new ViewTarget(
//						findViewById(R.id.show_last_case)), true);
//				mShowcaseView.setButtonPosition(getLastShowCaseBtnLocation());
//				mShowcaseView.setButtonText("确定");
//				break;
//			case 3:
//				PreferenceUtils.setPrefBoolean(MainActivity.this, "showcase",
//						false);
//				mMainViewPager.setAlpha(1.0f);
//				mShowcaseView.hide();
//				break;
//			default:
//				break;
//			}
//			counter++;
//		}
//	};
//
//	private RelativeLayout.LayoutParams getLastShowCaseBtnLocation() {
//		RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(
//				ViewGroup.LayoutParams.WRAP_CONTENT,
//				ViewGroup.LayoutParams.WRAP_CONTENT);
//		lps.addRule(RelativeLayout.CENTER_IN_PARENT);
//		lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//		int margin = ((Number) (getResources().getDisplayMetrics().density * 12))
//				.intValue();
//		lps.setMargins(0, 0, margin, 0);
//		return lps;
//	}
}
