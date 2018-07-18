package com.vkyoungcn.smartdevices.yomemory.sqlite;

import android.provider.BaseColumns;

/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public final class YoMemoryContract {
//* 是数据库的设计方案类；描述了其中的表和字段结构。
//*
//*  程序共有四种资源Mission、Group、Item、SingleLog（日志）；
//*  Mission是程序的顶层组织单位。
//*  Group是Mission内具体记忆活动的组织单位（承担记录复习时间、计算并显示组内资源记忆效果的职能）
//*  其中Item是程序的基础资源。不同任务可能对应不同的Items资源，
//*  因而Item表可能会存在不止一张，根据（由Mission提供的）不同后缀生成不同表名。

    private static YoMemoryContract instance;

    static {
        instance = new YoMemoryContract();
    }

//    防止类意外实例化，令构造器为private。
    private YoMemoryContract(){}

    public static YoMemoryContract getInstance(){
        return instance;
    }


//    id列交似乎是由BC类自动设为_ID=_id的。
    /*任务表*/
    public static class Mission implements BaseColumns {
        public static final String TABLE_NAME = "missions";
        public static final String COLUMN_NAME ="mission_name";
        public static final String COLUMN_DESCRIPTION = "mission_description";
        public static final String COLUMN_DETAIL_DESCRIPTION = "mission_detail";
        public static final String COLUMN_TABLE_ITEM_SUFFIX = "table_item_suffix";
        public static final String COLUMN_STAR = "star_color";//星标标记，数字形式对应不同的图片。
    }



    /* version 1 */
    /*
     * 分组表
     * 分组的学习记录存储在单独的表中，所含items在Item表中反向记录。
     * 注意，LAST_LEARNING_TIME_LONG、EFFECTIVE_LEARNING_TIMES不需设置，由Logs表计算得来。
     * */
    public static class Group implements BaseColumns {
        public static final String TABLE_NAME = "group_table";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_MISSION_ID = "mission_id";//属于哪个任务
        public static final String COLUMN_SETTING_UP_TIME_LONG = "init_setting_time";//long型（DB整型）；分组。
    }

    /*
    * 用于分组的各次复习时间的记录表
    * 分组：记录 = 1：n
    * */
    public static class LearningLogs implements BaseColumns {
        public static final String TABLE_NAME = "learning_logs";
        public static final String COLUMN_TIME_IN_LONG = "long";
        public static final String COLUMN_GROUP_ID = "group_id";
        public static final String COLUMN_IS_MS_EFFECTIVE = "is_effective";//时间下限的计算只依据MS有效的复习记录来计算。
        //RMA、时间上限的计算只要求是最新的Log，不要Log的类型。
    }//但是，仍然不需要使用专门的LOG类，在DB中将区分后的结果以long列表形式传出即可。




    /*
    * 外语词汇类资源的资源表模板，此类表结构一致，如下；
    * （不同任务会一般对应不同Item表，各表以表名尾缀区分）
    * 记忆时间的序列记录，由分组属性承担，Item不设此项记录。
    * */
    public static class ItemBasic implements BaseColumns {
        public static final String TABLE_NAME = "item_";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PHONETIC = "phonetic";
        public static final String COLUMN_TRANSLATIONS = "translations";

        public static final String COLUMN_IS_CHOSE = "been_chose";
        public static final String COLUMN_IS_LEARNED = "been_learned";//用于“已学百分比”。
        public static final String COLUMN_GROUP_ID = "group_id";//原则上，不允许从属于多个分组
        public static final String COLUMN_PRIORITY = "priority";
        public static final String COLUMN_FAILED_SPELLING_TIMES = "times_failed_spelling";
//        public static final String COLUMN_FAILED_REMINDING_TIMES = "times_failed_reminding";
    }

}
