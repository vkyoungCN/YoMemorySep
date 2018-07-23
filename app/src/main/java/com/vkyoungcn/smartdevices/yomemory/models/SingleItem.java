package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class SingleItem implements Parcelable {
//* Item资源。

    /* 字段 */
    private int id =0;
    private String name="";
    private String phonetic ="";
    private String translations ="";

    private boolean isChose=false;//已被分组抽取过的词汇置true，不会被其他分组再次抽取。
    private boolean isLearned=false;//已被分组抽取过的词汇置true，不会被其他分组再次抽取。
    private int groupId =0;
    private short priority = 2;//初始2，数字越大越重要。当优先级在7以下时，每拼错、记错一次+1；可以手动调节数值。
    private short failedSpelling_times = 0;//本词汇迄今拼写错误总次数。自动记录，点击翻面查看原词时，当次复习置拼错，总量+1；但一次复习中的多次拼错只记录1次。

    public SingleItem() {
    }

    public SingleItem(int id, String name, String phonetic, String translations, boolean isChose, int groupId, boolean isLearned, short priority, short failedSpelling_times) {
        this.id = id;
        this.name = name;
        this.phonetic = phonetic;
        this.translations = translations;
        this.isChose = isChose;
        this.groupId = groupId;
        this.isLearned = isLearned;
        this.priority = priority;
        this.failedSpelling_times = failedSpelling_times;
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

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getTranslations() {
        return translations;
    }

    public void setTranslations(String translations) {
        this.translations = translations;
    }

    public boolean isChose() {
        return isChose;
    }

    public void setChose(boolean chose) {
        isChose = chose;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public boolean isLearned() {
        return isLearned;
    }

    public void setLearned(boolean learned) {
        isLearned = learned;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public short getFailedSpelling_times() {
        return failedSpelling_times;
    }

    public void setFailedSpelling_times(short failedSpelling_times) {
        this.failedSpelling_times = failedSpelling_times;
    }
    public void failSpellingSelfAddOne(){
        this.failedSpelling_times++;
    }

    public String printSelf(){
        String isChoseInStr = isChose?"是":"否";
        String isLearnedInStr = isLearned?"是":"否";
        return "id:"+id+", "+
                "名称："+name+", \n"+
                "发音："+phonetic+", \n"+
                "汉义："+translations+", \n\n"+
                "是否已被选取："+isChoseInStr+", \n"+
                "属于哪个分组(id)："+groupId+", \n"+
                "是否已学习："+isLearnedInStr+", \n"+
                "优先级(2级为默认)："+priority+", \n"+
                "错误次数："+failedSpelling_times;
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
        parcel.writeString(phonetic);
        parcel.writeString(translations);

        parcel.writeByte((byte)( isChose?1:0));
        parcel.writeByte((byte)( isLearned?1:0));
        parcel.writeInt(groupId);
        parcel.writeInt(priority);
        parcel.writeInt(failedSpelling_times);
    }

    public static final Parcelable.Creator<SingleItem> CREATOR = new Parcelable.Creator<SingleItem>(){
        @Override
        public SingleItem createFromParcel(Parcel parcel) {
            return new SingleItem(parcel);
        }

        @Override
        public SingleItem[] newArray(int size) {
            return new SingleItem[size];
        }
    };

    private SingleItem(Parcel in){
        id = in.readInt();
        name = in.readString();
        phonetic = in.readString();
        translations = in.readString();

        isChose = in.readByte()==1;
        isLearned = in.readByte()==1;
        groupId = in.readInt();
        priority = (short) in.readInt();
        failedSpelling_times = (short) in.readInt();
    }


}
