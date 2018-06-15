package com.vkyoungcn.smartdevices.yomemory.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vkyoungcn.smartdevices.yomemory.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by VkYoung16 on 2018/3/26 0026.
 */

public class YoMemoryDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "YoMemory-DbHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YoMemory.db";
    private volatile static YoMemoryDbHelper sYoMemoryDbHelper = null;
    private SQLiteDatabase mSQLiteDatabase = null;

    private Context context = null;

    public static final String DEFAULT_ITEM_SUFFIX = "default13531";

    /* 建表语句
     * 初次运行时创建的表：Mission、Group、LearningLogs、Item_default13531；
     * 各任务特有Items表在添加具体任务时创建；
     * */
    public static final String SQL_CREATE_MISSION =
            "CREATE TABLE " + YoMemoryContract.Mission.TABLE_NAME + " (" +
                    YoMemoryContract.Mission._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.Mission.COLUMN_NAME + " TEXT, "+
                    YoMemoryContract.Mission.COLUMN_TABLE_ITEM_SUFFIX + " TEXT, "+
                    YoMemoryContract.Mission.COLUMN_STAR + " INTEGER, "+
                    YoMemoryContract.Mission.COLUMN_DESCRIPTION + " TEXT)";


    public static final String SQL_CREATE_GROUP =
            "CREATE TABLE " + YoMemoryContract.Group.TABLE_NAME + " (" +
                    YoMemoryContract.Group._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.Group.COLUMN_DESCRIPTION + " TEXT, "+
                    YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG + " INTEGER, "+
                    YoMemoryContract.Group.COLUMN_MISSION_ID + " INTEGER REFERENCES "+
                    YoMemoryContract.Mission.TABLE_NAME+"("+ YoMemoryContract.Mission._ID+") " +
                    "ON DELETE CASCADE)"; //外键采用级联删除

    public static final String SQL_CREATE_LEARNING_LOGS =
            "CREATE TABLE " + YoMemoryContract.LearningLogs.TABLE_NAME + " (" +
                    YoMemoryContract.LearningLogs._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG + " INTEGER, "+
                    YoMemoryContract.LearningLogs.COLUMN_GROUP_ID + " INTEGER REFERENCES "+
                    YoMemoryContract.Group.TABLE_NAME+"("+ YoMemoryContract.Group._ID+") " +
                    "ON DELETE CASCADE)"; //外键采用级联删除



    /*以下两种表需要根据具体的任务id创建，需动态生成建表语句*/
    /* 根据任务设定的尾缀创建具体的任务项目表，所需语句*/
    public String getSqlCreateItemWithSuffix(String suffix){
        return "CREATE TABLE " +
                YoMemoryContract.ItemBasic.TABLE_NAME + suffix+" (" +
                YoMemoryContract.ItemBasic._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                YoMemoryContract.ItemBasic.COLUMN_NAME + " TEXT, " +
                YoMemoryContract.ItemBasic.COLUMN_PHONETIC + " TEXT, " +
                YoMemoryContract.ItemBasic.COLUMN_TRANSLATIONS + " TEXT, " +

                YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE + " BOOLEAN" +
                YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED + " BOOLEAN" +
                YoMemoryContract.ItemBasic.COLUMN_PRIORITY + " INTEGER, " +
                YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES + " INTEGER, " +
                YoMemoryContract.ItemBasic.COLUMN_GROUP_ID + " INTEGER REFERENCES "+
                YoMemoryContract.Group.TABLE_NAME+"("+ YoMemoryContract.Group._ID+"))"; //外键无约束
        //当Group表中删除分组时，需由程序负责所属Items的归属归零；DB似乎没有直接适用的外键约束规则。
    }


    private static final String SQL_DROP_MISSION =
            "DROP TABLE IF EXISTS " +  YoMemoryContract.Mission.TABLE_NAME;
    private static final String SQL_DROP_GROUP =
            "DROP TABLE IF EXISTS " + YoMemoryContract.Group.TABLE_NAME;
    private static final String SQL_DROP_LEARNING_LOGS =
            "DROP TABLE IF EXISTS " + YoMemoryContract.LearningLogs.TABLE_NAME;

    /*各Items表的删除语句需动态生成*/
    public String getSqlDropItemWithSuffix(String suffix){
        return "DROP TABLE IF EXISTS " +  YoMemoryContract.ItemBasic.TABLE_NAME + suffix;
    }


    private YoMemoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;

        getWritableDatabaseIfClosedOrNull();
    }

    //DCL模式单例，因为静态内部类模式不支持传参
    public static YoMemoryDbHelper getInstance(Context context){
        if(sYoMemoryDbHelper == null){
            synchronized (YoMemoryDbHelper.class){
                if(sYoMemoryDbHelper == null){
                    sYoMemoryDbHelper = new YoMemoryDbHelper(context);
                }
            }
        }
        return sYoMemoryDbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MISSION);
        db.execSQL(SQL_CREATE_GROUP);
        db.execSQL(SQL_CREATE_LEARNING_LOGS);
        //默认的Item表在以下方法中建立，随后导入初始数据。
        dataInitialization(db);
    }

    /*
    * 在本方法中，
    * ①建立Item_default13531表；
    * ②为Mission表增添记录；
    * ③ 为Item_default13531表添加全部初始记录。
    * */
    private void dataInitialization(SQLiteDatabase db){
        db.execSQL(getSqlCreateItemWithSuffix(DEFAULT_ITEM_SUFFIX));

        //向Mission表增加默认记录
        Mission defaultMission  = new Mission("EnglishWords13531","螺旋式背单词",DEFAULT_ITEM_SUFFIX,1);
        createMission(db,defaultMission);//传入db是避免调用getDataBase，后者（会调用onCreate）导致递归调用错误

        //Item_default13531表数据导入
        importToItemDefaultFromCSV("EbbingWords13531.csv",db);
    }

    /*
     * 从csv文件向默认Item表导入数据；
     * 要求csv文件位于Assets目录、且为UTF-8编码。
     * csv文件首行要直接是数据，不能是列名。
     * */
    private void importToItemDefaultFromCSV(String csvFileName, SQLiteDatabase db){
        String line = "";

        InputStream is = null;
        try {
            is = context.getAssets().open(csvFileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            db.beginTransaction();

            int number = 0;
            while ((line = bufferedReader.readLine()) != null) {
                String[] str = line.split(",");

                ContentValues values = new ContentValues();
                //在csv文件中，第1列是id，舍去不要；2列是单词、3列音标、4列释义字串。
                values.put(YoMemoryContract.ItemBasic.COLUMN_NAME, str[1]);
                values.put(YoMemoryContract.ItemBasic.COLUMN_PHONETIC, str[2]);//音标
                values.put(YoMemoryContract.ItemBasic.COLUMN_TRANSLATIONS, str[3]);//释义字串

                values.put(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES,0);//默认值0
                values.put(YoMemoryContract.ItemBasic.COLUMN_PRIORITY,2);//默认是2
                values.put(YoMemoryContract.ItemBasic.COLUMN_GROUP_ID,0);//默认值0
                values.put(YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE, false);//v9,补【经提取测试，log.i中输出为0,并且是数字0，如果再提取，需要匹配为 = 0】
                values.put(YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED, false);
                db.insert(YoMemoryContract.ItemBasic.TABLE_NAME + DEFAULT_ITEM_SUFFIX, null, values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        db.setTransactionSuccessful();
        db.endTransaction();

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // 使用for实现跨版本升级数据库
        for (int i = oldVersion; i < newVersion; i++) {
            switch (i) {
               /* case 4:
                示范
                    db.execSQL("DROP TABLE IF EXISTS temp_old_group");
                    db.execSQL("DROP TABLE IF EXISTS "+YoMemoryContract.GroupCrossItem.TABLE_NAME + DEFAULT_ITEM_SUFFIX);
                    break;*/

                default:
                    break;
            }
        }
    }


    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        for (int i = oldVersion; i > newVersion; i--) {
            switch (i) {
                //没写。。
                default:
                    break;
            }
        }

    }

    /*CRUD部分需要时再写*/
/*
    public long createMission(Mission mission){
        long l;

        getWritableDatabaseIfClosedOrNull();
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Mission.COLUMN_NAME, mission.getName());
        values.put(YoMemoryContract.Mission.COLUMN_DESCRIPTION, mission.getDescription());
        values.put(YoMemoryContract.Mission.COLUMN_TABLE_ITEM_SUFFIX, mission.getTableItem_suffix());

        l = mSQLiteDatabase.insert(YoMemoryContract.Mission.TABLE_NAME, null, values);
        closeDB();

        return l;
    }
*/


    /*
    * 此重载版本在数据库的onCreate()方法中使用，传递db以免递归调用错误。
    * */
    private void createMission(SQLiteDatabase db, Mission mission){
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Mission.COLUMN_NAME, mission.getName());
        values.put(YoMemoryContract.Mission.COLUMN_DESCRIPTION, mission.getDescription());
        values.put(YoMemoryContract.Mission.COLUMN_TABLE_ITEM_SUFFIX, mission.getTableItem_suffix());

        db.insert(YoMemoryContract.Mission.TABLE_NAME, null, values);
        closeDB();
    }

    /*public Mission getMissionById(long mission_id){
        Mission mission = new Mission();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Mission.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Mission._ID+" = "+mission_id;
        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            mission.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Mission._ID)));
            mission.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_NAME)));
            mission.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_DESCRIPTION)));
            mission.setTableItem_suffix(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_TABLE_ITEM_SUFFIX)));
        }else{
            return null;
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return mission;
    }*/

    public List<Mission> getAllMissions(){
        List<Mission> missions = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Mission.TABLE_NAME;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                Mission mission = new Mission();
                mission.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Mission._ID)));
                mission.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_NAME)));
                mission.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_DESCRIPTION)));
                mission.setTableItem_suffix(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_TABLE_ITEM_SUFFIX)));

                missions.add(mission);
            }while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return missions;

    }

    /*public List<String> getAllMissionTitles(){
        List<String> missionTitles = new ArrayList<>();
        String selectQuery = "SELECT "+ YoMemoryContract.Mission.COLUMN_NAME
                +" FROM "+ YoMemoryContract.Mission.TABLE_NAME;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                missionTitles.add(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Mission.COLUMN_NAME)));
            }while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return missionTitles;

    }*/


    public List<SingleItem> getAllItemsOfMission(String tableNameSuffix){
        List<SingleItem> items = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            do {
                SingleItem item = new SingleItem();
                item.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic._ID)));
                item.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_NAME)));
                item.setPhonetic(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_PHONETIC)));
                item.setTranslations(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_TRANSLATIONS)));

                item.setChose(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE))==1);//【待测试。getString .equals(false)可用】
                item.setLearned(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED))==1);//【待测试。getString .equals(false)可用】
                item.setGroupId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_GROUP_ID)));
                item.setPriority(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_PRIORITY)));
                item.setFailedSpelling_times(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES)));

                items.add(item);
            }while (cursor.moveToNext());
        }else{
            return null;
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return items;
    }

    /*
    * 使用了sqlite聚合函数
    * */
    public float getLearnedPercentageOfMission(String tableNameSuffix){
        Log.i(TAG, "getLearnedPercentageOfMission: before.");
        float percentage = -1;
        String selectQuery = "SELECT COUNT("+YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED+")/" +
                "COUNT(*) FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
                percentage = cursor.getFloat(0);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return percentage;
    }


    public List<SingleItem> getItemsByGroupId(int groupId, String tableNameSuffix){
        Log.i(TAG, "getItemsByGroupId: before any.");
        List<SingleItem> items = new ArrayList<>();

        String selectQuery = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic.COLUMN_GROUP_ID +" = "+groupId;
        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            do {
                SingleItem item = new SingleItem();
                item.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic._ID)));
                item.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_NAME)));
                item.setPhonetic(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_PHONETIC)));
                item.setTranslations(cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_TRANSLATIONS)));

                item.setChose(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE))==1);//【待测试。getString .equals(false)可用】
                item.setLearned(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED))==1);//【待测试。getString .equals(false)可用】
                item.setGroupId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_GROUP_ID)));
                item.setPriority(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_PRIORITY)));
                item.setFailedSpelling_times(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES)));

                items.add(item);
            }while (cursor.moveToNext());
            Log.i(TAG, "getItemsByGroupId: cursor ok.");
        }else{
            return null;
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return items;
    }

