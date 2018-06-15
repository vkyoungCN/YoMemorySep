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
    private String description="";
    private String tableItem_suffix="";
    private int starType = 1;//星标类型，默认1（对应蓝色？）。

    //空构造器
    public Mission() {
    }

    //完全构造器
    public Mission(int id, String name, String description, String tableItem_suffix, int starType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tableItem_suffix = tableItem_suffix;
        this.starType = starType;
    }

    //无id构造器
    public Mission(String name, String description, String tableItem_suffix, int starType) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
        parcel.writeString(description);
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
        description = in.readString();
        tableItem_suffix = in.readString();
        starType = in.readInt();
    }

}
