package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.os.Bundle;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public interface OnGeneralDfgInteraction {
//* DialogFragment与调用方Activity之间交互（从DFG向ACT回送数据）的通用（自定义）接口
//* 回调方法中传回两个数据：①本操作的类型（是下列定义的操作类型常量之一）；②本操作附带的数据（
// 以Bundle形式发回。）

    int LEARNING_GENERAL = 2021;
    int LEARNING_GENERAL_NO_GID = 2029;//快速复习模式下特殊信号，通常的LG是提前准备好目标gid发送
    // 给LPA的，而快速复习可能需要根据timeThreshold筛选最需要复习的组，该工作需由LPA负责，因而暂无gid发送。
    int LEARNING_AND_CREATE_ORDER = 2022;
    int LEARNING_AND_CREATE_RANDOM = 2023;
    int LEARNING_AND_MERGE =2024;

    int JUMP_TO_GROUP_LIST_THIS_FRAG = 2027;

    int LEARNING_FINISH_DFG_CONFIRM = 2101;
    int LEARNING_FINISH_DFG_BACK = 2102;
    int LEARNING_FINISH_DFG_GIVE_UP = 2103;

    int LEARNING_TIME_UP_DFG_CONFIRM = 2111;
    int LEARNING_TIME_UP_DFG_GIVE_UP = 2112;

    int LC_FINISH_AMOUNT_ZERO =2121;//对应于结束dfg中，完成数量实际为0（从而不生成新组）的情况

    int DELETE_GROUP = 2221;
    int CREATE_GROUP = 2222;

    int FAST_LEARN = 2231;
    int FAST_RE_PICK = 2232;

    int FETCH_NEW_GROUPS_INFO_FOR_MERGE = 2151;


    void onButtonClickingDfgInteraction(int dfgType, Bundle data);
}
