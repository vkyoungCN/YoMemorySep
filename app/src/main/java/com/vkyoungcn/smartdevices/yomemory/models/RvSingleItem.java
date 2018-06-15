package com.vkyoungcn.smartdevices.yomemory.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by VkYoung16 on 2018/3/26 0026.
 */

public class RvSingleItem implements Parcelable {
    private int id =0;
    private String name="";
    private String phonetic ="";
    private String translations ="";

    private String isChose= "否";//与DB版不同的字段1.
    private String isLearned="否";//与DB版不同的字段2.
    private int groupId =0;
    private short priority = 2;//初始2，数字越大越重要。当优先级在7以下时，每拼错、记错一次+1；可以手动调节数值。
    private short failedSpelling_times = 0;//本词汇迄今拼写错误总次数。自动记录，点击翻面查看原词时，当次复习置拼错，总量+1；但一次复习中的多次拼错只记录1次。

    public RvSingleItem(int id, String name, String phonetic, String translations, String isChose, String isLearned, int groupId, short priority, short failedSpelling_times) {
        this.id = id;
        this.name = name;
        this.phonetic = phonetic;
        this.translations = translations;
        this.isChose = isChose;
        this.isLearned = isLearned;
        this.groupId = groupId;
        this.priority = priority;
        this.failedSpelling_times = failedSpelling_times;
    }

    public RvSingleItem(SingleItem singleItem){
        this.id = singleItem.getId();
        this.name = singleItem.getName();
        this.phonetic = singleItem.getPhonetic();
        this.translations = singleItem.getTranslations();
        this.isChose = singleItem.isChose()?"是":"否";
        this.isLearned = singleItem.isLearned()?"是":"否";
        this.groupId = groupId;
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

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getIsChose() {
        return isChose;
    }

    public void setIsChose(String isChose) {
        this.isChose = isChose;
    }

    public String getIsLearned() {
        return isLearned;
    }

    public void setIsLearned(String isLearned) {
        this.isLearned = isLearned;
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

        parcel.writeString(isChose);
        parcel.writeString(isLearned);
        parcel.writeInt(groupId);
        parcel.writeInt(priority);
        parcel.writeInt(failedSpelling_times);
    }

    public static final Creator<RvSingleItem> CREATOR = new Creator<RvSingleItem>(){
        @Override
        public RvSingleItem createFromParcel(Parcel parcel) {
            return new RvSingleItem(parcel);
        }

        @Override
        public RvSingleItem[] newArray(int size) {
            return new RvSingleItem[size];
        }
    };

    private RvSingleItem(Parcel in){
        id = in.readInt();
        name = in.readString();
        phonetic = in.readString();
        translations = in.readString();

        isChose = in.readString();
        isLearned = in.readString();
        groupId = in.readInt();
        priority = (short) in.readInt();
        failedSpelling_times = (short) in.readInt();
    }


}
