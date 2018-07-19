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
public class RvMergeGroup implements Parcelable{
// * 一个专用的数据模型类
// * 在发起合并式学习时，需要选择用于合并的来源分组，此一操作中需要对备选分组的部分信息予以显示，
// * 且需携带“是否选中”信息（在DBGroup或RVGroup中都不存在该字段），故而新设专用类。
    private static final String TAG = "RvMergeGroup";

    private boolean isChecked = false;//对应checkBox
    private int id = 0;//DB列（Rv中计划不予显示，但是最后需要作为必要数据后送）
    private int size = 0;//本组所属资源总量、
    private String description = "";


    public RvMergeGroup(RVGroup rvGroup) {
        this.id = rvGroup.getId();
        this.size = rvGroup.getTotalItemsNum();
        this.description = rvGroup.getDescription();
    }

    public RvMergeGroup(DBGroup dbGroup) {
        this.id = dbGroup.getId();
        this.size = dbGroup.getTotalItemNum();
        this.description = dbGroup.getDescription();

    }

    public RvMergeGroup(boolean isChecked, int id, int size, String description) {
        this.isChecked = isChecked;
        this.id = id;
        this.size = size;
        this.description = description;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        //如果不包含引用成员，则浅复制即可
        RvMergeGroup rvMergeGroup = null;
        try {
            rvMergeGroup = (RvMergeGroup) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return rvMergeGroup;

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
        parcel.writeInt(size);
        parcel.writeString(description);
    }

    public static final Parcelable.Creator<RvMergeGroup> CREATOR = new Parcelable.Creator<RvMergeGroup>(){
        @Override
        public RvMergeGroup createFromParcel(Parcel parcel) {
            return new RvMergeGroup(parcel);
        }

        @Override
        public RvMergeGroup[] newArray(int size) {
            return new RvMergeGroup[size];
        }
    };

    private RvMergeGroup(Parcel in){
        isChecked = in.readByte()==1;
        id = in.readInt();
        size = in.readInt();
        description = in.readString();
    }

}
