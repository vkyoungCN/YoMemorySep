package com.vkyoungcn.smartdevices.yomemory.sqlite;

import android.provider.BaseColumns;


/**
 * Created by VkYoung16 on 2018/3/26 0026.
 * 是数据库的设计方案类；描述了其中的表和字段结构。
 */

public final class YoMemoryContract {
    private static YoMemoryContract instance;

    static {
        instance = new YoMemoryContract();
    }

//    防止类意外实例化，令构造器为private。
    private YoMemoryContract(){}

    public static YoMemoryContract getInstance(){
        return instance;
    }

     /*
     *  程序共有三种资源Mission、Group、Item；
     *  其中Item是程序的资源，根据任务的数量，Item会存在不止一张表，其表名后半部分动态生成。
     *  此外还有任务的学习时间记录表。
     * */


//    id列交由DB自动负责。
    /*任务表*/
    public static class Mission implements BaseColumns {
        public static final String TABLE_NAME = "missions";
        public static final String COLUMN_NAME ="mission_name";
        public static final String COLUMN_DESCRIPTION = "mission_description";
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
        public static final String COLUMN_IS_EFFECTIVE = "is_effective";
    }

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
