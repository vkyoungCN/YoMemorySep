package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

/*
* 用于仅需修改Group表时。常规的DBGroup需要涉及group、log、items三张表的查询。
* 在合并学习中的碎片拆分逻辑中，被拆分碎片组只需修改group表中的description字段；
* 其item表（所属各子Items）的修改由其他逻辑直接改动相关items的归属gid。
* */
public class DbTableGroup implements Parcelable{
    private static final String TAG = "DBGroup";

    private int id = 0;//DB列
    private String description="";//DB列。默认填入该组“起始-末尾”词汇
    private int mission_id=0;//v5新增。
    private long settingUptimeInLong = 0;//初学时间，需据此记录计算已过时间和当前所处时间段的颜色。默认0.




    //从DB读取数据时，需要先声明一个空的。
    public DbTableGroup() {
    }

    public DbTableGroup(int id, String description, int mission_id, long settingUptimeInLong) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;
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
    }

    public static final Creator<DbTableGroup> CREATOR = new Creator<DbTableGroup>(){
        @Override
        public DbTableGroup createFromParcel(Parcel parcel) {
            return new DbTableGroup(parcel);
        }

        @Override
        public DbTableGroup[] newArray(int size) {
            return new DbTableGroup[size];
        }
    };

    private DbTableGroup(Parcel in){
        id = in.readInt();
        description = in.readString();
        mission_id = in.readInt();
        settingUptimeInLong = in.readLong();
    }


}
