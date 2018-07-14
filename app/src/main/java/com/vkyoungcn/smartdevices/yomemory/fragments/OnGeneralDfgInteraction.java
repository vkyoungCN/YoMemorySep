package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.os.Bundle;

public interface OnGeneralDfgInteraction {
    int LEARNING_GENERAL = 2021;
    int LEARNING_AND_CREATE_ORDER = 2022;
    int LEARNING_AND_CREATE_RANDOM = 2023;
    int LEARNING_AND_MERGE =2024;

    int JUMP_TO_GROUP_LIST_THIS_FRAG = 2027;

    int LEARNING_FINISH_DFG_CONFIRM = 2101;
    int LEARNING_FINISH_DFG_BACK = 2102;
    int LEARNING_FINISH_DFG_GIVE_UP = 2103;

    int LC_FINISH_AMOUNT_ZERO =2121;//对应于结束dfg中，完成数量实际为0（从而不生成新组）的情况

    int DELETE_GROUP = 2221;
    int CREATE_GROUP = 2222;

    int LEARNING_TIME_UP_DFG_CONFIRM = 2111;
    int LEARNING_TIME_UP_DFG_GIVE_UP = 2112;


    void onButtonClickingDfgInteraction(int dfgType, Bundle data);
}
