package com.vkyoungcn.smartdevices.yomemory.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vkyoungcn.smartdevices.yomemory.LogoPageActivity;
import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class YoMemoryDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "YoMemoryDbHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "YoMemory.db";

    private volatile static YoMemoryDbHelper sYoMemoryDbHelper = null;

    //类内所有的DB操作均采用以下引用进行。通过同一的获取和关闭方法进行获取、关闭。
    private SQLiteDatabase mSQLiteDatabase;

//    private Context context = null;
    private static final String DEFAULT_ITEM_SUFFIX = "default13531";

    /* 建表语句*/
    // 初次运行时创建的表：Missions、Group、LearningLogs、Item_default13531；
    //任务表
    private static final String SQL_CREATE_MISSIONS =
            "CREATE TABLE " + YoMemoryContract.Missions.TABLE_NAME + " (" +
                    YoMemoryContract.Missions._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.Missions.COLUMN_TITLE + " TEXT, "+
                    YoMemoryContract.Missions.COLUMN_DESCRIPTION + " TEXT, "+
                    YoMemoryContract.Missions.COLUMN_USING_CROSS_TABLE + " BOOLEAN, "+
                    YoMemoryContract.Missions.COLUMN_SOURCE_TABLE + " TEXT, "+
                    YoMemoryContract.Missions.COLUMN_STAR + " INTEGER, "+
                    YoMemoryContract.Missions.COLUMN_IS_SHOWING + " BOOLEAN)";

    //分组表
    private static final String SQL_CREATE_GROUP =
            "CREATE TABLE " + YoMemoryContract.Groups.TABLE_NAME + " (" +
                    YoMemoryContract.Groups._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.Groups.COLUMN_DESCRIPTION + " TEXT, "+
                    YoMemoryContract.Groups.COLUMN_MID + " INTEGER, "+
                    YoMemoryContract.Groups.COLUMN_CREATE_TIME + " INTEGER, "+
                    "FOREIGN KEY("+YoMemoryContract.Groups.COLUMN_MID +") REFERENCES "+
                    YoMemoryContract.Missions.TABLE_NAME+"("+ YoMemoryContract.Missions._ID+") " +
                    "ON DELETE CASCADE)"; //外键采用级联删除
    //日志表
    private static final String SQL_CREATE_LEARNING_LOGS =
            "CREATE TABLE " + YoMemoryContract.LearningLogs.TABLE_NAME + " (" +
                    YoMemoryContract.LearningLogs._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG + " INTEGER, "+
                    YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE + " BOOLEAN, "+
                    YoMemoryContract.LearningLogs.COLUMN_GROUP_ID + " INTEGER,"+
                    "FOREIGN KEY ("+YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+
                    ") REFERENCES "+
                    YoMemoryContract.Groups.TABLE_NAME+"("+ YoMemoryContract.Groups._ID+") " +
                    "ON DELETE CASCADE)"; //外键采用级联删除

    /* 内置英语5W词汇资源库的items表*/
    public static final String SQL_CREATE_ENGLISH_COMMON_ITEMS =
            "CREATE TABLE " + YoMemoryContract.EnglishCommonItems.TABLE_NAME + " (" +
                    YoMemoryContract.EnglishCommonItems._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    YoMemoryContract.EnglishCommonItems.COLUMN_WORD + " TEXT, "+
                    YoMemoryContract.EnglishCommonItems.COLUMN_PHONETIC + " TEXT, "+
                    YoMemoryContract.EnglishCommonItems.COLUMN_DEFINITION + " BOOLEAN, "+
                    YoMemoryContract.EnglishCommonItems.COLUMN_TRANSLATION + " TEXT, "+
                    YoMemoryContract.EnglishCommonItems.COLUMN_BNC + " INTEGER, "+
                    YoMemoryContract.EnglishCommonItems.COLUMN_FRQ + " INTEGER )";



    /* 删表语句*/
    private static final String SQL_DROP_MISSIONS =
            "DROP TABLE IF EXISTS " +  YoMemoryContract.Missions.TABLE_NAME;
    private static final String SQL_DROP_GROUPS =
            "DROP TABLE IF EXISTS " + YoMemoryContract.Groups.TABLE_NAME;
    private static final String SQL_DROP_ENGLISH_COMMON_ITEMS =
            "DROP TABLE IF EXISTS " + YoMemoryContract.EnglishCommonItems.TABLE_NAME;
    private static final String SQL_DROP_LEARNING_LOGS =
            "DROP TABLE IF EXISTS " + YoMemoryContract.LearningLogs.TABLE_NAME;
    private static final String SQL_DROP_ENGLISH_CROSS =
            "DROP TABLE IF EXISTS " + YoMemoryContract.EnglishCross.TABLE_NAME;


    //构造器
    private YoMemoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
