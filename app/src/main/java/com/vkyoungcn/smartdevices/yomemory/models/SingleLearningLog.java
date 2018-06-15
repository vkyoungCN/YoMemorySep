package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class SingleLearningLog implements Parcelable{
    private static final String TAG = "SingleLearningLog";
    private long timeInLong = 0;
    private int groupId = 0;
    private boolean isEffective = false;

    public SingleLearningLog(long timeInLong, int groupId, boolean isEffective) {
        this.timeInLong = timeInLong;
        this.groupId = groupId;
        this.isEffective = isEffective;
    }

    public long getTimeInLong() {
        return timeInLong;
    }

    public void setTimeInLong(long timeInLong) {
        this.timeInLong = timeInLong;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public boolean isEffective() {
        return isEffective;
    }

    public void setEffective(boolean effective) {
        isEffective = effective;
    }

    /*
     * 以下是Parcelable要求的内容
     * */
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(timeInLong);
        parcel.writeInt(groupId);
        parcel.writeByte((byte)(isEffective?1:0));

    }

    public static final Parcelable.Creator<SingleLearningLog> CREATOR = new Parcelable.Creator<SingleLearningLog>(){
        @Override
        public SingleLearningLog createFromParcel(Parcel parcel) {
            return new SingleLearningLog(parcel);
        }

        @Override
        public SingleLearningLog[] newArray(int size) {
            return new SingleLearningLog[size];
        }
    };

    private SingleLearningLog(Parcel in){
        timeInLong = in.readLong();
        groupId = in.readInt();
        isEffective = in.readByte() == 1;
    }




}
