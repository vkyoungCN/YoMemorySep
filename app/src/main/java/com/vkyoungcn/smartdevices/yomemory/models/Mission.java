package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("all")
/**
 * Created by VkYoung16 on 2018/3/26 0026.
 */

public class Mission implements Parcelable {
    private int id=0;
    private String name="";
    private String simpleDescription ="";
    private String detailDescriptions="";
    private String tableItem_suffix="";
    private int starType = 1;//星标类型，默认1（对应蓝色？）。【目前没什么意义，考虑后期改成任务“标签类型”】

    //空构造器
    public Mission() {
    }

    //完全构造器


    public Mission(int id, String name, String simpleDescription, String detailDescriptions, String tableItem_suffix, int starType) {
        this.id = id;
        this.name = name;
        this.simpleDescription = simpleDescription;
        this.detailDescriptions = detailDescriptions;
        this.tableItem_suffix = tableItem_suffix;
        this.starType = starType;
    }

    //无id构造器
    public Mission(String name, String simpleDescription, String detailDescriptions, String tableItem_suffix, int starType) {
        this.name = name;
        this.simpleDescription = simpleDescription;
        this.detailDescriptions = detailDescriptions;
        this.tableItem_suffix = tableItem_suffix;
        this.starType = starType;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSimpleDescription() {
        return simpleDescription;
    }

    public void setSimpleDescription(String simpleDescription) {
        this.simpleDescription = simpleDescription;
    }

    public int getStarType() {
        return starType;
    }

    public void setStarType(int starType) {
        this.starType = starType;
    }

    public String getTableItem_suffix() {
        return tableItem_suffix;
    }

    public void setTableItem_suffix(String tableItem_suffix) {
        this.tableItem_suffix = tableItem_suffix;
    }

    public String getDetailDescriptions() {
        return detailDescriptions;
    }

    public void setDetailDescriptions(String detailDescriptions) {
        this.detailDescriptions = detailDescriptions;
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
        parcel.writeString(name);
        parcel.writeString(simpleDescription);
        parcel.writeString(detailDescriptions);
        parcel.writeString(tableItem_suffix);
        parcel.writeInt(starType);
    }

    public static final Parcelable.Creator<Mission> CREATOR = new Parcelable.Creator<Mission>(){
        @Override
        public Mission createFromParcel(Parcel parcel) {
            return new Mission(parcel);
        }

        @Override
        public Mission[] newArray(int size) {
            return new Mission[size];
        }
    };

    private Mission(Parcel in){
        id = in.readInt();
        name = in.readString();
        simpleDescription = in.readString();
        detailDescriptions = in.readString();
        tableItem_suffix = in.readString();
        starType = in.readInt();
    }

}
