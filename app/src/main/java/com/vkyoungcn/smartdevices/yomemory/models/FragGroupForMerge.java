package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;

/*
 * 用于选择“用于合并的碎片分组”时使用的临时Model类。
 * */
public class FragGroupForMerge implements Parcelable{
    private static final String TAG = "FragGroupForMerge";

    private boolean isChecked = false;//对应checkBox
    private int id = 0;//DB列
    private int totalItemsNum= 0;//本组所属资源总量


    public FragGroupForMerge(RVGroup rvGroup) {
        this.id = rvGroup.getId();
        this.totalItemsNum = rvGroup.getTotalItemsNum();
    }

    public FragGroupForMerge(DBGroup dbGroup) {
        this.id = dbGroup.getId();
        this.totalItemsNum = dbGroup.getTotalItemNum();
    }

    public FragGroupForMerge(boolean isChecked, int id, short totalItemsNum) {
        this.isChecked = isChecked;
        this.id = id;
        this.totalItemsNum = totalItemsNum;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTotalItemsNum() {
        return totalItemsNum;
    }

    public void setTotalItemsNum(int totalItemsNum) {
        this.totalItemsNum = totalItemsNum;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        //如果不包含引用成员，则浅复制即可
        FragGroupForMerge fragGroupForMerge = null;
        try {
            fragGroupForMerge = (FragGroupForMerge) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return fragGroupForMerge;

    }


    /*
     * 以下是Parcelable要求的内容
     * */
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte)( isChecked?1:0));
        parcel.writeInt(id);
        parcel.writeInt(totalItemsNum);
    }

    public static final Parcelable.Creator<FragGroupForMerge> CREATOR = new Parcelable.Creator<FragGroupForMerge>(){
        @Override
        public FragGroupForMerge createFromParcel(Parcel parcel) {
            return new FragGroupForMerge(parcel);
        }

        @Override
        public FragGroupForMerge[] newArray(int size) {
            return new FragGroupForMerge[size];
        }
    };

    private FragGroupForMerge(Parcel in){
        isChecked = in.readByte()==1;
        id = in.readInt();
        totalItemsNum = in.readInt();
    }

}
