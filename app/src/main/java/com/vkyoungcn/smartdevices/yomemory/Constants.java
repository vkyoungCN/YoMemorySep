package com.vkyoungcn.smartdevices.yomemory;

/* 信息声明标准格式*/
/*
* 作者：杨胜 @中国海洋大学
* 别名：杨镇时
* author：Victor Young@ Ocean University of China
* email: yangsheng@ouc.edu.cn
* 2018.08.01
* */


public interface Constants {
//用于同一记录程序中出现的（部分）常量
//主要是用于跨类传递的Bundle数据标签常量（目的在于减少笔误出错）
//
//各Activity内的MESSAGE常量依然由各Activity独立维护；
//各类、接口必须的自定义常量仍然由各类、各接口自行维护。


    /* Bundle TAGs */
    public static final String STR_BUNDLE_FOR_GENERAL = "BUNDLE_FOR_GENERAL";
    public static final String STR_BUNDLE_FOR_MISSION = "BUNDLE_FOR_MISSION";
    public static final String STR_BUNDLE_FOR_MERGE ="BUNDLE_FOR_MERGE";  public static final String STR_GROUP = "GROUP";
    public static final String STR_DEFAULT_MANNER_SETTINGS ="DEFAULT_MANNER_SETTINGS";
    public static final String STR_DEFAULT_MANNER_R_SETTINGS = "DEFAULT_MANNER_R_SETTINGS";
    public static final String STR_DESCRIPTION ="DESCRIPTION";
    public static final String STR_FAST_R_MANNER = "FAST_R_MANNER";

    public static final String STR_GIDS_FOR_MERGE = "GIDS_FOR_MERGE";
    public static final String STR_GROUP_ID = "GROUP_ID";
    public static final String STR_GROUP_ID_FOR_MERGE ="GROUP_ID_FOR_MERGE";
    public static final String STR_GROUP_ID_TO_JUMP = "GROUP_ID_TO_JUMP";
    public static final String STR_GROUP_ID_TO_LEARN = "GROUP_ID_TO_LEARN";
    public static final String STR_GROUP_SIZE = "GROUP_SIZE";
    public static final String STR_GROUPS = "GROUPS";

    public static final String STR_EMPTY_ITEMS_POSITIONS = "EMPTY_ITEMS_POSITIONS";
    public static final String STR_IDS_GROUPS_READY_TO_MERGE = "IDS_GROUPS_READY_TO_MERGE";
    public static final String STR_IS_FIRST_LAUNCH = "STR_IS_FIRST_LAUNCH";
    public static final String STR_IS_ORDER = "IS_ORDER";
    public static final String STR_ITEMS = "ITEMS";
    public static final String STR_ITEMS_FOR_LEARNING = "ITEMS_FOR_LEARNING";

    public static final String STR_LEARNING_TYPE = "LEARNING_TYPE";
    public static final String STR_MISSION = "MISSION";
    public static final String STR_MISSION_ID ="MISSION_ID";
    public static final String STR_NO_MORE_BOX = "NO_MORE_BOX";

    public static final String STR_POSITION = "POSITION";
    public static final String STR_PRIORITY_SETTING = "PRIORITY_SETTING";
        public static final String STR_REST_MINUTES = "REST_MINUTES";
    public static final String STR_REST_SECONDS = "REST_SECONDS";
    public static final String STR_START_TIME = "START_TIME";
    public static final String STR_STR_PERCENTAGE = "STR_PERCENTAGE";
    public static final String STR_TABLE_NAME_SUFFIX = "TABLE_NAME_SUFFIX";
    public static final String STR_TABLE_SUFFIX="TABLE_SUFFIX";

    public static final String STR_WRONG_ITEMS_POSITIONS = "WRONG_ITEMS_POSITIONS";

