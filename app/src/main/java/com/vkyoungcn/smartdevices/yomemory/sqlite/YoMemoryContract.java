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
//* 在新的（2018.08.26）版本中，资源结构重新组织为：库/任务/分组/项目
//* 其中库需要从服务器下载，且通常是在需要加载新任务时，作为资源而下载，
// （下载的内容包括：①库信息：对应库表中的一条待增加新记录；②一份数据文件，对应一个新的独立的库资源表xxxItems；
// 然后③还会生成新的独立的 本库所属任务--本库所属items 的交叉表；④一条任务信息）
//    如果只新增一项任务（即其资源库已存在），则只需下载该任务的任务信息（对应mission表的一条新记录）
//*
//  库是新架构下的顶层资源组织节点。
//*  Group是Mission内具体记忆活动的组织单位（承担记录复习时间、计算并显示组内资源记忆效果的职能）
//*  其中Item是程序的基础资源。不同库拥有不同种类的Items资源；库大体按语种区分，库的任务通常也是同一语种的，
// 单项任务不建议跨库；

    private static YoMemoryContract instance;

    static {
        instance = new YoMemoryContract();
    }

//    防止类意外实例化，令构造器为private。
    private YoMemoryContract(){}
    public static YoMemoryContract getInstance(){
        return instance;
    }

//    id列交由BC类自动设为_ID=_id。
    /* 库不需要专门的记录表，一个库对应一个items资源表，相关的对应关系在服务器端进行厂商级维护即可*/
    /* 内置的英语5W词库的items表*/
    public static class EnglishCommonItems implements BaseColumns {
        public static final String TABLE_NAME = "english_common_items";
        public static final String COLUMN_WORD = "word";
        public static final String COLUMN_PHONETIC = "phonetic";
        public static final String COLUMN_DEFINITION = "definition";
        public static final String COLUMN_TRANSLATION = "translation";
//    public static final String COLUMN_TAG = "translation";
         public static final String COLUMN_BNC = "bnc";
         public static final String COLUMN_FRQ = "frq";
//    public static final String COLUMN_DETAIL = "translation";
//    public static final String COLUMN_AUDIO = "translation";
    }


    /*任务表。任务记录是跨资源库存在的，不同资源库的mission可以记录在同一张mission表内*/
    public static class Missions implements BaseColumns {
        public static final String TABLE_NAME = "missions";
        public static final String COLUMN_TITLE ="title";
        public static final String COLUMN_DESCRIPTION = "mission_description";
        public static final String COLUMN_USING_CROSS_TABLE = "using_cross_table";
        public static final String COLUMN_SOURCE_TABLE = "source_table";
        public static final String COLUMN_STAR = "star";//星标标记，数字形式对应不同的图片。
        public static final String COLUMN_IS_SHOWING = "is_showing";
    }

    /*任务-库资源交叉表，含gid、含任务所属item的记录信息*/
    public static class EnglishCross implements BaseColumns {
        public static final String TABLE_NAME = "english_cross";
        public static final String COLUMN_MID ="mid";
        public static final String COLUMN_IID ="iid";
        public static final String COLUMN_GID ="gid";
        public static final String COLUMN_REMOVED ="removed";
        public static final String COLUMN_LEARNED ="learned";
        public static final String COLUMN_PRIORITY ="priority";
        public static final String COLUMN_FAIL_TIMES ="fail_times";
    }

    /* 分组表*/
    public static class Groups implements BaseColumns {
        public static final String TABLE_NAME = "groups";
        public static final String COLUMN_DESCRIPTION ="description";
        public static final String COLUMN_MID ="mid";
        public static final String COLUMN_CREATE_TIME ="create_time";
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


/*  public static class ItemBasic implements BaseColumns {
        public static final String TABLE_NAME = "item_";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PHONETIC = "phonetic";
        public static final String COLUMN_TRANSLATIONS = "translations";

        public static final String COLUMN_IS_CHOSE = "been_chose";
        public static final String COLUMN_IS_LEARNED = "been_learned";//用于“已学百分比”。
        public static final String COLUMN_GROUP_ID = "group_id";//原则上，不允许从属于多个分组
        public static final String COLUMN_PRIORITY = "priority";
        public static final String COLUMN_FAILED_SPELLING_TIMES = "times_failed_spelling";
    }
*/

}
