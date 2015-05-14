package com.will.easyweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.will.easyweather.R;
import com.will.easyweather.bean.Category;
import com.will.easyweather.bean.City;
import com.will.easyweather.bean.Item;

import java.util.ArrayList;
import java.util.List;

public class DrawerListAdapter extends BaseAdapter {
	private static final int ITEM_TYPE = 0;
	private static final int CATEGORY_TYPE = 1;
	private List<Object> mItems;
	private LayoutInflater mLayoutInflater;

	public DrawerListAdapter(Context context) {
		mItems = new ArrayList<Object>();
		mLayoutInflater = LayoutInflater.from(context);
	}

	public void setData(List<City> tmpCities) {
		mItems.clear();

		mItems.add(new Category("城市管理"));
		mItems.add(new Item(Item.INFINITE_ID, "编辑地点", R.drawable.edit_location));

		for (int i = 0; i < tmpCities.size(); i++)
			if (tmpCities.get(i).getIsLocation()) {
				mItems.add(new Item(i, tmpCities.get(i).getName(), R.drawable.auto_locate));
			} else {
				mItems.add(new Item(i, tmpCities.get(i).getName(), R.drawable.location));
			}

		mItems.add(new Category("工具"));
		mItems.add(new Item(Item.FEEDBACK_ID, "意见与建议",R.drawable.send_feedback));
		mItems.add(new Item(Item.ABOUT_ID, "关于",R.drawable.about));
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mItems.size();
	}

	@Override
	public Object getItem(int position) {
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position) instanceof Item ? ITEM_TYPE : CATEGORY_TYPE;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position) instanceof Item;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		int type = getItemViewType(position);

		switch (type) {
			case CATEGORY_TYPE:
				ViewHolder1 viewHolder1;
				if (convertView == null) {
					convertView = mLayoutInflater.inflate(R.layout.drawer_list_item_category, parent, false);
					viewHolder1 = new ViewHolder1();
					viewHolder1.categoryTitleView = (TextView) convertView.findViewById(R.id.category_title);
					convertView.setTag(R.drawable.ic_launcher + type,viewHolder1);
				} else {
					viewHolder1 = (ViewHolder1) convertView.getTag(R.drawable.ic_launcher + type);
				}
				Category category = (Category) getItem(position);
				viewHolder1.categoryTitleView.setText(category.mTitleStr);
				break;

			case ITEM_TYPE:
				ViewHolder2 viewHolder2;
				if (convertView == null) {
					convertView = mLayoutInflater.inflate(R.layout.drawer_list_item_item, parent, false);
					viewHolder2 = new ViewHolder2();
					viewHolder2.iconView = (ImageView) convertView.findViewById(R.id.item_icon);
					viewHolder2.cityView = (TextView) convertView.findViewById(R.id.city_name);
					convertView.setTag(R.drawable.ic_launcher + type,viewHolder2);
				} else {
					viewHolder2 = (ViewHolder2) convertView.getTag(R.drawable.ic_launcher + type);
				}
				Item item = (Item) getItem(position);
				viewHolder2.cityView.setText(item.mTitleStr);
				viewHolder2.iconView.setImageResource(item.mIconRes);
				break;

		}
		return convertView;

	}

	private static final class ViewHolder1 {
		TextView categoryTitleView;
	}

	private static final class ViewHolder2 {
		ImageView iconView;
		TextView cityView;

	}
}
