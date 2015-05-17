package com.will.easyweather.view;

import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.will.easyweather.R;
import com.will.easyweather.util.L;

public class CountDownView extends FrameLayout {


	public interface OnCountDownFinishedListener {
		void onCountDownFinished();
	}

	private static final String TAG = "CAM_CountDownView";
	private static final int SET_TIMER_TEXT = 1;
	private TextView mRemainingSecondsView;
	private int mRemainingSecs = 0;
	private OnCountDownFinishedListener mListener;
	private Animation mCountDownAnim;
//	private SoundPool mSoundPool;
//	private int mBeepTwice;
//	private int mBeepOnce;
//	private boolean mPlaySound;
	private final Handler mHandler = new MainHandler();

	public CountDownView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCountDownAnim = AnimationUtils.loadAnimation(context,
				R.anim.count_down_exit);
		// Load the beeps
//		mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
//		mBeepOnce = mSoundPool.load(context, R.raw.beep_once, 1);
//		mBeepTwice = mSoundPool.load(context, R.raw.beep_twice, 1);
	}

	public boolean isCountingDown() {
		return mRemainingSecs > 0;
	};

	private void remainingSecondsChanged(int newVal) {

		mRemainingSecs = newVal;

		//如果倒数到0了，回调 onCountDownFinished
		if (newVal == 0) {
			setVisibility(View.INVISIBLE);
			if(mListener != null)
			mListener.onCountDownFinished();
		} else {

			//显示更新后的数字，取消上一个未播放完的动画，重新开始新的动画，并在一秒后发送更新消息
			Locale locale = getResources().getConfiguration().locale;
			String localizedValue = String.format(locale, "%d", newVal);
			mRemainingSecondsView.setText(localizedValue);
			// Fade-out animation
			mCountDownAnim.reset();
			mRemainingSecondsView.clearAnimation();
			mRemainingSecondsView.startAnimation(mCountDownAnim);

			// Play sound effect for the last 3 seconds of the countdown
//			if (mPlaySound) {
//				if (newVal == 1) {
//					mSoundPool.play(mBeepTwice, 1.0f, 1.0f, 0, 0, 1.0f);
//				} else if (newVal <= 3) {
//					mSoundPool.play(mBeepOnce, 1.0f, 1.0f, 0, 0, 1.0f);
//				}
//			}

			//handler收到消息让计数器减1，然后再次调用 remainingSecondsChanged
			mHandler.sendEmptyMessageDelayed(SET_TIMER_TEXT, 1000);
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mRemainingSecondsView = (TextView) findViewById(R.id.remaining_seconds);
	}

	public void setCountDownFinishedListener(
			OnCountDownFinishedListener listener) {
		mListener = listener;
	}

	public void startCountDown(int sec) {
		if (sec <= 0) {
			L.i(TAG, "Invalid input for countdown timer: " + sec + " seconds");
			return;
		}
		setVisibility(View.VISIBLE);
		remainingSecondsChanged(sec);
	}

	public void cancelCountDown() {
		if (mRemainingSecs > 0) {
			mRemainingSecs = 0;
			mHandler.removeMessages(SET_TIMER_TEXT);
			setVisibility(View.INVISIBLE);
		}
	}

	private class MainHandler extends Handler {
		@Override
		public void handleMessage(Message message) {
			if (message.what == SET_TIMER_TEXT) {
				remainingSecondsChanged(mRemainingSecs - 1);
			}
		}
	}
}