//        this.context = context;

        getWritableDatabaseIfClosedOrNull();
    }


    //DCL模式单例获取方法
    // 因为静态内部类模式不支持传参故而采用DCL模式
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

    /* 必要的覆写方法 */
    @Override
    public void onCreate(SQLiteDatabase db) {
        //根据API，本方法在初次使用数据库时（比如初次请求生成mSQLiteDatabase）自动调用
        //此情景下，执行如下任务.
//        Log.i(TAG, "onCreate: be");
        //建表
        db.execSQL(SQL_CREATE_MISSIONS);
        db.execSQL(SQL_CREATE_GROUP);
        db.execSQL(SQL_CREATE_LEARNING_LOGS);

//    只进行建表。导入数据的工作稍后再行处理。（这样可以避免在本类中持有context字段）
//    由于本类对自身的持有是静态的，因而编译器提示在静态字段中持有context实例导致……忘了

//        dataInitialization(db);
    }

    /*
    * 在本方法中，
    * ①建立Item_default13531表；
    * ②为Mission表增添记录；
    * ③ 为Item_default13531表添加全部初始记录。
    * */
    /*private void dataInitialization(SQLiteDatabase db){
        db.execSQL(getSqlCreateItemWithSuffix(DEFAULT_ITEM_SUFFIX));

        //向Mission表增加默认记录
        String detail_13531 = context.getResources().getString(R.string.introduction_Mission13531);
        Mission defaultMission  = new Mission("EnglishWords13531","螺旋式背单词",detail_13531,DEFAULT_ITEM_SUFFIX,2);
        createMission(db,defaultMission);//传入db是避免调用getDataBase，后者（会调用onCreate）导致递归调用错误

        //Item_default13531表数据导入
        importToItemDefaultFromCSV("EbbingWords13531.csv",db);
    }*/

    /*
     * 从csv文件向默认Item表导入数据；
     * 要求csv文件位于Assets目录、且为UTF-8编码。
     * csv文件首行要直接是数据，不能是列名。
     * */
  /*  private void importToItemDefaultFromCSV(String csvFileName, SQLiteDatabase db){
        String line = "";

        InputStream is = null;
        try {
            is = context.getAssets().open(csvFileName);
            int totalBytes = is.available();
            boolean reported_1 = false,reported_2=false,reported_3=false,reported_4 = false;
//            boolean reported_5=false;
            //Java可以一行声明多个，但是需要分布赋值（另，布尔的默认初始值可能本身就是false）

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            db.beginTransaction();

            int readBytes = 0;
            while ((line = bufferedReader.readLine()) != null) {
                readBytes+=line.length();//大体估算吧，我不知道要不要+1（把结束符号算上）
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

                if(!reported_1 && readBytes>(totalBytes/10)){
                    //读取了10%了，通知(只能通知一次)
                    reported_1 = true;
                    //【以下这种操作要求本方法所在的“DB初始填充逻辑”只能在LogoPageActivity中运行。】
                    ((LogoPageActivity)context).setNewPercentNum(20);//设置为一个稍大的数字。
                }else if(!reported_2 && readBytes>(totalBytes/4)){
                    reported_2 =true;
                    ((LogoPageActivity)context).setNewPercentNum(45);
                }else if(!reported_3 && readBytes>(totalBytes/2)){
                    reported_3 =true;
                    ((LogoPageActivity)context).setNewPercentNum(90);
                }else if(!reported_4 && readBytes>((totalBytes/10)*8)){
                    //如果在8/10时通知，来不及显示。
                    reported_4 =true;
                    ((LogoPageActivity)context).setNewPercentNum(100);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        db.setTransactionSuccessful();
        db.endTransaction();

    }*/

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

    /* 需要一个可由外调用的“导入基础数据”的方法，待。*/


    /*CRUD部分*/
    public long createMission(Mission mission){
        long l;

        getWritableDatabaseIfClosedOrNull();
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Missions.COLUMN_NAME, mission.getName());
        values.put(YoMemoryContract.Missions.COLUMN_DESCRIPTION, mission.getSimpleDescription());
        values.put(YoMemoryContract.Missions.COLUMN_DETAIL_DESCRIPTION, mission.getDetailDescriptions());
        values.put(YoMemoryContract.Missions.COLUMN_TABLE_ITEM_SUFFIX, mission.getTableItem_suffix());
        values.put(YoMemoryContract.Missions.COLUMN_STAR,mission.getStarType());

        l = mSQLiteDatabase.insert(YoMemoryContract.Missions.TABLE_NAME, null, values);
        closeDB();

        return l;
    }


    /*
    * 此重载版本在数据库的onCreate()方法中使用，传递db以免递归调用错误。
    * */
    private void createMission(SQLiteDatabase db, Mission mission){
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Missions.COLUMN_NAME, mission.getName());
        values.put(YoMemoryContract.Missions.COLUMN_DESCRIPTION, mission.getSimpleDescription());
        values.put(YoMemoryContract.Missions.COLUMN_DETAIL_DESCRIPTION, mission.getDetailDescriptions());
        values.put(YoMemoryContract.Missions.COLUMN_TABLE_ITEM_SUFFIX, mission.getTableItem_suffix());

        db.insert(YoMemoryContract.Missions.TABLE_NAME, null, values);
        closeDB();
    }

    /*
    * 修改Mission的星标
    * 因为在页面中能获取的（修改后数据）是Rv版的，所以在此使用ArrayList<RvMission>。
     * */
    public int updateMissionStartInBatches(ArrayList<RvMission> missions,ArrayList<Integer> positions){
        int affectedRows = 0;
        getReadableDatabaseIfClosedOrNull();

        for (int i : positions) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(YoMemoryContract.Missions.COLUMN_STAR,missions.get(i).getStarType());

            affectedRows = mSQLiteDatabase.update(YoMemoryContract.Missions.TABLE_NAME,contentValues,
                    YoMemoryContract.Missions._ID+" = ? ",new String[]{String.valueOf(missions.get(i).getId())} );
        }

        closeDB();
        return affectedRows;
    }

    /* 修改单项的星标 */
    public int updateMissionStart(RvMission rvMission){
        int affectedRows = 0;
        getReadableDatabaseIfClosedOrNull();

        ContentValues contentValues = new ContentValues();
        contentValues.put(YoMemoryContract.Missions.COLUMN_STAR,rvMission.getStarType());

        affectedRows = mSQLiteDatabase.update(YoMemoryContract.Missions.TABLE_NAME,contentValues,
                YoMemoryContract.Missions._ID+" = ? ",new String[]{String.valueOf(rvMission.getId())} );
//        Log.i(TAG, "updateMissionStart: rvm.starType="+rvMission.getStarType()+", affected　rows="+affectedRows);
        /*String tempStr = "SELECT "+YoMemoryContract.Missions.COLUMN_STAR+" FROM "
                +YoMemoryContract.Missions.TABLE_NAME+" WHERE "+YoMemoryContract.Missions._ID+" = "+rvMission.getId();
        Cursor cursor =mSQLiteDatabase.rawQuery(tempStr,null);
        int startType = -1;
        if(cursor.moveToFirst()){
            startType = cursor.getInt(0);
        }
        Log.i(TAG, "updateMissionStart: startType just re-got in DB="+startType);*/

        closeDB();
        return affectedRows;
    }

    /*public Missions getMissionById(long mission_id){
        Missions mission = new Missions();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Missions.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Missions._ID+" = "+mission_id;
        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            mission.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Missions._ID)));
            mission.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_NAME)));
            mission.setSimpleDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_DESCRIPTION)));
            mission.setTableItem_suffix(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_TABLE_ITEM_SUFFIX)));
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
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Missions.TABLE_NAME;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                Mission mission = new Mission();
                mission.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Missions._ID)));
                mission.setName(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_NAME)));
                mission.setSimpleDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_DESCRIPTION)));
                mission.setDetailDescriptions(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_DETAIL_DESCRIPTION)));
                mission.setTableItem_suffix(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_TABLE_ITEM_SUFFIX)));
                mission.setStarType(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_STAR)));
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
        String selectQuery = "SELECT "+ YoMemoryContract.Missions.COLUMN_NAME
                +" FROM "+ YoMemoryContract.Missions.TABLE_NAME;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                missionTitles.add(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Missions.COLUMN_NAME)));
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

    /*
    * 一个任务的资源总量
    * */
    public int getNumOfItemsOfMission(String tableNameSuffix){
        int totalNum = 0;
        if(tableNameSuffix == null ||tableNameSuffix.isEmpty()){
            return totalNum;
        }
        String selectQuery = "SELECT COUNT(*) FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            totalNum = cursor.getInt(0);
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return totalNum;
    }

    /*
     * 一个任务的已学资源总量
     * */
    public int getLearnedNumOfItemsOfMission(String tableNameSuffix){
        int totalNum = 0;
        if(tableNameSuffix == null ||tableNameSuffix.isEmpty()){
            return totalNum;
        }
        String selectQuery = "SELECT COUNT(*) FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix+
                " WHERE "+YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED+" = 1";

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            totalNum = cursor.getInt(0);
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return totalNum;
    }

    /*
     * 一个任务的分组总量
    * */
    public int getGroupsNumOfMission(int missionId){
        int totalNum = 0;
        String selectQuery = "SELECT COUNT(*) FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " WHERE "+YoMemoryContract.Group.COLUMN_MISSION_ID+" = "+missionId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery,null);

        if(cursor.moveToFirst()){
            totalNum = cursor.getInt(0);
        }
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return totalNum;
    }



    public List<SingleItem> getAllItemsOfMission(String tableNameSuffix){
        List<SingleItem> items = new ArrayList<>();
        if(tableNameSuffix == null ||tableNameSuffix.isEmpty()){
            return items;
        }

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


    public ArrayList<SingleItem> getItemsByGroupId(int groupId, String tableNameSuffix){
//        Log.i(TAG, "getItemsByGroupId: before any.");
        ArrayList<SingleItem> items = new ArrayList<>();

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

    /*
    * 建立新分组的时候，必须将所属items置为已抽取；所以需要tableSuffix
    * 新建的分组没由学习记录，不需要向Logs表写内容。
    * 新版Group类已不再持有所含Items的id列表，因而需要额外传入
    * */
    public int createGroup(Group group, ArrayList<Integer> subItemIds, String tableSuffix){
        long l;
        getWritableDatabaseIfClosedOrNull();

        mSQLiteDatabase.beginTransaction();
        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Group.COLUMN_DESCRIPTION, group.getDescription());
        values.put(YoMemoryContract.Group.COLUMN_MISSION_ID, group.getMission_id());
        values.put(YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG, group.getSettingUptimeInLong());

        l = mSQLiteDatabase.insert(YoMemoryContract.Group.TABLE_NAME, null, values);

        //需要获取新生成记录的gid列数据
        String strGetGid = "SELECT "+YoMemoryContract.Group._ID+" FROM "+
                YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG+
                " = "+ group.getSettingUptimeInLong();
        Cursor cursor = mSQLiteDatabase.rawQuery(strGetGid, null);
        int gid = 0;
        if(cursor.moveToFirst()){
            gid  = cursor.getInt(0);
        }
//        int groupId = getGroupIdByLineWithOutOpenDb(l,tableSuffix);

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setItemsChose(tableSuffix, gid, subItemIds);
        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();


        closeDB();
        return gid;
    }

    /*
    * 建立一个容量为0的分组，用于拆分分组时先行一步生成临时分组
    * 也用在LC最后生成时先产生分组记录（从而获取可用的gid）
    * */
    public int createEmptyGroup(Group group){
        int gid = 0;
        getWritableDatabaseIfClosedOrNull();

        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.Group.COLUMN_DESCRIPTION, group.getDescription());
        values.put(YoMemoryContract.Group.COLUMN_MISSION_ID, group.getMission_id());
        values.put(YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG, group.getSettingUptimeInLong());

        mSQLiteDatabase.insert(YoMemoryContract.Group.TABLE_NAME, null, values);

        String queryNewGroupId = "SELECT "+YoMemoryContract.Group._ID+" FROM "
                +YoMemoryContract.Group.TABLE_NAME+" WHERE "
                +YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG +" = "
                + group.getSettingUptimeInLong();
        Cursor cursor = mSQLiteDatabase.rawQuery(queryNewGroupId,null);
        if(cursor.moveToFirst()){
            gid = cursor.getInt(0);
        }
        Log.i(TAG, "createEmptyGroup: gid="+gid);
        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeDB();
        return gid;
    }

    /*
     *
     * */
    public long createSingleLog(SingleLearningLog sLog){
        long l;
        getWritableDatabaseIfClosedOrNull();

        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG, sLog.getTimeInLong());
        values.put(YoMemoryContract.LearningLogs.COLUMN_GROUP_ID, sLog.getGroupId());
        values.put(YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE, sLog.isEffective());

        l = mSQLiteDatabase.insert(YoMemoryContract.LearningLogs.TABLE_NAME, null, values);

        closeDB();
        return l;
    }


    /*
     * 用于内部调用，位于事务内，不关DB。
     * */
    private long createSingleLogLeaveDbOpen(SingleLearningLog sLog){
        long l;
        getWritableDatabaseIfClosedOrNull();

        ContentValues values = new ContentValues();

        values.put(YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG, sLog.getTimeInLong());
        values.put(YoMemoryContract.LearningLogs.COLUMN_GROUP_ID, sLog.getGroupId());
        values.put(YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE, sLog.isEffective());

        l = mSQLiteDatabase.insert(YoMemoryContract.LearningLogs.TABLE_NAME, null, values);

        return l;
    }

    /*
    * 用于拆分组时，复制旧组log给新组
    * */
    public boolean createBatchLogsForGroup(ArrayList<SingleLearningLog> sLogs, int gid){
        long l;
        boolean correct = true;//标记是否都正确
        getWritableDatabaseIfClosedOrNull();

        for (SingleLearningLog s : sLogs) {
            ContentValues values = new ContentValues();

            values.put(YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG, s.getTimeInLong());
            values.put(YoMemoryContract.LearningLogs.COLUMN_GROUP_ID, gid);
            values.put(YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE, s.isEffective());

            l = mSQLiteDatabase.insert(YoMemoryContract.LearningLogs.TABLE_NAME, null, values);
            if(l == -1){ correct = false; }
        }

        closeDB();
        return correct;
    }


    /*
    * 需要读取两个表，group表获取4个字段、Logs表（经计算）获取两个字段
    * */
    public ArrayList<Group> getAllGroupsByMissionId(int missionsId, String tableSuffix){
//        Log.i(TAG, "getAllGroupsByMissionId: be");
        ArrayList<Group> groups = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Group.COLUMN_MISSION_ID+" = "+missionsId;

        getReadableDatabaseIfClosedOrNull();

        //操作两个表的读取。【问：】读取不开事务也行吧？
        mSQLiteDatabase.beginTransaction();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                Group group = new Group();
                int groupId = cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group._ID));//需多次使用，改为实名变量。
                group.setId(groupId);
                group.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DESCRIPTION)));
                group.setSettingUptimeInLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG)));
                group.setMission_id(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_MISSION_ID)));

                //【？】能否在cursor循环下读另外一张表
                group.setLastLearningTime(getLastLearningTimeInLong(groupId));
                group.setEffectiveRePickingTimes(getEffectiveLearningTime(groupId));

                group.setTotalItemsNum(getTotalSubItemsNumOfGroup(groupId,tableSuffix));

                groups.add(group);
            }while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();

        closeDB();
        return groups;
    }


    /*
    * 本方法所获取的时间记录，用于计算RMA、也可用于时间区间上限。
    * 但是时间区间下限的记录则需要使用有效复习中的最大时间记录。
    * */
    private long getLastLearningTimeInLong(int groupId){
        String selectLastTimeQuery = "SELECT "+YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG+
                " FROM "+YoMemoryContract.LearningLogs.TABLE_NAME+" WHERE "+
                YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+" = "+groupId+" ORDER BY "+
                YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG+ " DESC";
        getWritableDatabaseIfClosedOrNull();//因为相关联的前一个方法之前已经打开了WDB，这里使用getW。

        Cursor cursor = mSQLiteDatabase.rawQuery(selectLastTimeQuery,null);

        long resultLong = 0;
        if(cursor.moveToFirst()){//降序之下只取第一行即可。不使用LIMIT。
            resultLong = cursor.getLong(0);//毕竟只有1列的结果。【但是不知道是否从0起】
        }
//        Log.i(TAG, "getLastLearningTimeInLong: result LastLT="+resultLong);

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //不能关DB，因为事务尚未完成。
        return resultLong;
    }

    /*
    * 本方法用于取出有效学习（使得MS等级提升的学习）记录的时间，在计算“MS可提升时间区间”的下限时，
    * 需要使用本时间记录。（不区分MS有效性的学习记录中的最大值，对于RMA和时间区间上限的计算有效）
    * */
    private long getLastEffectiveLearningTimeInLong(int groupId){
//        Log.i(TAG, "getLastEffectiveLearningTimeInLong: be.");
        String selectLastTimeQuery = "SELECT "+YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG+
                " FROM "+YoMemoryContract.LearningLogs.TABLE_NAME+" WHERE "+
                YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+" = "+groupId+
                " AND "+YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE+" = 1 ORDER BY "+
                YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG+ " DESC";
        getWritableDatabaseIfClosedOrNull();//因为相关联的前一个方法之前已经打开了WDB，这里使用getW。

        Cursor cursor = mSQLiteDatabase.rawQuery(selectLastTimeQuery,null);

        long resultLong = 0;
        if(cursor.moveToFirst()){//降序之下只取第一行即可。不使用LIMIT。
            resultLong = cursor.getLong(0);//毕竟只有1列的结果。【但是不知道是否从0起】
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //不能关DB，因为事务尚未完成。
        return resultLong;
    }

    private byte getEffectiveLearningTime(int groupId){
//        Log.i(TAG, "getEffectiveLearningTime: be");
        String selectCountEffectiveQuery = "SELECT COUNT(*) "+" FROM "+
                YoMemoryContract.LearningLogs.TABLE_NAME+" WHERE "+
                YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+" = "+groupId+
                " AND "+YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE+" = 1";

        getWritableDatabaseIfClosedOrNull();//同一事务的前一方法中尚有打开的wdb。
        Cursor cursor = mSQLiteDatabase.rawQuery(selectCountEffectiveQuery,null);

        byte resultNumber = 0;
        if(cursor.moveToFirst()){
            resultNumber = (byte) cursor.getInt(0);
        }
//                Log.i(TAG, "getEffectiveLearningTime: result MS="+resultNumber);

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultNumber;
    }

    /*
    * 用于内部
    * */
    private short getTotalSubItemsNumOfGroup(int groupId, String tableSuffix){
//        Log.i(TAG, "getTotalSubItemsNumOfGroup: be.");
        String selectNumSubItemsQuery = "SELECT COUNT(*) "+" FROM "+
                YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+" WHERE "+
                YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+" = "+groupId;

        getWritableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectNumSubItemsQuery,null);

        short resultNum = 0;
        if(cursor.moveToFirst()){
            resultNum = cursor.getShort(0);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultNum;
    }

    /*
    * 用于外部，带关DB。
    * */
    public int getSubItemsNumOfGroup(int groupId, String tableSuffix){
//        Log.i(TAG, "getSubItemsNumOfGroup: be.");
        String selectNumSubItemsQuery = "SELECT COUNT(*) "+" FROM "+
                YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+" WHERE "+
                YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+" = "+groupId;

        getWritableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectNumSubItemsQuery,null);

        short resultNum = 0;
        if(cursor.moveToFirst()){
            resultNum = cursor.getShort(0);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSQLiteDatabase.close();
        return resultNum;
    }


    /* 获取分组的描述字段*/
    public String getGroupDescriptionById(int groupId){
        String selectGdQuery = "SELECT "+
                YoMemoryContract.Group.COLUMN_DESCRIPTION+" FROM "+
                YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group._ID+" = "+groupId;

        getWritableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectGdQuery,null);

        String strDesp = "";
        if(cursor.moveToFirst()){
            strDesp = cursor.getString(0);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSQLiteDatabase.close();
        return strDesp;
    }

    public Group getGroupById(int groupId, String tableSuffix){
        Group group = new Group();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " WHERE "+ YoMemoryContract.Group._ID+" = "+groupId;

        getReadableDatabaseIfClosedOrNull();
        mSQLiteDatabase.beginTransaction();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            group.setId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group._ID)));
            group.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DESCRIPTION)));
            group.setSettingUptimeInLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG)));
            group.setMission_id(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_MISSION_ID)));

            group.setLastLearningTime(getLastLearningTimeInLong(groupId));
            group.setEffectiveRePickingTimes(getEffectiveLearningTime(groupId));

            group.setTotalItemsNum(getTotalSubItemsNumOfGroup(groupId,tableSuffix));

        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();
