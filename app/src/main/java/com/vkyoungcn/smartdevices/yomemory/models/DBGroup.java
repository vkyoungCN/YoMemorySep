package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/*
* 主要用于向数据库存记录（的最后与DB交互）时使用，以及从数据库取数据（最初从DB取出数据）时使用
* 若要将数据应用到UI，需转化为Group类。
* */
public class DBGroup implements Parcelable{
    private static final String TAG = "DBGroup";

    private int id = 0;//DB列
    private String description="";//DB列。默认填入该组“起始-末尾”词汇
    private int mission_id=0;//v5新增。
    private long settingUptimeInLong = 0;//初学时间，需据此记录计算已过时间和当前所处时间段的颜色。默认0.

    //以下两项由Logs表提供
    private long lastLearningTime = 0;//最新的学习记录（是否有效m皆可；RMA的计算不需要持有整个Logs列表）。
    private byte effectiveRePickingTimes = 0;//有效学习次数（*背后的设计原理：复习间隔过久时的新一次复习，只刷新记忆量，不抬升曲线，属“无效”复习，不增加有效次数）
    //可直接对应为MS字段

    //以下字段由Items表提供
    //所含Items的数量，有两种方案①：因为只在显示时需要，考虑直接由RvGroup负责拉取。②出于职能清晰明确的考虑，由本类拉取。（暂定后者）
    private short totalItemNum = 0;//本组下含多少项资源。用于Rv显示。


    //从DB读取数据时，需要先声明一个空的。
    public DBGroup() {
    }

    public DBGroup(int id, String description, int mission_id, long settingUptimeInLong, long lastLearningTime, byte effectiveRePickingTimes, short totalItemNum) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;
        this.lastLearningTime = lastLearningTime;
        this.effectiveRePickingTimes = effectiveRePickingTimes;
        this.totalItemNum = totalItemNum;
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

    public short getTotalItemNum() {
        return totalItemNum;
    }

    public void setTotalItemNum(short totalItemNum) {
        this.totalItemNum = totalItemNum;
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

        parcel.writeInt(totalItemNum);

    }

    public static final Parcelable.Creator<DBGroup> CREATOR = new Parcelable.Creator<DBGroup>(){
        @Override
        public DBGroup createFromParcel(Parcel parcel) {
            return new DBGroup(parcel);
        }

        @Override
        public DBGroup[] newArray(int size) {
            return new DBGroup[size];
        }
    };

    private DBGroup(Parcel in){
        id = in.readInt();
        description = in.readString();
        mission_id = in.readInt();
        settingUptimeInLong = in.readLong();
        lastLearningTime = in.readLong();
        effectiveRePickingTimes = in.readByte();
        totalItemNum = (short)in.readInt();
    }


}
