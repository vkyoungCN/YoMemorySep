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
public class RvMergeGroup extends RVGroup implements Parcelable{
// * 一个专用的数据模型类
// * 在发起合并式学习时，需要选择用于合并的来源分组，此一操作中需要对备选分组的部分信息予以显示，
// * 且需携带“是否选中”信息（在DBGroup或RVGroup中都不存在该字段），故而新设专用类。
    private static final String TAG = "RvMergeGroup";

    private boolean isChecked = false;//对应checkBox
//    private int totalItemsNum = 0;//本组所属资源总量、
//    private String description = "";


    public RvMergeGroup(int id, String description, int mission_id, long settingUptimeInLong, long lastLearningTime, byte effectiveRePickingTimes, short totalItemsNum, float RM_Amount, byte memoryStage, boolean isChecked) {
        super(id, description, mission_id, settingUptimeInLong, lastLearningTime, effectiveRePickingTimes, totalItemsNum, RM_Amount, memoryStage);
        this.isChecked = isChecked;
    }

    public RvMergeGroup(RVGroup group) {
        this.id = group.getId();
        this.description = group.getDescription();
        this.mission_id = group.getMission_id();
        this.settingUptimeInLong = group.getSettingUptimeInLong();
        this.lastLearningTime = group.getLastLearningTime();//后面会使用，注意顺序哦。
        this.memoryStage = group.getEffectiveRePickingTimes() ;//后一句会使用，注意顺序哦。
        this.totalItemsNum = group.getTotalItemsNum();
        this.RM_Amount = group.getRM_Amount();

    }

    public RvMergeGroup(Group group) {
        this.id = group.getId();
        this.description = group.getDescription();
        this.mission_id = group.getMission_id();
        this.settingUptimeInLong = group.getSettingUptimeInLong();
        this.lastLearningTime = group.getLastLearningTime();//后面会使用，注意顺序哦。
        this.memoryStage = group.getEffectiveRePickingTimes() ;//后一句会使用，注意顺序哦。
        this.totalItemsNum = group.getTotalItemsNum();
        //此版本下没有Rma字段值。
    }

    public RvMergeGroup(boolean isChecked, int id, int size, String description) {
        this.isChecked = isChecked;
        this.id = id;
        this.totalItemsNum = (short) size;
        this.description = description;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    @Override
    public Object clone() {
        //如果不包含引用成员，则浅复制即可
        RvMergeGroup rvMergeGroup = null;
        try {
            rvMergeGroup = (RvMergeGroup) super.clone();
        } catch (Exception e) {
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
        parcel.writeInt(totalItemsNum);
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
        totalItemsNum = in.readInt();
        description = in.readString();
    }

}