//        Log.i(TAG, "getGroupById: group.ms="+group.getEffectiveRePickingTimes()+", group.llt="+group.getLastLearningTime());
        closeDB();
        return group;
    }


    /*
    * 本方法不负责对所含有的items的处理；应由调用方根据实际情况安排具体处理逻辑。
    * 批量删除group表中的几条记录
    * 因为Logs表的gid字段按级联删除模式建立到group的外键，因而DB会自动删除之
    * 而Items表与group表之间的外键没有特殊约束，因而需要手动处理所属items的归属。
    * 本方法主要适用于合并式学习之后对“被吞噬”分组的删除；其余普通的删除应使用……方法。
    * */
    public void deleteJustGroups(ArrayList<Integer> groupIds){
        getWritableDatabaseIfClosedOrNull();

        StringBuilder sbr = new StringBuilder();
        sbr.append("( ");

        for (int i :groupIds) {
            sbr.append(i);
            sbr.append(", ");
        }

        sbr.deleteCharAt(sbr.length()-2);
        sbr.append(")");

        String deleteSingleGroupSql = "DELETE FROM "+ YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group._ID+" IN "+sbr.toString();

        mSQLiteDatabase.execSQL(deleteSingleGroupSql);
        closeDB();
    }


    /*
    * 要删除一个分组，需要①group组删除，②items表资源还原（groupId列置-1，isChose置否……）
    * ③logs表中的资源groupId置-1。【暂定用-1代表被删除资源】(Logs表是否要手动处理？)
    * */
    public void deleteGroupById(int groupId,String tableSuffix){
        String deleteSingleGroupSql = "DELETE FROM "+ YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group._ID+" = "+groupId;
        getWritableDatabaseIfClosedOrNull();
        mSQLiteDatabase.beginTransaction();

        //这个方法没法返回结构状态【待。】
        mSQLiteDatabase.execSQL(deleteSingleGroupSql);
        setItemsUnChoseAndRemoveGid(tableSuffix,groupId);
        setLogsWithMinusGid(groupId);

        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();

        closeDB();

    }

    public void setLogsWithMinusGid(int gid){
        //gid暂定置为-1。表示是被删除过的。
        String logsMinusGidSql = "UPDATE "+ YoMemoryContract.LearningLogs.TABLE_NAME+
                " SET "+ YoMemoryContract.LearningLogs.COLUMN_GROUP_ID +
                " = -1 WHERE "+ YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+
                " = "+gid;

        mSQLiteDatabase.execSQL(logsMinusGidSql);
        //因为位于其他方法开启的事务内，所以不能关DB。
    }


    /*
     * 仅修改group表的描述字段
     * */
    public int updateTableGroupDescriptionSingle(int groupId,String newDescription){
        int affectedRows = 0;

        getReadableDatabaseIfClosedOrNull();

        ContentValues contentValues = new ContentValues();
        contentValues.put(YoMemoryContract.Group.COLUMN_DESCRIPTION,newDescription);

        affectedRows = mSQLiteDatabase.update(YoMemoryContract.Group.TABLE_NAME,contentValues,
                YoMemoryContract.Group._ID+" = ? ",new String[]{String.valueOf(groupId)} );
        closeDB();
        return affectedRows;
    }

    //取指定第几行上的数据
    public Group getGroupByLine(long line, String tableSuffix){
        Group group = new Group();
        String selectOneByLinesQuery = "SELECT * FROM "+ YoMemoryContract.Group.TABLE_NAME+
                " LIMIT "+line+",1";
        //【取最后一条的写法：】 " LIMIT (SELECT COUNT(*) FROM "+YoMemoryContract.Group.TABLE_NAME+" )-1,1";

        getReadableDatabaseIfClosedOrNull();
        mSQLiteDatabase.beginTransaction();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectOneByLinesQuery, null);

        if(cursor.moveToFirst()){
            int groupId = cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group._ID));

            group.setId(groupId);
            group.setDescription(cursor.getString(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_DESCRIPTION)));
            group.setSettingUptimeInLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_SETTING_UP_TIME_LONG)));
            group.setMission_id(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.Group.COLUMN_MISSION_ID)));

            group.setLastLearningTime(getLastLearningTimeInLong(groupId));
            group.setEffectiveRePickingTimes(getEffectiveLearningTime(groupId));

            group.setTotalItemsNum(getTotalSubItemsNumOfGroup(groupId,tableSuffix));

        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();

        closeDB();
        return group;
    }

    //取指定第几行上的分组的gid，但是在事务内，不需要开db
    /*public int getGroupIdByLineWithOutOpenDb(long line,String tableSuffix){
        int gid = 0;
        String selectOneByLinesQuery = "SELECT "+YoMemoryContract.Group._ID+" FROM "+
                YoMemoryContract.Group.TABLE_NAME+" LIMIT "+line+",1";
        //【取最后一条的写法：】 " LIMIT (SELECT COUNT(*) FROM "+YoMemoryContract.Group.TABLE_NAME+" )-1,1";

        Cursor cursor = mSQLiteDatabase.rawQuery(selectOneByLinesQuery, null);

        if(cursor.moveToFirst()){
            gid  = cursor.getInt(0);
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gid;
    }
*/
    /*
    * 为指定分组新增一条Logs学习记录
    * （在程序中提前判断好是否是“有效学习”，直接记在Log类中，此处直接使用。）
    * */
    public long insertNewLearningLogOfGroup(int groupId,SingleLearningLog newLog){
        long lines;
        getWritableDatabaseIfClosedOrNull();

        ContentValues values = new ContentValues();
        values.put(YoMemoryContract.LearningLogs.COLUMN_GROUP_ID,newLog.getGroupId());
        values.put(YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE,newLog.isEffective());
        values.put(YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG,newLog.getTimeInLong());

        lines = mSQLiteDatabase.insert(YoMemoryContract.LearningLogs.TABLE_NAME, null, values);

        closeDB();
        return lines;


    }


    /*
    * 获取指定分组的所有LearningLogs
    * */
    public ArrayList<SingleLearningLog> getAllLogsOfGroup(int groupId){
        ArrayList<SingleLearningLog> learningLogs = new ArrayList<>();
        String selectQuery = "SELECT * FROM "+ YoMemoryContract.LearningLogs.TABLE_NAME+
                " WHERE "+ YoMemoryContract.LearningLogs.COLUMN_GROUP_ID+" = "+groupId;

        getReadableDatabaseIfClosedOrNull();
        Cursor cursor = mSQLiteDatabase.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()){
            do{
                SingleLearningLog singleLog = new SingleLearningLog();

                singleLog.setGroupId(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.LearningLogs.COLUMN_GROUP_ID)));
                singleLog.setTimeInLong(cursor.getLong(cursor.getColumnIndex(YoMemoryContract.LearningLogs.COLUMN_TIME_IN_LONG)));
                singleLog.setEffective(cursor.getInt(cursor.getColumnIndex(YoMemoryContract.LearningLogs.COLUMN_IS_MS_EFFECTIVE))==1);

                learningLogs.add(singleLog);
            }while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeDB();
        return learningLogs;
    }


