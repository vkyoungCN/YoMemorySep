package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;
import java.util.ArrayList;

public class RVGroup implements Parcelable{
    private static final String TAG = "RVGroup";

    private int id = 0;//DB列
    private String description="";//DB列。默认填入该组“起始-末尾”词汇
    private int mission_id=0;//属于哪个任务。
    private long settingUptimeInLong = 0;//初学时间。默认0.
    private ArrayList<SingleLearningLog> learningLogs = new ArrayList<>();//分组的所有复习/学习记录，暂不引入SingleLog类。
【我的设计思路可能存在缺陷——从正确的角度看，可能没有必要在Group中保持一个Logs的完全列表；只需要持有：①有效操作次数（但是，需要
    升级SingleLearningLog类，使其具备isEffective字段，从而能让DB直接给出有效次数数据）；②最近的一条Logs记录（用于
    计算和当前时点的时间间隔）】


    //以下两项是需要在Rv中展示且需要计算的来的值（对于在详情页中才进行展示的信息，在此不单列字段）
    private byte RM_Amount = 0;//记忆存量（Retaining Memory Amount）
    private byte memoryStage = 0;//记忆级别，等于m有效数值（有效实操次数）。记忆级别越高，衰减越缓慢。

    public RVGroup(int id, String description, int mission_id, long settingUptimeInLong, ArrayList<SingleLearningLog> learningLogs, byte RM_Amount, byte memoryStage) {
        this.id = id;
        this.description = description;
        this.mission_id = mission_id;
        this.settingUptimeInLong = settingUptimeInLong;
        this.learningLogs = learningLogs;
        this.RM_Amount = RM_Amount;
        this.memoryStage = memoryStage;
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
        this.learningLogs = dbGroup.getLearningLogs();

        this.RM_Amount = getCurrentRMAmount();//需要计算。需要从learningLogs得到数值。需要核心业务类。
        this.memoryStage = ;



    }


    /*
    * 用于为当前分组（基于当前分组的Logs记录），计算当前时点下的记忆存量
    * */
    private void getCurrentRMAmount(){
        //首先要获取要参与计算的m值，即实际有效的复习次数
        //*是否有效需根据复习时的记忆存量是否在所设的阈限之上来确定，因而应当在Log中予以记录，
        // 如果利用历史记录逐一检验就太复杂了


        //获取当前时间和最后一条Log之间的时间间隔(这个只需要值最大的一条即可，不必管什么有效不有效)

        //代入函数计算得记忆存量。

    }

    private double baseCalculate(float alpha, float beta, int fakeNum, int realNum, int timeInterval){
        double m1 = 100*(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum));//分子部分
        double m2 = timeInterval+(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum))-1;//分母部分
        double retainingMemory = m1/m2;
        BigDecimal rM_BD = new BigDecimal(retainingMemory);
        return rM_BD.setScale(1,BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /*
    * 用于刷新retainingAmount和memoryStage字段；
    * 从而用于Rv-UI显示的变更。
    * */
    public void refreshRM(){

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

    public ArrayList<Long> getLearningLogs() {
        return learningLogs;
    }

    public void setLearningLogs(ArrayList<Long> learningLogs) {
        this.learningLogs = learningLogs;
    }

    public byte getRM_Amount() {
        return RM_Amount;
    }

    public void setRM_Amount(byte RM_Amount) {
        this.RM_Amount = RM_Amount;
    }

    public byte getMemoryStage() {
        return memoryStage;
    }

    public void setMemoryStage(byte memoryStage) {
        this.memoryStage = memoryStage;
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
        parcel.writeSerializable(learningLogs);
        parcel.writeByte(RM_Amount);
        parcel.writeByte(memoryStage);
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
        learningLogs = (ArrayList<Long>) in.readSerializable();
        RM_Amount = in.readByte();
        memoryStage = in.readByte();
    }



}
