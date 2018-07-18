package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class RVGroup implements Parcelable ,Cloneable{
//* 本类用于减轻RecyclerView在数据显示过程中的负担，提前将需要转换的数据计算出来，直接提供显示资源
//* 本类还提供有一些公共静态计算方法，用于计算指定分组的MS/RMA/限制时间等数据

    private static final String TAG = "RVGroup";

    /* 字段 */
    //group表提供
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

    public RVGroup() {
    }

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
        if(memoryStage==0){
            //新设组，尚未学习，其Rma直接设0
            return 0.0f;
        }

        //获取当前时间和最后一条Log之间的时间间隔(这个只需要值最大的一条即可，不必管什么有效不有效)
        long timeIntervalInLong = System.currentTimeMillis()-lastLearningTime;
        int timeInterval = (int) (timeIntervalInLong/(1000*60));

        //代入函数计算得记忆存量。
        return baseCalculate(2.5f,2.05f,3,memoryStage,timeInterval);

    }


    public static float getRMAmount(byte memoryStage, long lastLearningTime){
//        Log.i(TAG, "getCurrentRMAmount: be.");
        //首先要获取要参与计算的m值，即实际有效的复习次数。直接使用之前已获得的数据。
        if(memoryStage==0){
            //新设组，尚未学习，其Rma直接设0
            return 0.0f;
        }

        //获取当前时间和最后一条Log之间的时间间隔(这个只需要值最大的一条即可，不必管什么有效不有效)
        long timeIntervalInLong = System.currentTimeMillis()-lastLearningTime;
        int timeInterval = (int) (timeIntervalInLong/(1000*60));

        //代入函数计算得记忆存量。
        return baseCalculate(2.5f,2.05f,3,memoryStage,timeInterval);

    }

    public static float baseCalculate(float alpha, float beta, int fakeNum, int realNum, int timeInterval){
        double m1 = 100*(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum));//分子部分
        double m2 = timeInterval+(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum))-1;//分母部分
        double retainingMemory = m1/m2;
        BigDecimal rM_BD = new BigDecimal(retainingMemory);
        return rM_BD.setScale(1,BigDecimal.ROUND_HALF_UP).floatValue();
    }

    public int getMinutesTillFarThreshold(){
        return minutesTillFarThreshold(this.memoryStage,this.getRM_Amount());
    }

    /*
    * （在指定MS下）衰减到某个指定的M值之前还剩多长时间
    * 在该时间内复习可以提升记忆等级MS，否则只是刷新记忆总量而已。
    * 该指定的M值，取决于当前所处的MS。
    * 【分组新建后，无任何学习数据，此时的MS设计为=0，经过一次学习后，MS加到1；随后的复习继续提升。】
    * */
    public static int minutesTillFarThreshold(byte memoryStage,float currentRma){
        float alpha = 2.5f;
        float beta =2.05f;
        int fakeNum = 3;

        int target_RM ;
        switch (memoryStage){
            case 0:
                return -1;//目前MS=0仍然对应新建组尚未学习状态，只需尽快学习，无时间限制。
            case 1:
                target_RM =10;
                break;
            case 2:
                target_RM =12;
                break;
            case 3:
                target_RM =16;
                break;
            case 4:
                target_RM=25;
                break;
            case 5:
                target_RM=39;
                break;
            case 6:
                target_RM=59;
                break;
            case 7:
                target_RM=78;
                break;
            case 8:
                target_RM=89;
                break;
            case 9:
                target_RM=95;
                break;
            case 10:
                target_RM=98;
                break;
            default:
                target_RM = 99;

        }
        if(currentRma<=target_RM){
            return -2;//已经超时
        }
        double block_1 = (Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),memoryStage));
        return (int)((100*block_1)/target_RM+1-block_1);
    }


    public static int minutesBeforeShortThreshold(byte memoryStage,float currentRma){
        return minutesTillFarThreshold(memoryStage,currentRma)/5;//暂定为上限的1/5。
        //具体时间当然还需要结合上次有效的记录时间和本值进行计算……
    }


    /*
    * 用于刷新retainingAmount和memoryStage字段；
    * 从而用于Rv-UI显示的变更。
    * 通过返回值告知调用方是否进行了实际的更新。
    * */
    public boolean needRmaRefresh(){
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
        RVGroup rvGroup = new RVGroup();//这里指向new或者null都不出错。但是即使指向null，RvGroup也必须有空构造器
        // 否则崩溃，空指针错误。同时RvGroup需要实现Cloneable.
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
        parcel.writeLong(lastLearningTime);
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
