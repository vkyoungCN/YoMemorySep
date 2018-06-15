package com.vkyoungcn.smartdevices.yomemory.spiralCore;

import android.os.Parcel;
import android.os.Parcelable;

import com.vkyoungcn.learningtools.R;


/*
* 此类是分组的一项状态集（阶段（以颜色表示）+ 距初学的已过时间）
* 颜色和时间均根据分组的“初学时间”进行计算而获得、初学时间在新版本中是DB字段，位于Group表中。
* */
public class GroupState implements Parcelable {
    private stateNumber state = stateNumber.NEWLY_CREATED;//用于判断的标志。初版采用颜色资源判断，开销太大。
    private int colorResId = 0;//当前阶段对应的颜色资源id，配合setBackgroundResource (int resid)，传入0时移除背景。

    private short remainingMinutes = 0;//距离下一阶段还剩余多少分钟。
    private short remainingHours = 0;//距离下一阶段还剩余多少小时。
    private short remainingDays = 0;

    /*虽然Android官方不建议使用Enum，声称其内存开销二倍于替代方案；但是其便利性无可替代
     * 相应的额外开销值得付出。stack overflow上仍可见许多关于Enum使用方式的讨论可见并未实际弃用
     * 【后来发现多数地方都直接使用了stateColor替代（作为判断条件），而无需专用枚举类】
     * */
    public enum stateNumber {
         NEWLY_CREATED,//新建分组，未学习。
        NOT_YET,//还未到复习时间
         AVAILABLE,//可以复习了
         MISSED_ONCE,//错过一次，应尽快复习。
         MISSED_TWICE,//连续错过两次，本分组超时，复习失败。
         ACCOMPLISHED,//复习按计划完成，已上岸。
    }

    /* 这个是实际资源组，需要指定具体资源，不是enum 所以只能使用静态final类。*/
    public final static class ColorResId {
        public static final int COLOR_NEWLY = R.color.colorGP_Newly ;//新建组，没有初记Log,可能还没学习，建议绿色；
        public static final int COLOR_STILL_NOT = R.color.colorGP_STILL_NOT;//还没到复习时间，建议对应灰色；
        public static final int COLOR_AVAILABLE = R.color.colorGP_AVAILABLE;//可以进行复习，应在xHxM内完成本次复习，建议蓝色；
        public static final int COLOR_MISSED_ONCE = R.color.colorGP_Miss_ONCE;//错过了上一次，应在xHxM内完成本次复习，建议橙色；
        public static final int COLOR_MISSED_TWICE = R.color.colorGP_Miss_TWICE;//连续错过两次，标红。
        public static final int COLOR_FULL =0;//完成12次记录，超4个月的记录,建议使用无色。
    }

    public GroupState() {
    }

    public GroupState(String logsStr) {

        this.state = LogList.getCurrentStateIntegerForGroup(logsStr);
        this.colorResId = LogList.getCurrentColorResourceForGroup(logsStr);

        RemainingTimeAmount remainingTimeAmount = LogList.getCurrentRemainingTimeForGroup(logsStr);
        this.remainingMinutes = remainingTimeAmount.getRemainingMinutes();
        this.remainingHours = remainingTimeAmount.getRemainingHours();
        this.remainingDays = remainingTimeAmount.getRemainingDays();
    }

    public stateNumber getState() {
        return state;
    }

    public void setState(stateNumber state) {
        this.state = state;
    }

    public int getColorResId() {
        return colorResId;
    }
    public void setColorResId(int colorResId) {
        this.colorResId = colorResId;
    }

    public short getRemainingMinutes() {
        return remainingMinutes;
    }
    public void setRemainingMinutes(short remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public short getRemainingHours() {
        return remainingHours;
    }
    public void setRemainingHours(short remainingHours) {
        this.remainingHours = remainingHours;
    }

    public short getRemainingDays() {
        return remainingDays;
    }
    public void setRemainingDays(short remainingDays) {
        this.remainingDays = remainingDays;
    }


    /*
    * 以下是Parcelable要求的内容
    * */
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeSerializable(state);
        parcel.writeInt(colorResId);
        parcel.writeInt((int)remainingMinutes);
        parcel.writeInt((int)remainingHours);
        parcel.writeInt((int)remainingDays);
    }

    public static final Creator<GroupState> CREATOR = new Creator<GroupState>(){
        @Override
        public GroupState createFromParcel(Parcel parcel) {
            return new GroupState(parcel);
        }

        @Override
        public GroupState[] newArray(int size) {
            return new GroupState[size];
        }
    };

    private GroupState(Parcel in){
        state = (stateNumber) in.readSerializable();
        colorResId = in.readInt();
        remainingMinutes = (short) in.readInt();
        remainingHours = (short) in.readInt();
        remainingDays = (short) in.readInt();
    }


}