/*    public void removeGroupById(int groupId, String subItemIdsStr, String itemTableNameSuffix){
        getWritableDatabaseIfClosedOrNull();
        String deleteSingleGroupSql = "DELETE FROM "+ YoMemoryContract.Group.TABLE_NAME+" WHERE "+
                YoMemoryContract.Group._ID+" = "+groupId;

        mSQLiteDatabase.beginTransaction();

        mSQLiteDatabase.execSQL(deleteSingleGroupSql);

        setItemsUnChose(itemTableNameSuffix,subItemIdsStr);

        mSQLiteDatabase.setTransactionSuccessful();
        mSQLiteDatabase.endTransaction();
        closeDB();
    }*/


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

    /*
    * 修改一组items的其中三项内容（用于学习完成后需要拆分分组时）
    * */
    public int updateItemsTri(String tableSuffix, ArrayList<SingleItem> items){
        int rowsAffected = 0;
        getWritableDatabaseIfClosedOrNull();

        for (SingleItem singleItem : items) {
            ContentValues contentValues = new ContentValues();
            //只有三项需要修改
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_GROUP_ID,singleItem.getGroupId());
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_PRIORITY,singleItem.getPriority());
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES,singleItem.getFailedSpelling_times());

            rowsAffected+= mSQLiteDatabase.update(YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix,contentValues,
                    YoMemoryContract.ItemBasic._ID+" = ?",new String[]{String.valueOf(singleItem.getId())});
        }

        closeDB();
        return rowsAffected;

    }

    /*
     * 修改一组items的其中两项内容（用于学习完成后不需拆分分组时）
     * */
    public int updateItemsPrtAndErr(String tableSuffix, ArrayList<SingleItem> items){
        int rowsAffected = 0;
        getWritableDatabaseIfClosedOrNull();

        for (SingleItem singleItem : items) {
            ContentValues contentValues = new ContentValues();
            //只有三项需要修改
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_PRIORITY,singleItem.getPriority());
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES,singleItem.getFailedSpelling_times());

            rowsAffected += mSQLiteDatabase.update(YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix,contentValues,
                    YoMemoryContract.ItemBasic._ID+" = ?",new String[]{String.valueOf(singleItem.getId())});
        }

        closeDB();
        return rowsAffected;

    }


    /*
     * 修改一组items的后五项内容（用于LC学习模式完成后更新所属items）
     * 修改的内容是：已抽取、已学习、gid、优先级、错次。
     * 其中将两个布尔值直接置true。
     * */
    public int updateItemsPdgWithDoubleTrue(String tableSuffix, ArrayList<SingleItem> items){
        int rowsAffected = 0;
        getWritableDatabaseIfClosedOrNull();

        for (SingleItem singleItem : items) {
            ContentValues contentValues = new ContentValues();
            //五项需要修改
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE,true);
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_IS_LEARNED,true);
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_GROUP_ID,singleItem.getGroupId());
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_PRIORITY,singleItem.getPriority());
            contentValues.put(YoMemoryContract.ItemBasic.COLUMN_FAILED_SPELLING_TIMES,singleItem.getFailedSpelling_times());

            rowsAffected = mSQLiteDatabase.update(YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix,contentValues,
                    YoMemoryContract.ItemBasic._ID+" = ?",new String[]{String.valueOf(singleItem.getId())});
        }

        closeDB();
        return rowsAffected;

    }

    /*
    * 删除分组时，所含的items应当①置为未使用；②将item对应的gid置为-1。
    * 置为未使用可以通过gid快速处理，而抽取时似乎只能构造iid范围列表字串
    * */
    private void setItemsUnChoseAndRemoveGid(String tableSuffix, int groupId){
        getWritableDatabaseIfClosedOrNull();

        //gid暂定置为-1。表示是被删除过的。
        String itemsGiveBackSql = "UPDATE "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+
                " SET "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +
                " = 0,"+YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+" = -1 "+
                " WHERE "+ YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+
                " = "+groupId;

        mSQLiteDatabase.execSQL(itemsGiveBackSql);
        //因为位于其他方法开启的事务内，所以不能关DB。
    }

    /*private void setItemsUnChoseAndRemoveGid(String tableSuffix, ArrayList<Integer> itemIds){
        getWritableDatabaseIfClosedOrNull();

        if(itemIds==null||itemIds.isEmpty()){
            return;
        }

        StringBuilder sbr = new StringBuilder();
        sbr.append("( ");

        for (int i :
                itemIds) {
            sbr.append(i);
            sbr.append(", ");
        }

        sbr.deleteCharAt(sbr.length()-2);
        sbr.append(")");

        //gid暂定置为-1。表示是被删除过的。
        String itemsGiveBackSql = "UPDATE "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+
                " SET "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +
                " = 0,"+YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+" = -1 "+
                " WHERE "+ YoMemoryContract.ItemBasic._ID+
                " IN "+sbr.toString();

        mSQLiteDatabase.execSQL(itemsGiveBackSql);
        //因为位于其他方法开启的事务内，所以不能关DB。
    }*/


    private void setItemsChose(String tableSuffix,int groupId, ArrayList<Integer> subItemIds){

        if(subItemIds==null||subItemIds.isEmpty()){
            return;
        }

        StringBuilder sbr = new StringBuilder();
        sbr.append("( ");
        for (int i : subItemIds) {
            sbr.append(i);
            sbr.append(", ");
        }

        sbr.deleteCharAt(sbr.length()-2);//【错误记录，这里原来误写成str，错得很隐蔽】
        sbr.append(")");

        String itemsChoseSql = "UPDATE "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableSuffix+
                " SET "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +
                " = 1, "+YoMemoryContract.ItemBasic.COLUMN_GROUP_ID+" = "+
                +groupId+" WHERE "+ YoMemoryContract.ItemBasic._ID+
                " IN "+sbr.toString();

        mSQLiteDatabase.execSQL(itemsChoseSql);
        //因为位于其他方法开启的事务内，所以不能关DB。

    }

    /*
    * 按顺序选取Item中前n项记录（的id），提供给任务组的生成。
    * 要求从尚未抽取的items中选取。
    * 相应记录暂不该为“已抽取”，而由创建Group时的方法进行）
    * 未能选到任何结果时，返回null；
    * */
    public ArrayList<Integer> getCertainAmountItemIdsOrderly(int amount, String tableNameSuffix){
        ArrayList<Integer> ids = new ArrayList<>();
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
     * 按顺序选取Item中前n项记录,为边学边建准备数据。
     * 要求从尚未抽取的items中选取。
     * 相应记录暂不该为“已抽取”，真正创建Group时再处理）
     * 未能选到任何结果时，返回null；
     * */
    public ArrayList<SingleItem> getCertainAmountItemsOrderly(int amount, String tableNameSuffix){
        ArrayList<SingleItem> items = new ArrayList<>();

        String selectQueryOuter = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+  YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" =  0  OR "
                + YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" IS NULL LIMIT "+amount;


        getReadableDatabaseIfClosedOrNull();
//        mSQLiteDatabase.beginTransaction();

        Cursor cursor = mSQLiteDatabase.rawQuery(selectQueryOuter, null);

        if(cursor.moveToFirst()){
            do{
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

        }//【即使无结果也不能返回null；返回长为0的list即可】

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return items;
    }





    /*
     * 随机选取Item中前n项记录（的id），提供给任务组的生成。item置为已抽取的操作在建组时进行。
     * 未能选到任何结果时，返回null；
     * 【待】我怎么记得涉及到SQLite的ID的项目都需用long啊？！
     * */
    public ArrayList<Integer> getCertainAmountItemIdsRandomly(int amount, String tableNameSuffix){
        ArrayList<Integer> ids = new ArrayList<>();
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

    /*
     * 随机选取n项Items,为边学边建准备数据。
     * 要求从尚未抽取的items中选取。
     * 相应记录暂不该为“已抽取”，真正创建Group时再处理）
     * 未能选到任何结果时，返回null；
     * */
    public ArrayList<SingleItem> getCertainAmountItemsRandomly(int amount, String tableNameSuffix){
        ArrayList<SingleItem> items = new ArrayList<>();

        String selectQueryOuter = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" =  0  OR "
                + YoMemoryContract.ItemBasic.COLUMN_IS_CHOSE +" IS NULL"
                + " ORDER BY RANDOM() LIMIT "+amount;

        getReadableDatabaseIfClosedOrNull();
//        mSQLiteDatabase.beginTransaction();

        Cursor cursor = mSQLiteDatabase.rawQuery(selectQueryOuter, null);

        if(cursor.moveToFirst()){
            do{
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

        }//【即使无结果也不能返回null；返回长为0的list即可】

        try {
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDB();

        return items;
    }

    /*
     * 根据ItemId列表来获取Items。
     * */
    public ArrayList<SingleItem> getItemsWithList(ArrayList<Integer> idList,String tableNameSuffix){
//        Log.i(TAG, "getItemsWithList: before any.");
        ArrayList<SingleItem> items = new ArrayList<>();

        StringBuilder sbd = new StringBuilder();
        for (Integer i: idList) {
            sbd.append(i);
            sbd.append(", ");
        }
        sbd.deleteCharAt(sbd.length()-2);//去掉末尾多余的逗号。

        String selectQuery = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic._ID +" IN ( "+sbd.toString()+")";

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
//            Log.i(TAG, "getItemsWithList: cursor ok.");
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
     * 根据gid列表来获取对应各组所含的Items。
     * 获取的items按照所属Group的id进行分组，且按gid升序。
     * */
    public ArrayList<SingleItem> getItemsWithInGidList(ArrayList<Integer> gidList,String tableNameSuffix){
//        Log.i(TAG, "getItemsWithInGidList: before any.");
        ArrayList<SingleItem> items = new ArrayList<>();

        StringBuilder sbd = new StringBuilder();
        for (Integer i: gidList) {
            sbd.append(i);
            sbd.append(", ");
        }
        sbd.deleteCharAt(sbd.length()-2);//去掉末尾多余的逗号。

        String selectQuery = "SELECT * FROM "+ YoMemoryContract.ItemBasic.TABLE_NAME+tableNameSuffix
                +" WHERE "+ YoMemoryContract.ItemBasic.COLUMN_GROUP_ID +" IN ( "+sbd.toString()+")"
                +" ORDER BY "+YoMemoryContract.ItemBasic.COLUMN_GROUP_ID + " ASC";

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
//            Log.i(TAG, "getItemsWithList: cursor ok.");
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