//【2018.6.8修改到此。D2】

    /*
    * 建立新分组的时候，必须将所属items置为已抽取；所以需要tableSuffix
    * */
    public long createGroup(DBGroup dbGroup, String tableSuffix){
        long l;
        getWritableDatabaseIfClosedOrNull();

        mSQLiteDatabase.beginTransaction();
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Group.COLUMN_DESCRIPTION, dbGroup.getDescription());
        values.put(YoMemoryContract.Group.COLUMN_DOUBLE_KILL, dbGroup.isDoubleKill());
        values.put(YoMemoryContract.Group.COLUMN_TRIPLE_KILL, dbGroup.isTripleKill());
        values.put(YoMemoryContract.Group.COLUMN_INIT_LEARNING_LONG, dbGroup.getInitLearningLong());
        values.put(YoMemoryContract.Group.COLUMN_MISSION_ID, dbGroup.getMission_id());
        values.put(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_30, dbGroup.getRePickingTimes_30m());
        values.put(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_EARLY, dbGroup.getEarlyTimeRePickingTimes());
        values.put(YoMemoryContract.Group.COLUMN_SUB_ITEM_IDS, dbGroup.getSubItemIdsStr());

        l = mSQLiteDatabase.insert(YoMemoryContract.Group.TABLE_NAME, null, values);

        setItemsChose(tableSuffix, dbGroup.getSubItemIdsStr());
        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();

        closeDB();
        return l;
    }

    public List<DBGroup> getAllGroupsByMissionId(int missionsId){
        List<DBGroup> groups = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Group.COLUMN_MISSION_ID+" = "+missionsId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                DBGroup group = new DBGroup();
                group.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group._ID)));
                group.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DESCRIPTION)));
                group.setSubItemIdsStr(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_SUB_ITEM_IDS)));
                group.setInitLearningLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_INIT_LEARNING_LONG)));
                group.setMission_id(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_MISSION_ID)));
                group.setRePickingTimes_30m(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_30)));
                group.setEarlyTimeRePickingTimes(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_EARLY)));
                group.setDoubleKill(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DOUBLE_KILL))==1);
                group.setTripleKill(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_TRIPLE_KILL))==1);

                groups.add(group);
            }while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return groups;
    }

    public DBGroup getGroupById(int groupId){
        DBGroup group = new DBGroup();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Group._ID+" = "+groupId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            group.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group._ID)));
            group.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DESCRIPTION)));
            group.setSubItemIdsStr(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_SUB_ITEM_IDS)));
            group.setInitLearningLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_INIT_LEARNING_LONG)));
            group.setMission_id(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_MISSION_ID)));
            group.setRePickingTimes_30m(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_30)));
            group.setEarlyTimeRePickingTimes(cursor.getShort(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_RE_PICKING_TIMES_EARLY)));
            group.setDoubleKill(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DOUBLE_KILL))==1);
            group.setTripleKill(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_TRIPLE_KILL))==1);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return group;
    }


    /*
    * 更新指定分组的Logs学习记录。
    * */
    public long updateLogOfGroupById(int groupId,String newFullyLogs){
        long lines;
        getWritableDatabaseIfClosedOrNull();
        ContentValues values = new ContentValues();
        values.put(YoMemoryContract.Group.COLUMN_GROUP_LOGS,newFullyLogs);
        lines = mSQLiteDatabase.update(YoMemoryContract.Group.TABLE_NAME,values,
                YoMemoryContract.Group._ID+"=?",new String[]{String.valueOf(groupId)});
        closeDB();
        return lines;


    }

    public int updateExtraLearningOneAsTrue(int groupId){
        int lines;
        getWritableDatabaseIfClosedOrNull();

        ContentValues values = new ContentValues();
        values.put(YoMemoryContract.Group.COLUMN_EXTRA_1H,true);
        lines = mSQLiteDatabase.update(YoMemoryContract.Group.TABLE_NAME,values,
                YoMemoryContract.Group._ID+"=?",new String[]{String.valueOf(groupId)});
        closeDB();
        return lines;
    }

    public int updateExtraLearning24HourAsAddOne(int groupId){
        int lines;
        getWritableDatabaseIfClosedOrNull();

        short oldTimes = getExtraLearningTimes(groupId);

        ContentValues values = new ContentValues();
        values.put(YoMemoryContract.Group.COLUMN_EXTRA_24H,oldTimes+1);
        lines = mSQLiteDatabase.update(YoMemoryContract.Group.TABLE_NAME,values,
                YoMemoryContract.Group._ID+"=?",new String[]{String.valueOf(groupId)});
        closeDB();
        return lines;
    }

    private short getExtraLearningTimes(int groupId){
        short times = 0;
        String selectQuery = "SELECT "+ YoMemoryContract.Group.COLUMN_EXTRA_24H+" FROM "
                + YoMemoryContract.Group.TABLE_NAME+" WHERE "+ YoMemoryContract.Group._ID+" = "
                +groupId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            times = cursor.getShort(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_EXTRA_24H));
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return times;

    }


    public void removeGroupById(int groupId, String subItemIdsStr, String itemTableNameSuffix){
        getWritableDatabaseIfClosedOrNull();
        String deleteSingleGroupSql = "DELETE FROM "+ YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group._ID+" = "+groupId;

        mSQLiteDatabase.beginTransaction();

        mSQLiteDatabase.execSQL(deleteSingleGroupSql);

        setItemsUnChose(itemTableNameSuffix,subItemIdsStr);

        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();
        closeDB();
    }


    /*
    * Timer中每隔1分钟检查一次当前mission的各group，符合条件的会调用此函数设为废弃。
    * */
   /* public void setGroupObsoleted(int groupId){
        getWritableDatabaseIfClosedOrNull();
        String setGroupObsoletedSql = "UPDATE "+YoMemoryContract.Group.TABLE_NAME+
                " SET "+YoMemoryContract.Group.COLUMN_IS_OBSOLETED+
                " = 'true' WHERE "+YoMemoryContract.Group._ID+" = "+groupId;
        mSQLiteDatabase.execSQL(setGroupObsoletedSql);
        closeDB();
    }*/

    /*该方法将位于其他方法开启的事务内，因而不能有关DB操作*/
    private void setItemsUnChose(String tableSuffix, String itemIds){
        getWritableDatabaseIfClosedOrNull();

        if(itemIds==null||itemIds.isEmpty()){
            return;
        }
        String[] str =  itemIds.split(";");
        StringBuilder sbr = new StringBuilder();
        sbr.append("( ");
        for (String s: str) {
            sbr.append(s);
            sbr.append(", ");
        }
        sbr.deleteCharAt(sbr.length()-2);
        sbr.append(")");

        String itemsGiveBackSql = "UPDATE "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+
                " SET "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +
                " = 0 WHERE "+ YoMemoryContract.ItemBasic._ID+
                " IN "+sbr.toString();

        mSQLiteDatabase.execSQL(itemsGiveBackSql);
    }

    /*
    * 设为已抽取，该方法将位于其他方法开启的事务内，因而不能有关DB操作。
    * */
    private void setItemsChose(String tableSuffix, String itemIds){
        getWritableDatabaseIfClosedOrNull();

        if(itemIds==null||itemIds.isEmpty()){
            return;
        }
        String[] str =  itemIds.split(";");
        StringBuilder sbr = new StringBuilder();
        sbr.append("( ");
        for (String s: str) {
            sbr.append(s);
            sbr.append(", ");
        }
        sbr.deleteCharAt(sbr.length()-2);//【错误记录，这里原来误写成str，错得很隐蔽】
        sbr.append(")");

        String itemsChoseSql = "UPDATE "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+
                " SET "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +
                " = 1 WHERE "+ YoMemoryContract.ItemBasic._ID+
                " IN "+sbr.toString();

        mSQLiteDatabase.execSQL(itemsChoseSql);
    }

    /*
    * 按顺序选取Item中前n项记录（的id），提供给任务组的生成。
    * 要求是未抽取的
    * 相应记录要置已抽取（标记为抽取的操作改由创建Group时进行，作为一个整体事务，以免取回itemId后创建失败。）
    * 未能选到任何结果时，返回null；
    * */
    public List<Integer> getCertainAmountItemIdsOrderly(int amount, String tableNameSuffix){
        List<Integer> ids = new ArrayList<>();
        String selectQueryInner = "SELECT "+ YoMemoryContract.ItemBasic._ID
                +" FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" =  0  OR "
                + YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" IS NULL";
        String selectQueryOuter = "SELECT "+ YoMemoryContract.ItemBasic._ID
                +" FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic._ID+" IN ( "+selectQueryInner+" ) LIMIT "+amount;


        getReadableDatabaseIfClosedOrNull();
//        mSQLiteDatabase.beginTransaction();

        Cursor cursor = mSQLiteDatabase.rawQuery(selectQueryOuter, null);

        if(cursor.moveToFirst()){
            do{
                int i = cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic._ID));
                ids.add(i);
            }while (cursor.moveToNext());

        }//【即使无结果也不能返回null；返回长为0的list即可】

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return ids;
    }


    /*
     * 随机选取Item中前n项记录（的id），提供给任务组的生成。item置为已抽取的操作在建组时进行。
     * 未能选到任何结果时，返回null；
     * 【待】我怎么记得涉及到SQLite的ID的项目都需用long啊？！
     * */
    public List<Integer> getCertainAmountItemIdsRandomly(int amount, String tableNameSuffix){
        List<Integer> ids = new ArrayList<>();
        String selectQuery = "SELECT "+ YoMemoryContract.ItemBasic._ID
                + " FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" = 0 OR "
                + YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" IS NULL "
                + " ORDER BY RANDOM() LIMIT "+amount;

        getReadableDatabaseIfClosedOrNull();

        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                ids.add(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.ItemBasic._ID)));
            }while (cursor.moveToNext());

        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return ids;
    }


    public String getSingleItemNameById(long itemId, String suffix){
        String itemName;
        String selectQuery = "SELECT "+ YoMemoryContract.ItemBasic.COLUMN_NAME+
                " FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+suffix+
                " WHERE "+ YoMemoryContract.ItemBasic._ID+" = "+itemId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            itemName = cursor.getString(cursor.getColumnIndex(YoMemoryContract.ItemBasic.COLUMN_NAME));

        }else{
            return null;
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();
        return itemName;

    }

    private String getStingSubIdsWithParenthesisForWhereSql(String subItemsIdsStr){
        if(subItemsIdsStr==null||subItemsIdsStr.isEmpty()){
            return "()";
        }
        String[] strings =  subItemsIdsStr.split(";");
        StringBuilder builder = new StringBuilder();
        builder.append("( ");
        for (String s: strings) {
            builder.append(s);
            builder.append(", ");
        }
        builder.deleteCharAt(builder.length()-2);
        builder.append(")");
        return builder.toString();

    }

    private void getWritableDatabaseIfClosedOrNull(){
        if(mSQLiteDatabase==null || !mSQLiteDatabase.isOpen()) {
            mSQLiteDatabase = this.getWritableDatabase();
        }/*else if (mSQLiteDatabase.isReadOnly()){
            //只读的不行，先关，再开成可写的。
            try{
                mSQLiteDatabase.close();
            }catch(SQLException e){
                e.printStackTrace();
            }
            mSQLiteDatabase = this.getWritableDatabase();
        }*/
    }

    private void getReadableDatabaseIfClosedOrNull(){
        if(mSQLiteDatabase==null || !mSQLiteDatabase.isOpen()) {
            mSQLiteDatabase = this.getReadableDatabase();
            //如果是可写DB，也能用，不再开关切换。
        }
    }

    //关数据库
    private void closeDB(){
        if(mSQLiteDatabase != null && mSQLiteDatabase.isOpen()){
            try{
                mSQLiteDatabase.close();
            }catch(SQLException e){
                e.printStackTrace();
            }
        } // end if
    }

}