    public static final String STR_DEFAULT_MANNER = "DEFAULT_MANNER";
    public static final String STR_NO_MORE_R_BOX = "NO_MORE_R_BOX";
    public static final String STR_WRONG_AMOUNT="WRONG_AMOUNT";
    public static final String STR_FINISH_AMOUNT ="FINISH_AMOUNT";
    public static final String STR_TOTAL_AMOUNT="TOTAL_AMOUNT";
    public static final String STR_EMPTY_AMOUNT = "EMPTY_AMOUNT";
    public static final String STR_TOTAL_NUM = "TOTAL_NUM";
    public static final String STR_DONE_NUM="DONE_NUM";
    public static final String STR_EMPTY_NUM = "EMPTY_NUM";
    public static final String STR_CORRECT_NUM = "CORRECT_NUM";
    public static final String STR_WRONG_NUM="WRONG_NUM";
    public static final String STR_NEW_GROUP="NEW_GROUP";
    public static final String STR_WRONG_NAMES="WRONG_NAMES";
    public static final String STR_NEW_RMA="NEW_RMA";
    public static final String STR_OLD_RMA="OLD_RMA";
    public static final String STR_NEW_MS="NEW_MS";
    public static final String STR_OLD_MS="OLD_MS";
    public static final String STR_IS_MS_UP="IS_MS_UP";
    public static final String STR_IS_TOO_LATE="IS_TOO_LATE";
    public static final String STR_TERM_MS = "KEY_N";
    public static final String STR_TERM_AMOUNT ="KEY_M";
    public static final String STR_RV_MERGE_GROUP = "RV_MERGE_GROUP";
    public static final String STR_NEW_MS_FOR_FETCH = "NEW_MS_FOR_FETCH";
    public static final String STR_FIXED_CHOSED_GROUP = "STR_FIXED_CHOSED_GROUP";



    /* Date类Pattern字串 */
    public static final String STR_DATE_PATTEN_1 = "yyyy-MM-dd HH:mm:ss";

    /* Fragment TAGs */
    public static final String FG_STR_CREATE_GROUP = "CREATE_GROUP";
    public static final String FG_STR_FAST_LEARN = "FAST_LEARN";
    public static final String FG_STR_FAST_RE_PICK ="FAST_RE_PICK";
    public static final String FG_STR_GIVE_EXPLANATION = "GIVE_EXPLANATION";
    public static final String FG_STR_HANDY_FINISH = "HANDY_FINISH";

    public static final String FG_STR_LEARNING_ADD_IN_ORDER="LEARNING_ADD_IN_ORDER";
    public static final String FG_STR_LEARNING_ADD_RANDOM = "LEARNING_ADD_RANDOM";
    public static final String FG_STR_LESS_IN_GD_DIA = "LESS_IN_GD_DIA";

    public static final String FG_STR_READY_TO_LEARN_GEL = "READY_TO_LEARN_GEL";
    public static final String FG_STR_READY_TO_LEARN_LESS = "READY_TO_LEARN_LESS";
    public static final String FG_STR_READY_TO_LEARN_MERGE ="READY_TO_LEARN_MERGE";

    public static final String FG_STR_SHOW_LOGS = "SHOW_LOGS";
    public static final String FG_STR_TIME_UP_FINISH ="TIME_UP_FINISH";


    public static final String FG_STR_DELETE_GROUP ="DELETE_GROUP";
    public static final String FG_STR_;

    /* SharedPreferences */
    public static final String SP_STR_TITLE_YO_MEMORY = "SP_STR_TITLE_YO_MEMORY";
    public static final String SP_STR_BTN_EXPLAIN_CLICKED = "BTN_EXPLAIN_CLICKED";
    public static final String SP_STR_IS_ORDER = "IS_ORDER";
    public static final String SP_STR_NO_MORE_TIPS ="NO_MORE_TIPS";

    public static final String SP_FAST_R_MANNER = "FAST_R_MANNER";
    public static final String SP_NO_MORE_R_TIPS ="NO_MORE_R_TIPS";
    public static final String SP_;
    public static final String SP_;




}
