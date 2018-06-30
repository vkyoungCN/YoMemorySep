package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.os.Bundle;

public interface OnGeneralDfgInteraction {
    int LEARNING_GENERAL = 2021;
    int LEARNING_AND_CREATE_ORDER = 2022;
    int LEARNING_AND_CREATE_RANDOM = 2023;
    int LEARNING_AND_MERGE =2024;

    int JUMP_TO_GROUP_LIST_THIS_FRAG = 2027;

    int TIME_UP_CONFIRM_DIVIDE = 2091;
    int TIME_UP_CONFIRM_ADD_ERR = 2092;
    int TIME_UP_DISCARD = 2093;


    void onButtonClickingDfgInteraction(int dfgType, Bundle data);
}
