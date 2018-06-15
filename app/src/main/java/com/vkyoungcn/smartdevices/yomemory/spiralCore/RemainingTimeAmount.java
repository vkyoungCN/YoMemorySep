package com.vkyoungcn.smartdevices.yomemory.spiralCore;

@SuppressWarnings("all")
public class RemainingTimeAmount {
//    private static final String TAG = "RemainingTimeAmount";

    private byte remainingMinutes = 0;//距离下一阶段还剩余多少分钟。
    private byte remainingHours = 0;//距离下一阶段还剩余多少小时。
    private byte remainingDays = 0;

    public RemainingTimeAmount(byte remainingMinutes, byte remainingHours, byte remainingDays) {
        this.remainingMinutes = remainingMinutes;
        this.remainingHours = remainingHours;
        this.remainingDays = remainingDays;
    }

    public byte getRemainingMinutes() {
        return remainingMinutes;
    }

    public void setRemainingMinutes(byte remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public byte getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(byte remainingHours) {
        this.remainingHours = remainingHours;
    }

    public byte getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(byte remainingDays) {
        this.remainingDays = remainingDays;
    }

    public static long getRemainingTimeInMinutes(RemainingTimeAmount remainingTimeAmount){
        return (remainingTimeAmount.getRemainingDays()*24*60+remainingTimeAmount.getRemainingHours()*60+remainingTimeAmount.getRemainingMinutes());
    }

}
