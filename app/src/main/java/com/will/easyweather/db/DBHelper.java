package com.will.easyweather.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    Context mContext;
    public static final String CREATE_USER_CITY="create table user_city ("
            +"id integer primary key autoincrement, "
            +"city_code text, "
            +"city_name text, "
            +"is_locate integer, "
            +"is_default integer, "
            +"city_order integer)";

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext=context;

    }

    @Override
    public void onCreate(SQLiteDatabase db){
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

//    @Override
//    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
//    }

}
