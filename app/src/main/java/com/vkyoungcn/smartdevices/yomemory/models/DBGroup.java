package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

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
    private ArrayList<SingleLearningLog> learningLogs = new ArrayList<>();//分组的所有复习/学习记录，暂不引入SingleLog类。

    public DBGroup(int id, String description, int mission_id, long settingUptimeInLong, ArrayList<SingleLearningLog> learningLogs) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;
        this.learningLogs = learningLogs;
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

    public ArrayList<SingleLearningLog> getLearningLogs() {
        return learningLogs;
    }

    public void setLearningLogs(ArrayList<SingleLearningLog> learningLogs) {
        this.learningLogs = learningLogs;
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
        parcel.writeSerializable(learningLogs);
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
        learningLogs = (ArrayList<SingleLearningLog>) in.readSerializable();
    }


}
