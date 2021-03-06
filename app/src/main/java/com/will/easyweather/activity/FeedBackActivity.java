package com.will.easyweather.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.will.easyweather.R;
import com.will.easyweather.util.T;

public class FeedBackActivity extends Activity {
	private EditText mFeedBackEt;
	private Button mSendBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed_back_view);
		((TextView) findViewById(R.id.city_title)).setText("信息反馈");
		findViewById(R.id.back_image).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mFeedBackEt = (EditText) findViewById(R.id.fee_back_edit);
		mSendBtn = (Button) findViewById(R.id.feed_back_btn);
		mSendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String content = mFeedBackEt.getText().toString();
				if (!TextUtils.isEmpty(content)) {
					Intent intent = new Intent(Intent.ACTION_SENDTO);
					intent.setType("text/plain");
					intent.putExtra(Intent.EXTRA_SUBJECT, "E天气 - 信息反馈");
					intent.putExtra(Intent.EXTRA_TEXT, content);
					intent.setData(Uri.parse("mailto:chenv1989@gmail.com"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					FeedBackActivity.this.startActivity(intent);
				} else {
					T.showShort(FeedBackActivity.this, "亲,多说几句嘛!么么哒！");
				}
			}
		});
	}
}
