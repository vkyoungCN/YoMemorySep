package com.vkyoungcn.smartdevices.yomemory.fragments;

import android.os.Bundle;

public interface OnLearningConfirmDfgInteraction {
    int LEARNING_GENERAL = 2021;
    int LEARNING_AND_CREATE_ORDER = 2022;
    int LEARNING_AND_CREATE_RANDOM = 2023;
    int LEARNING_AND_MERGE =2024;

    int JUMP_TO_GROUP_LIST_THIS_FRAG = 2027;
    void onLearningConfirmDfgInteraction(int dfgType,Bundle data);
}
