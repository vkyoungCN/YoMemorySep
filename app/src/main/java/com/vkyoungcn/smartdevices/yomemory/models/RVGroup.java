package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;

/*
* 本类用于减轻RecyclerView的Adapter中，数据显示过程中的负担，提前将需要转换的数据计算出来，直接提供。
* */
public class RVGroup implements Parcelable{
    private static final String TAG = "RVGroup";

    private int id = 0;//DB列
    private String description="";//DB列。默认填入该组“起始-末尾”词汇
    private int mission_id=0;//属于哪个任务。
    private long settingUptimeInLong = 0;//初学时间。默认0.
    private long lastLearningTime = 0;//分组的复习/学习记录中最新的一条，默认值为0，是出错。

    //以下两项是需要在Rv中展示且需要计算的来的值（对于在详情页中才进行展示的信息，不需单列字段）
    private float RM_Amount = 0;//记忆存量（Retaining Memory Amount）
    private byte memoryStage = 0;//记忆级别，记忆级别越高，衰减越缓慢。等于DBGroup中的effectiveRePickingTime,二者相互等价，字段只设其一。

    //以下一项是Item表提供给DBGroup的
    private short totalItemsNum= 0;//本组所属资源总量

    public RVGroup(int id, String description, int mission_id, long settingUptimeInLong, long lastLearningTime, float RM_Amount, byte memoryStage, short totalItemsNum) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;

        this.lastLearningTime = lastLearningTime;
        this.RM_Amount = RM_Amount;
        this.memoryStage = memoryStage;

        this.totalItemsNum = totalItemsNum;
    }

    /*
    * 根据DBGroup数据计算得到RvGroup；
    * 其中记忆存量和记忆级别需要计算
    * */
    public RVGroup(DBGroup dbGroup){
        this.id = dbGroup.getId();
        this.description = dbGroup.getDescription();
        this.mission_id = dbGroup.getMission_id();
        this.settingUptimeInLong = dbGroup.getSettingUptimeInLong();
        this.lastLearningTime = dbGroup.getLastLearningTime();//后面会使用，注意顺序哦。
        this.memoryStage = dbGroup.getEffectiveRePickingTimes() ;//后一句会使用，注意顺序哦。

        this.RM_Amount = getCurrentRMAmount();//需要计算。需要使用刚刚获取的lastLearningTime、memoryStage数据。
        this.totalItemsNum = dbGroup.getTotalItemNum();

    }


    /*
    * 用于为当前分组（当前分组的Logs记录），计算当前时点下的记忆存量
    * */
    private float getCurrentRMAmount(){
//        Log.i(TAG, "getCurrentRMAmount: be.");
        //首先要获取要参与计算的m值，即实际有效的复习次数。直接使用之前已获得的数据。

        //获取当前时间和最后一条Log之间的时间间隔(这个只需要值最大的一条即可，不必管什么有效不有效)
        long timeIntervalInLong = System.currentTimeMillis()-lastLearningTime;
        int timeInterval = (int) (timeIntervalInLong/(1000*60));

        //代入函数计算得记忆存量。
        return baseCalculate(2.5f,2.05f,3,memoryStage,timeInterval);

    }

    private float baseCalculate(float alpha, float beta, int fakeNum, int realNum, int timeInterval){
        double m1 = 100*(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum));//分子部分
        double m2 = timeInterval+(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum))-1;//分母部分
        double retainingMemory = m1/m2;
        BigDecimal rM_BD = new BigDecimal(retainingMemory);
        return rM_BD.setScale(1,BigDecimal.ROUND_HALF_UP).floatValue();
    }


    /*
    * 衰减到某个指定的M值之前还剩多长时间
    * 在该时间内复习可以提升记忆等级MS，否则只是刷新记忆总量而已。
    * 该指定的M值，取决于当前所处的MS。
    * （由于设计中原realNum是对应复习，后来将学习和复习统一化，则m应从0其，初学次对应的m是0，对应的MS
    * 记忆等级也是0）
    * */
    public static int minutesRemainTillThreshold( byte memoryStage){
        float alpha = 2.5f;
        float beta =2.05f;
        int fakeNum = 3;

        int target_RM ;
        switch (memoryStage){
            case 0:
            case 1:
            case 2:
                target_RM =10;
                break;
            case 3:
            case 4:
                target_RM=12;
                break;
            case 5:
                target_RM=24;
                break;
            case 6:
                target_RM=36;
                break;
            default:
                target_RM = 50;
        }

        double block_1 = (Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),memoryStage));
        return (int)((100*block_1)/target_RM+1-block_1);
    }


    /*
    * 用于刷新retainingAmount和memoryStage字段；
    * 从而用于Rv-UI显示的变更。
    * 通过返回值告知调用方是否进行了实际的更新。
    * */
    public boolean refreshRMA(){
        float newRMA = getCurrentRMAmount();
        if(this.RM_Amount == newRMA){
            return false;//代表新旧值一致，无需更新
        }else {
            this.RM_Amount = newRMA;
            return true;//新旧值不同，需更新
        }
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

    public float getRM_Amount() {
        return RM_Amount;
    }

    public void setRM_Amount(float RM_Amount) {
        this.RM_Amount = RM_Amount;
    }

    public byte getMemoryStage() {
        return memoryStage;
    }

    public void setMemoryStage(byte memoryStage) {
        this.memoryStage = memoryStage;
    }

    public short getTotalItemsNum() {
        return totalItemsNum;
    }

    public void setTotalItemsNum(short totalItemsNum) {
        this.totalItemsNum = totalItemsNum;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        //如果不包含引用成员，则浅复制即可
        RVGroup rvGroup = null;
        try {
            rvGroup = (RVGroup)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return rvGroup;

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
        parcel.writeSerializable(lastLearningTime);
        parcel.writeByte(memoryStage);
        parcel.writeFloat(RM_Amount);
        parcel.writeInt(totalItemsNum);
    }

    public static final Parcelable.Creator<RVGroup> CREATOR = new Parcelable.Creator<RVGroup>(){
        @Override
        public RVGroup createFromParcel(Parcel parcel) {
            return new RVGroup(parcel);
        }

        @Override
        public RVGroup[] newArray(int size) {
            return new RVGroup[size];
        }
    };

    private RVGroup(Parcel in){
        id = in.readInt();
        description = in.readString();
        mission_id = in.readInt();
        settingUptimeInLong = in.readLong();
        lastLearningTime = in.readLong();
        memoryStage = in.readByte();
        RM_Amount = in.readFloat();
        totalItemsNum = (short)in.readInt();
    }



}
