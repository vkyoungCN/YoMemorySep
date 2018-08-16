package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class Group implements Parcelable{
//* “分组”对应的数据模型，
//* 分组对应两种数据模型DBGroup和RVGroup，其中DB版对应数据库操作，RV版对应UI中的快速直接显示。
//* 对应DB中的三张表：group表全体；部分字段从logs表、items表获取数据。
//* 若要将数据应用到UI，需转化为RVGroup类。
    private static final String TAG = "Group";

    /* 字段 */
    //group表
    int id = 0;//DB列
    String description="";//DB列。默认填入该组“起始-末尾”词汇
    int mission_id=0;//v5新增。
    long settingUptimeInLong = 0;//初学时间，需据此记录计算已过时间和当前所处时间段的颜色。默认0.

    //以下两项由Logs表提供
    long lastLearningTime = 0;//最新的学习记录（是否有效m皆可；RMA的计算不需要持有整个Logs列表）。
    private byte effectiveRePickingTimes = 0;//有效学习次数（*背后的设计原理：复习间隔过久时的新一次复习，只刷新记忆量，不抬升曲线，属“无效”复习，不增加有效次数）
    //可直接对应为RVGroup类的MS字段

    //以下字段由Items表提供
    int totalItemsNum = 0;//本组下含多少项资源。


    //从DB读取数据时，需要先声明一个空的。
    public Group() {
    }

    //完全构造器
    public Group(int id, String description, int mission_id, long settingUptimeInLong, long lastLearningTime, byte effectiveRePickingTimes, short totalItemsNum) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;
        this.lastLearningTime = lastLearningTime;
        this.effectiveRePickingTimes = effectiveRePickingTimes;
        this.totalItemsNum = totalItemsNum;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMission_id() {
        return mission_id;
    }

    public void setMission_id(int mission_id) {
        this.mission_id = mission_id;
    }

    public long getSettingUptimeInLong() {
        return settingUptimeInLong;
    }

    public void setSettingUptimeInLong(long settingUptimeInLong) {
        this.settingUptimeInLong = settingUptimeInLong;
    }

    public long getLastLearningTime() {
        return lastLearningTime;
    }

    public void setLastLearningTime(long lastLearningTime) {
        this.lastLearningTime = lastLearningTime;
    }

    public byte getEffectiveRePickingTimes() {
        return effectiveRePickingTimes;
    }

    public void setEffectiveRePickingTimes(byte effectiveRePickingTimes) {
        this.effectiveRePickingTimes = effectiveRePickingTimes;
    }

    public int getTotalItemsNum() {
        return totalItemsNum;
    }

    public void setTotalItemsNum(int totalItemsNum) {
        this.totalItemsNum = totalItemsNum;
    }



    /*
     * 以下是Parcelable要求的内容
     * */
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(description);
        parcel.writeInt(mission_id);
        parcel.writeLong(settingUptimeInLong);

        parcel.writeLong(lastLearningTime);
        parcel.writeByte(effectiveRePickingTimes);

        parcel.writeInt(totalItemsNum);

    }

    public static final Parcelable.Creator<Group> CREATOR = new Parcelable.Creator<Group>(){
        @Override
        public Group createFromParcel(Parcel parcel) {
            return new Group(parcel);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    private Group(Parcel in){
        id = in.readInt();
        description = in.readString();
        mission_id = in.readInt();
        settingUptimeInLong = in.readLong();
        lastLearningTime = in.readLong();
        effectiveRePickingTimes = in.readByte();
        totalItemsNum = (short)in.readInt();
    }


}
