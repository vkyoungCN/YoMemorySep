package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.vkyoungcn.smartdevices.yomemory.R;

@SuppressWarnings("all")
/**
 * Created by VkYoung16 on 2018/3/26 0026.
 */

public class RvMission implements Parcelable {
    private int id=0;
    private String name="";
    private String description="";
    private String tableItem_suffix="";
    private int starType = 1;//星标类型，默认1（对应蓝色？）。

    private int startResourceId = 0;//用于Rv快速显示，从startType量计算转换得来。

    //完全构造器
    public RvMission(int id, String name, String description, String tableItem_suffix, int starType, int startResourceId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tableItem_suffix = tableItem_suffix;
        this.starType = starType;
        this.startResourceId = startResourceId;
    }

    //专用于从DB数据转换的构造器
    public RvMission(Mission mission){
        this.id = mission.getId();
        this.name = mission.getName();
        this.description = mission.getDescription();
        this.tableItem_suffix = mission.getTableItem_suffix();
        this.starType = mission.getStarType();

        switch (starType){
            case 0:
                this.startResourceId = R.drawable.star_gray;
                break;
            case 1:
                this.startResourceId = R.drawable.star_blue;
                break;
            case 2:
                this.startResourceId = R.drawable.star_red;
                break;
            default:
                this.startResourceId = R.drawable.star_blue;
        }
        this.startResourceId = startResourceId;

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

    public int getStartResourceId() {
        return startResourceId;
    }

    public void setStartResourceId(int startResourceId) {
        this.startResourceId = startResourceId;
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
        parcel.writeInt(startResourceId);

    }

    public static final Creator<RvMission> CREATOR = new Creator<RvMission>(){
        @Override
        public RvMission createFromParcel(Parcel parcel) {
            return new RvMission(parcel);
        }

        @Override
        public RvMission[] newArray(int size) {
            return new RvMission[size];
        }
    };

    private RvMission(Parcel in){
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        tableItem_suffix = in.readString();
        starType = in.readInt();
        startResourceId = in.readInt();
    }

}
