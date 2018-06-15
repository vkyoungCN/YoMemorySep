package com.vkyoungcn.smartdevices.yomemory.spiralCore;

import android.content.Context;

import com.vkyoungcn.learningtools.R;
import com.vkyoungcn.learningtools.spiralCore.GroupState.stateNumber;
import com.vkyoungcn.learningtools.sqlite.YouMemoryDbHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/*
* 是对应一组任务的所有已完成记忆的Log的集合
* 日志记录数字从0开始：0 是建组，1是初学，复习从2起。】
* */
public class LogList {
    private static final String TAG = "LogList";


    /*
    * 根据当前时间，计算指定任务分组当前所处的颜色状态，以及到下一阶段还剩多长时间；
    *
    * 设计思路是：
    * ①前24小时是复习黄金时间，复习密度高；（而且前72小时内可以加班记忆）；
    * ②24小时内规则较复杂
    * ③24小时以上规则较为简单，但大体都是在某很宽泛的时段内可以进行某次复习；
    * 如果超过时限未执行复习，记Miss一次。
    *
    * 时间计算因素可以修改，即参数baseTimeFactor（初始设计为90）
    * 最大复习次数可修改，初始设计为12次。
    *
    *
    * 超过某复习时段允许的时间区间后，任务分组界面无法再进入“执行复习”界面，从而限制复习活动
    * （group详情界面会每隔一分钟进行刷新）。如果能进入复习（哪怕在最后一分钟），由于已进入到
    * 新的界面，如果能在合理时间内完成复习，则可按合理的时间予以记录存入复习Log。
    * */
    //【注意，待】虽然向DB写数据应在onStop处进行，但应该在时间超过阶段节点时先将更新的Log-miss记录
    //附加给Group Manager，以免旧数据影响计算。
    //【待2】应该在时段截至前10或20分钟停止计时，不再等待复习，否则时间不足无法完成复习的。
    public static GroupState getGroupStateBaseOnGroupLogs(String strLogs) {
        GroupState groupState = new GroupState();
        long timeAmountMinutes ;
        int maxRePickingTimes = 12;//最大复习次数，当前设置为12，最后复习时间约在4~8个。
        int n ;

        if (strLogs == null || strLogs.isEmpty()) {
            groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);//待处理的错误情况
            return groupState;
        }
        String[] strLogsArray = strLogs.split(";");//每个元素是一条形如N#yyyy-MM-dd HH:mm:ss#false;的记录
        if(strLogsArray.length == 1){
            groupState.setColorResId(GroupState.ColorResId.COLOR_NEWLY);//刚建组，初学
            return groupState;
        }
        SingleLog firstLog = new SingleLog(strLogsArray[1]);//所有情形都要和初次学习时间做比较计算
        timeAmountMinutes = (System.currentTimeMillis() - firstLog.getTimeInMilli()) / (1000 * 60);//当前时间和初次学习时间相比，已过去多久
        n = strLogsArray.length-1;

        //n=1的计算与其他不同，直接手写；其他条件下函数式判断。
        if (n == 1) {
            int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

            long n1_timeRemainingStageI = 30 - timeAmountMinutes;//距“可以进行R30min次的复习”还有多久
            long n1_timeRemainingStageII = 60 - timeAmountMinutes;//距“错过R30min次的复习”还有多久
            long n1_timeRemainingStageIII = (4 * baseTimeFactor ) - timeAmountMinutes;//360-tAM，距“可以进行R2次复习”还有多久
            long n1_timeRemainingStageIV = (8 * baseTimeFactor ) - timeAmountMinutes;//720-tAM，距“错过R2次复习”还有多久
            long n1_timeRemainingStageV = (16 * baseTimeFactor ) - timeAmountMinutes;//1440-tAM，距“错过R3次复习”还有多久

            if (n1_timeRemainingStageI > 0) {
                groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                groupState.setRemainingMinutes((short) n1_timeRemainingStageI);
                groupState.setRemainingHours((short) 0);
                groupState.setRemainingDays((short) 0);
            } else if (n1_timeRemainingStageII > 0) {//位于[30,60)半开半闭区间内，可以执行首次复习
                groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                groupState.setRemainingMinutes((short) n1_timeRemainingStageII);
                groupState.setRemainingHours((short) 0);
                groupState.setRemainingDays((short) 0);
            } else if (n1_timeRemainingStageIII > 0) {//初记后，不到6小时
                //只有初记记录，且时间已超过1小时（待，处理与开始复习的2小时时限相容问题），说明
                //是错过了第一次（+30min）的复习，但此次不计算缺失。
                groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);//仍然算未到时间
                groupState.setRemainingMinutes((short) (n1_timeRemainingStageIII % 60));
                groupState.setRemainingHours((short) (n1_timeRemainingStageIII / 60));
                groupState.setRemainingDays((short) 0);
            } else if (n1_timeRemainingStageIV > 0) {//初记后，6小时~12小时之间，应执行第二次复习R2。
                groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                groupState.setRemainingMinutes((short) (n1_timeRemainingStageIV % 60));
                groupState.setRemainingHours((short) (n1_timeRemainingStageIV / 60));
                groupState.setRemainingDays((short) 0);
            } else if (n1_timeRemainingStageV > 0) {//初记后，12~24小时之间，应执行第三次复习R3，且记丢失一次。
                groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                groupState.setRemainingMinutes((short) (n1_timeRemainingStageV % 60));
                groupState.setRemainingHours((short) (n1_timeRemainingStageV / 60));
                groupState.setRemainingDays((short) 0);
            } else {//超过一天没复习，凉了。
                groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                groupState.setRemainingMinutes((short) 0);
                groupState.setRemainingHours((short) 0);
                groupState.setRemainingDays((short) 0);
            }
            return groupState;
        }

        if (n >= maxRePickingTimes) {
            //完成设置的最大复习次数，复习完成
            groupState.setColorResId(GroupState.ColorResId.COLOR_FULL);
            groupState.setRemainingMinutes((short) 0);
            groupState.setRemainingHours((short) 0);
            groupState.setRemainingDays((short) 0);
        }


        int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

        long timeRemainingStageI =(baseTimeFactor * 2 ^ n) - timeAmountMinutes;//距“可以进行下次复习”还有多久
        long timeRemainingStageII = (2 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过第一次复习”还有多久
        long timeRemainingStageIII = (4 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过两次复习”还有多久

        byte minuteReminder;
        byte hourReminder;
        byte dayReminder;

        //以下是有两条及以上日志，除n=2 略有不同外，其余规则一致
        if (timeAmountMinutes < 60) {//此时间段内的复习已完成。
            groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
            groupState.setRemainingMinutes((short) 0);
            groupState.setRemainingHours((short) 0);
            groupState.setRemainingDays((short) 0);
        } else if (timeRemainingStageI > 0) {
            groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);

            minuteReminder = (byte) (timeRemainingStageI % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageI / (60 * 24));

            groupState.setRemainingMinutes(minuteReminder);
            groupState.setRemainingHours(hourReminder);
            groupState.setRemainingDays(dayReminder);
        } else if (timeRemainingStageII > 0) {
            groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);

            minuteReminder = (byte) (timeRemainingStageII % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageII / (60 * 24));

            groupState.setRemainingMinutes(minuteReminder);
            groupState.setRemainingHours(hourReminder);
            groupState.setRemainingDays(dayReminder);
        } else if (timeRemainingStageIII > 0) {
            groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);

            minuteReminder = (byte) (timeRemainingStageIII % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageIII / (60 * 24));

            groupState.setRemainingMinutes(minuteReminder);
            groupState.setRemainingHours(hourReminder);
            groupState.setRemainingDays(dayReminder);
        } else {//太久没有复习，凉了。
            groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
            groupState.setRemainingMinutes((short) 0);
            groupState.setRemainingHours((short) 0);
            groupState.setRemainingDays((short) 0);
        }
        return groupState;
    }

    /*
    * 根据groupLogs返回对应的ColorResource，是前一方法的简化版*/
    public static int getCurrentColorResourceForGroup(String strLogs) {

        long timeAmountMinutes ;
        int maxRePickingTimes = 12;//最大复习次数，当前设置为12，最后复习时间约在4~8个。
        int n ;
        if (strLogs == null || strLogs.isEmpty()) {
            return GroupState.ColorResId.COLOR_MISSED_TWICE;
        }
        String[] strLogsArray = strLogs.split(";");//每个元素是一条形如N#yyyy-MM-dd HH:mm:ss#false;的记录
        if(strLogsArray.length == 1){
            return GroupState.ColorResId.COLOR_NEWLY;
        }
        SingleLog firstLog = new SingleLog(strLogsArray[1]);//所有情形都要和初次学习时间做比较计算

        long currLong = System.currentTimeMillis();
        timeAmountMinutes = (currLong - firstLog.getTimeInMilli()) / (1000 * 60);//当前时间和初次学习时间相比，已过去多久
        n = strLogsArray.length-1;

        //n=1的计算与其他不同，直接手写；其他条件下函数式判断。
        if (n == 1) {
            int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

            long n1_timeRemainingStageI = 30 - timeAmountMinutes;//距“可以进行R30min次的复习”还有多久
            long n1_timeRemainingStageII = 60 - timeAmountMinutes;//距“错过R30min次的复习”还有多久
            long n1_timeRemainingStageIII = (4 * baseTimeFactor ) - timeAmountMinutes;//360-tAM，距“可以进行R2次复习”还有多久
            long n1_timeRemainingStageIV = (8 * baseTimeFactor ) - timeAmountMinutes;//720-tAM，距“错过R2次复习”还有多久
            long n1_timeRemainingStageV = (16 * baseTimeFactor ) - timeAmountMinutes;//1440-tAM，距“错过R3次复习”还有多久

            if (n1_timeRemainingStageI > 0) {
                return GroupState.ColorResId.COLOR_STILL_NOT;
            } else if (n1_timeRemainingStageII > 0) {//位于[30,60)半开半闭区间内，可以执行首次复习
                return GroupState.ColorResId.COLOR_AVAILABLE;
            } else if (n1_timeRemainingStageIII > 0) {//初记后，不到6小时
                return GroupState.ColorResId.COLOR_STILL_NOT;//仍然算未到时间
            } else if (n1_timeRemainingStageIV > 0) {//初记后，6小时~12小时之间，应执行第二次复习R2。
                return GroupState.ColorResId.COLOR_AVAILABLE;
            } else if (n1_timeRemainingStageV > 0) {//初记后，12~24小时之间，应执行第三次复习R3，且记丢失一次。
                return GroupState.ColorResId.COLOR_MISSED_ONCE;
            } else {//超过一天没复习，凉了。
                return GroupState.ColorResId.COLOR_MISSED_TWICE;
            }
        }

        if (n >= maxRePickingTimes) {
            //完成设置的最大复习次数，复习完成
            return GroupState.ColorResId.COLOR_FULL;
        }

        int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

        long timeRemainingStageI =(baseTimeFactor * 2 ^ n) - timeAmountMinutes;//距“可以进行下次复习”还有多久
        long timeRemainingStageII = (2 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过第一次复习”还有多久
        long timeRemainingStageIII = (4 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过两次复习”还有多久

        //以下是有两条及以上日志，除n=2 略有不同外，其余规则一致
        if (timeAmountMinutes < 60) {//此时间段内的复习已完成。
            return GroupState.ColorResId.COLOR_STILL_NOT;
        } else if (timeRemainingStageI > 0) {
            return GroupState.ColorResId.COLOR_STILL_NOT;
        } else if (timeRemainingStageII > 0) {
            return GroupState.ColorResId.COLOR_AVAILABLE;
        } else if (timeRemainingStageIII > 0) {
            return GroupState.ColorResId.COLOR_MISSED_ONCE;
        } else {//太久没有复习，凉了。
            return GroupState.ColorResId.COLOR_MISSED_TWICE;
        }
    }

    /*
     * 根据groupLogs返回对应的state整型数，是简化版*/
    public static GroupState.stateNumber getCurrentStateIntegerForGroup(String strLogs) {

        long timeAmountMinutes ;
        int maxRePickingTimes = 12;//最大复习次数，当前设置为12，最后复习时间约在4~8个。
        int n ;
        if (strLogs == null || strLogs.isEmpty()) {//新建分组，还没开始学习。尽快开始学习。
            //错误情况，【暂未处理】
            return stateNumber.MISSED_TWICE;
        }
        String[] strLogsArray = strLogs.split(";");//每个元素是一条形如N#yyyy-MM-dd HH:mm:ss#false;的记录
        if(strLogsArray.length ==1){
            return stateNumber.NEWLY_CREATED;
        }
        SingleLog firstLog = new SingleLog(strLogsArray[1]);//所有情形都要和初次学习时间做比较计算
        timeAmountMinutes = (System.currentTimeMillis() - firstLog.getTimeInMilli()) / (1000 * 60);//当前时间和初次学习时间相比，已过去多久
        n = strLogsArray.length-1;

        //n=1的计算与其他不同，直接手写；其他条件下函数式判断。
        if (n == 1) {
            int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

            long n1_timeRemainingStageI = 30 - timeAmountMinutes;//距“可以进行R30min次的复习”还有多久
            long n1_timeRemainingStageII = 60 - timeAmountMinutes;//距“错过R30min次的复习”还有多久
            long n1_timeRemainingStageIII = (4 * baseTimeFactor ) - timeAmountMinutes;//360-tAM，距“可以进行R2次复习”还有多久
            long n1_timeRemainingStageIV = (8 * baseTimeFactor ) - timeAmountMinutes;//720-tAM，距“错过R2次复习”还有多久
            long n1_timeRemainingStageV = (16 * baseTimeFactor ) - timeAmountMinutes;//1440-tAM，距“错过R3次复习”还有多久

            if (n1_timeRemainingStageI > 0) {
                return stateNumber.NOT_YET;
            } else if (n1_timeRemainingStageII > 0) {//位于[30,60)半开半闭区间内，可以执行首次复习
                return stateNumber.AVAILABLE;
            } else if (n1_timeRemainingStageIII > 0) {//初记后，不到6小时
                return stateNumber.NOT_YET;//仍然算未到时间
            } else if (n1_timeRemainingStageIV > 0) {//初记后，6小时~12小时之间，应执行第二次复习R2。
                return stateNumber.AVAILABLE;
            } else if (n1_timeRemainingStageV > 0) {//初记后，12~24小时之间，应执行第三次复习R3，且记丢失一次。
                return stateNumber.MISSED_ONCE;
            } else {//超过一天没复习，凉了。
                return stateNumber.MISSED_TWICE;
            }
        }

        if (n >= maxRePickingTimes) {
            //完成设置的最大复习次数，复习完成
            return stateNumber.ACCOMPLISHED;
        }

        int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

        long timeRemainingStageI =(baseTimeFactor * 2 ^ n) - timeAmountMinutes;//距“可以进行下次复习”还有多久
        long timeRemainingStageII = (2 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过第一次复习”还有多久
        long timeRemainingStageIII = (4 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes);//距“错过两次复习”还有多久

        //以下是有两条及以上日志，除n=2 略有不同外，其余规则一致
        if (timeAmountMinutes < 60) {//此时间段内的复习已完成。
            return stateNumber.NOT_YET;
        } else if (timeRemainingStageI > 0) {
            return stateNumber.NOT_YET;
        } else if (timeRemainingStageII > 0) {
            return stateNumber.AVAILABLE;
        } else if (timeRemainingStageIII > 0) {
            return stateNumber.MISSED_ONCE;
        } else {//太久没有复习，凉了。
            return stateNumber.MISSED_TWICE;
        }
    }

    /*
    * 根据groupLogs返回对应的剩余时间，含天、小时、分钟三个字段*/
    public static RemainingTimeAmount getCurrentRemainingTimeForGroup(String strLogs) {

        RemainingTimeAmount remainingTimeAmount = new RemainingTimeAmount((byte) 0,(byte) 0,(byte) 0);
        long timeAmountMinutes ;
        int maxRePickingTimes = 12;//最大复习次数，当前设置为12，最后复习时间约在4~8个。
        int n ;

        if (strLogs == null || strLogs.isEmpty()) {//新建分组，还没开始学习。尽快开始学习。
            return remainingTimeAmount;//【实际为错误情况，待处理】
        }
        String[] strLogsArray = strLogs.split(";");//每个元素是一条形如N#yyyy-MM-dd HH:mm:ss#false;的记录
        if(strLogsArray.length ==1){
            return remainingTimeAmount;//初学
        }
        SingleLog firstLog = new SingleLog(strLogsArray[1]);//所有情形都要和初次学习时间做比较计算
        timeAmountMinutes = (System.currentTimeMillis() - firstLog.getTimeInMilli()) / (1000 * 60);//当前时间和初次学习时间相比，已过去多久
        n = strLogsArray.length-1;
//        Log.i(TAG, "getCurrentRemainingTimeForGroup: n="+n);

        //n=1的计算与其他不同，直接手写；其他条件下函数式判断。
        if (n == 1) {
            int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

            long n1_timeRemainingStageI = 30 - timeAmountMinutes;//距“可以进行R30min次的复习”还有多久
            long n1_timeRemainingStageII = 60 - timeAmountMinutes;//距“错过R30min次的复习”还有多久
            long n1_timeRemainingStageIII = (4 * baseTimeFactor ) - timeAmountMinutes;//360-tAM，距“可以进行R2次复习”还有多久
            long n1_timeRemainingStageIV = (8 * baseTimeFactor ) - timeAmountMinutes;//720-tAM，距“错过R2次复习”还有多久
            long n1_timeRemainingStageV = (16 * baseTimeFactor ) - timeAmountMinutes;//1440-tAM，距“错过R3次复习”还有多久

            if (n1_timeRemainingStageI > 0) {
                remainingTimeAmount.setRemainingMinutes((byte) n1_timeRemainingStageI);
                remainingTimeAmount.setRemainingHours((byte) 0);
                remainingTimeAmount.setRemainingDays((byte) 0);
            } else if (n1_timeRemainingStageII > 0) {//位于[30,60)半开半闭区间内，可以执行首次复习
                remainingTimeAmount.setRemainingMinutes((byte) n1_timeRemainingStageII);
                remainingTimeAmount.setRemainingHours((byte) 0);
                remainingTimeAmount.setRemainingDays((byte) 0);
            } else if (n1_timeRemainingStageIII > 0) {//初记后，不到6小时
                //只有初记记录，且时间已超过1小时（待，处理与开始复习的2小时时限相容问题），说明
                //是错过了第一次（+30min）的复习，但此次不计算缺失。
                remainingTimeAmount.setRemainingMinutes((byte) (n1_timeRemainingStageIII % 60));
                remainingTimeAmount.setRemainingHours((byte)(n1_timeRemainingStageIII / 60) );
                remainingTimeAmount.setRemainingDays((byte) 0);
            } else if (n1_timeRemainingStageIV > 0) {//初记后，6小时~12小时之间，应执行第二次复习R2。
                remainingTimeAmount.setRemainingMinutes((byte) (n1_timeRemainingStageIV % 60));
                remainingTimeAmount.setRemainingHours((byte) (n1_timeRemainingStageIV / 60));
                remainingTimeAmount.setRemainingDays((byte) 0);
            } else if (n1_timeRemainingStageV > 0) {//初记后，12~24小时之间，应执行第三次复习R3，且记丢失一次。
                remainingTimeAmount.setRemainingMinutes((byte) (n1_timeRemainingStageV % 60));
                remainingTimeAmount.setRemainingHours((byte) (n1_timeRemainingStageV / 60));
                remainingTimeAmount.setRemainingDays((byte) 0);
            } else {//超过一天没复习，凉了。
                remainingTimeAmount.setRemainingMinutes((byte) 0);
                remainingTimeAmount.setRemainingHours((byte) 0);
                remainingTimeAmount.setRemainingDays((byte) 0);
            }
            return remainingTimeAmount;
        }

        if (n >= maxRePickingTimes) {
            //完成设置的最大复习次数，复习完成
            remainingTimeAmount.setRemainingMinutes((byte) 0);
            remainingTimeAmount.setRemainingHours((byte) 0);
            remainingTimeAmount.setRemainingDays((byte) 0);

            return remainingTimeAmount;
        }


        int baseTimeFactor = 90;//时间因素设置为90分钟（原始效果见下方switch）；

        //以下是有两条及以上日志，除n=2 略有不同外，其余规则一致【由于后来取消对30~60min的复习进行记录，从而n的对应关系需要加1】
        long timeRemainingStageI =(baseTimeFactor * 2 ^ (n+1)) - timeAmountMinutes;//距“可以进行下次复习”还有多久
        long timeRemainingStageII = (2 * (baseTimeFactor * 2 ^ (n+1)) - timeAmountMinutes);//距“错过第一次复习”还有多久
        long timeRemainingStageIII = (4 * (baseTimeFactor * 2 ^ (n+1)) - timeAmountMinutes);//距“错过两次复习”还有多久

        byte minuteReminder = 0;
        byte hourReminder = 0;
        byte dayReminder = 0;
//        Log.i(TAG, "getCurrentRemainingTimeForGroup: TimeAmount="+timeAmountMinutes);
//        Log.i(TAG, "getCurrentRemainingTimeForGroup: timeRemainingStageI = "+timeRemainingStageI);
        if (timeAmountMinutes < 60) {//此时间段内的复习已完成。
            remainingTimeAmount.setRemainingMinutes((byte) 0);
            remainingTimeAmount.setRemainingHours((byte) 0);
            remainingTimeAmount.setRemainingDays((byte) 0);
        } else if (timeRemainingStageI > 0) {
            minuteReminder = (byte) (timeRemainingStageI % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageI / (60 * 24));
//            Log.i(TAG, "getCurrentRemainingTimeForGroup: Remainder M H D:"+ minuteReminder+hourReminder+dayReminder);

            remainingTimeAmount.setRemainingMinutes(minuteReminder);
            remainingTimeAmount.setRemainingHours(hourReminder);
            remainingTimeAmount.setRemainingDays(dayReminder);
        } else if (timeRemainingStageII > 0) {
            minuteReminder = (byte) (timeRemainingStageII % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageII / (60 * 24));

            remainingTimeAmount.setRemainingMinutes(minuteReminder);
            remainingTimeAmount.setRemainingHours(hourReminder);
            remainingTimeAmount.setRemainingDays(dayReminder);
        } else if (timeRemainingStageIII > 0) {
            minuteReminder = (byte) (timeRemainingStageIII % 60);
            hourReminder = (byte) ((timeRemainingStageI /60) % 24);
            dayReminder = (byte) (timeRemainingStageIII / (60 * 24));

            remainingTimeAmount.setRemainingMinutes(minuteReminder);
            remainingTimeAmount.setRemainingHours(hourReminder);
            remainingTimeAmount.setRemainingDays(dayReminder);
        } else {//太久没有复习，凉了。
            remainingTimeAmount.setRemainingMinutes((byte) 0);
            remainingTimeAmount.setRemainingHours((byte) 0);
            remainingTimeAmount.setRemainingDays((byte) 0);
        }
//        Log.i(TAG, "getCurrentRemainingTimeForGroup: H-M-S:"+remainingTimeAmount.getRemainingHours()+" "+remainingTimeAmount.getRemainingMinutes()+" "+remainingTimeAmount.getRemainingMinutes());
        return remainingTimeAmount;
    }



    /* version 2.0 大幅改进，语句缩减到几块，但时间余量还可进一步抽象化。
    //以下是有两条及以上日志，除n=2 略有不同外，其余规则一致
        if (timeAmountMinutes < 60) {//此时间段内的复习已完成。
        groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
        groupState.setRemainingMinutes((short) 0);
        groupState.setRemainingHours((short) 0);
        groupState.setRemainingDays((short) 0);
    } else if (timeAmountMinutes < (baseTimeFactor * 2 ^ n)) {
        groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);

        timeRemainingInMinutes = (((baseTimeFactor * 2 ^ n) - timeAmountMinutes));
        minuteReminder = (byte) (timeRemainingInMinutes % 60);
        hourReminder = (byte) (timeRemainingInMinutes % (60 * 24));
        dayReminder = (byte) (timeRemainingInMinutes / (60 * 24));

        groupState.setRemainingMinutes(minuteReminder);
        groupState.setRemainingHours(hourReminder);
        groupState.setRemainingDays(dayReminder);
    } else if (timeAmountMinutes < (2 * baseTimeFactor * 2 ^ n)) {
        groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);

        timeRemainingInMinutes = ((2 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes));
        minuteReminder = (byte) (timeRemainingInMinutes % 60);
        hourReminder = (byte) (timeRemainingInMinutes % (60 * 24));
        dayReminder = (byte) (timeRemainingInMinutes / (60 * 24));

        groupState.setRemainingMinutes(minuteReminder);
        groupState.setRemainingHours(hourReminder);
        groupState.setRemainingDays(dayReminder);
    } else if (timeAmountMinutes < (4 * baseTimeFactor * 2 ^ n)) {
        groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);

        timeRemainingInMinutes = ((4 * (baseTimeFactor * 2 ^ n) - timeAmountMinutes));
        minuteReminder = (byte) (timeRemainingInMinutes % 60);
        hourReminder = (byte) (timeRemainingInMinutes % (60 * 24));
        dayReminder = (byte) (timeRemainingInMinutes / (60 * 24));

        groupState.setRemainingMinutes(minuteReminder);
        groupState.setRemainingHours(hourReminder);
        groupState.setRemainingDays(dayReminder);
    } else {//太久没有复习，凉了。
        groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
        groupState.setRemainingMinutes((short) 0);
        groupState.setRemainingHours((short) 0);
        groupState.setRemainingDays((short) 0);
    }*/


    /* version 1.0最初设计版本，采用switch，语句特别多。
    switch (strLogsArray.length){//有多少条日志记录
            case 1://只有一次记录，只进行了初次学习。
                if(timeAmountMinutes<30){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short) timeAmountMinutes);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);

                }else if (timeAmountMinutes<60){//位于[30,60)半开半闭区间内，可以执行首次复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)(60-timeAmountMinutes));
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                    //R1的特殊之处还有：
                    //需要在+50min内进入（总+1h内完成）。
                }else if(timeAmountMinutes<360){//初记后，不到6小时
                    //只有初记记录，且时间已超过1小时（待，处理与开始复习的2小时时限相容问题），说明
                    //是错过了第一次（+30min）的复习，但此次不计算缺失。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);//仍然算未到时间
                    groupState.setRemainingMinutes((short)((360-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((360-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<720){//初记后，6小时~12小时之间，应执行第二次复习R2。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)((720-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((720-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<1440){//初记后，12~24小时之间，应执行第三次复习R3，且记丢失一次。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)((1440-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((1440-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else{//超过一天没复习，凉了。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;//case == 1 (Log记录仅有1项)到此完成。

            case 2:  //log.size==2，有两项记录。应该是初记和30分钟记忆；
                n = 2;
                if(timeAmountMinutes<60){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(90*2^n)){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)((360-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((360-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if (timeAmountMinutes<720){//位于[360,720)半开半闭区间内，可以执行第二次复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)((720-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((720-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<1440){//初记后，12小时以上，不到24小时；R2Miss
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)((1440-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)((1440-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else{//超过一天没复习（只在30分钟进行了复习），凉了。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 3:  //log.size==3，最后记录应是R2（第二次复习，6~12h）；
                n = 3;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){//超过12小时，不足24小时，可以进行R3
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)(((180*2^n)-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)(((180*2^n)-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(360*2^n)){//初记后，24小时以上，不到48小时；R3Miss
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)(((360*2^n)-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)(((360*2^n)-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else{//超过48小时，连续MISS两次复习，凉。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 4:  //log.size==4，最后记录应是R3（第三次复习，12~24h）；
                n=4;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){//超过24小时，不足48小时，可以进行R4
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)(((180*2^n)-timeAmountMinutes)%60));
                    groupState.setRemainingHours((short)(((180*2^n)-timeAmountMinutes)/60));
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(360*2^n)){//初记后，48小时以上，不到96小时；R4Miss
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short)((((360*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((360*2^n)-timeAmountMinutes)/(60*24)));
                }else{//超过96小时，连续MISS两次复习，凉。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 5:  //log.size==5，最后记录应是R4（第4次复习，24~48h）；
                n=5;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){//超过48小时，不足96小时，可以进行R5
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else if(timeAmountMinutes<(360*2^n)){//初记后，4天以上，不到8天；R5Miss
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((360*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((360*2^n)-timeAmountMinutes)/(60*24)));
                }else{//超过8天，连续MISS两次复习，凉。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 6:  //log.size==6，最后记录应是R5（第5次复习，2~4天）；
                n=6;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else if(timeAmountMinutes<(360*2^n)){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((360*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((360*2^n)-timeAmountMinutes)/(60*24)));
                }else{//连续MISS两次,凉。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 7:  //log.size==7，最后记录应是R6（第6次复习，4~8天）；
                n=7;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else if(timeAmountMinutes<(360*2^n)){
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((360*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((360*2^n)-timeAmountMinutes)/(60*24)));
                }else{//连续MISS两次,凉。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_TWICE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short) 0);
                }
                break;

            case 8:  //log.size==8，最后记录应是R7（第7次复习，8~16天）；
                n=8;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short) 0);
                }else if(timeAmountMinutes<(180*2^n)){//1个月之内都可以进行复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else{//超过1个月没复习，Miss第8次但此时已记忆多次不会再凉，后面都标橙色，不计时间。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short)0);
                }
                break;

            case 9:  //log.size==9，最后记录应是R8（第8次复习，16~32天）；
                n=9;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short)0);
                }else if(timeAmountMinutes<(180*2^n)){//1个月之内都可以进行复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else {//后期不会再出现红色状态，后面都标橙色，不计时间。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }
                break;

            case 10:  //log.size==10，最后记录应是R9（第9次复习，32~64天）；
                n=10;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short)0);
                }else if(timeAmountMinutes<(180*2^n)){//1个月之内都可以进行复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else {//后期不会再出现红色状态，后面都标橙色，不计时间。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }
                break;

            case 11:  //log.size==11，最后记录应是R10（第10次复习，64~128天(2~4个月)）；
                n=11;
                if(timeAmountMinutes<(90*2^n)){//此时间段内的复习已完成。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_STILL_NOT);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)0);
                    groupState.setRemainingDays((short)0);
                }else if(timeAmountMinutes<(180*2^n)){//1个月之内都可以进行复习
                    groupState.setColorResId(GroupState.ColorResId.COLOR_AVAILABLE);
                    groupState.setRemainingMinutes((short)0);
                    groupState.setRemainingHours((short)((((180*2^n)-timeAmountMinutes)/60)%24));
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }else {//后期不会再出现红色状态，后面都标橙色，不计时间。
                    groupState.setColorResId(GroupState.ColorResId.COLOR_MISSED_ONCE);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short)(((180*2^n)-timeAmountMinutes)/(60*24)));
                }
                break;

            case 12:  //log.size==12，最后记录应是R11（第11次复习，4~8个月)）；
                //最后一次复习，复习进程超4个月，很可能已在半年以上；所以只要复习到这里
                // （且中间没有连续缺失两次）就记为完成（但是如果中间有连续缺失，提示效果不好）
                    groupState.setColorResId(GroupState.ColorResId.COLOR_FULL);
                    groupState.setRemainingMinutes((short) 0);
                    groupState.setRemainingHours((short) 0);
                    groupState.setRemainingDays((short)0);
                break;
        }*/


    public static String getUpdatedGroupLogs(Context context, int groupId, long finishTime, int learningTypeColor){
        //调用本方法时传入的learningType需要是绿、蓝、橙三种之一。此三者处理方式一致，都是对旧Log进行附加。
        // 由复习发起页面控制进入逻辑。
        YouMemoryDbHelper memoryDbHelper = YouMemoryDbHelper.getInstance(context);
        String oldLogsStr = memoryDbHelper.getGroupById(groupId).getGroupLogs();

        if(oldLogsStr==null||oldLogsStr.isEmpty()){
            //没有记录是错误情况，现在只要建组就有一条log记录。
            return null;//以返回null作为错误情况；以返回empty作为额外学习。
        }

        //拆分Logs
        String[] oldLogs = oldLogsStr.split(";");
        //取最后一条，按分节拆分
        String[] sectionOfSingleLog = oldLogs[oldLogs.length-1].split("#");
        //首节为数字N（建组记录为0，初学记录为1 。60'内复习无记录；其后复习从2起编。）
        String oldNum = sectionOfSingleLog[0];
        if(oldNum == null||oldNum.isEmpty()){
            return null;
        }
        int num = Integer.parseInt(oldNum)+1;//注意，第30~60分钟的复习，不算在LOGS之内，而作为独立标记记录。
        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        /* 新方法似乎无此检验的必要了
        //对第二节的时间记录进行转换，判断是否大于60分钟。
        try {
            Date lastLogDate = sdFormat.parse(sectionOfSingleLog[1]);
            if((finishTime-lastLogDate.getTime())<=60*60*1000) return "";
            //如果本次复习时间与上次日志时间间隔在60分钟内，则为30~60间的复习，不计入Logs。且返回空串由调用方处理。
        } catch (ParseException e) {
            e.printStackTrace();
        }*/

        switch (learningTypeColor){
            case R.color.colorGP_Newly:
            case R.color.colorGP_AVAILABLE:
                //正常状态。生成一条。
                StringBuilder sbd = new StringBuilder();

                sbd.append(oldLogsStr);
                sbd.append(num);
                sbd.append("#");

                String time = sdFormat.format(finishTime);
                sbd.append(time);
                sbd.append("#");

                sbd.append("false");
                sbd.append(";");

                return sbd.toString();

            case R.color.colorGP_Miss_ONCE:
                //需要连续生成两条，前一条按--填充时间数字，boolean置true
                StringBuilder sbdMissedOnce = new StringBuilder();

                sbdMissedOnce.append(oldLogsStr);
                sbdMissedOnce.append(num-1);
                sbdMissedOnce.append("#");

                sbdMissedOnce.append("0000-00-00 00:00:00#");

                sbdMissedOnce.append("true;");

                //继续补充第二条
                sbdMissedOnce.append(num);
                sbdMissedOnce.append("#");

                String time2 = sdFormat.format(finishTime);
                sbdMissedOnce.append(time2);
                sbdMissedOnce.append("#");

                sbdMissedOnce.append("false");
                sbdMissedOnce.append(";");

                return sbdMissedOnce.toString();
            default:
                return null;
        }
    }

    public static ArrayList<String[]> getListStrLogsFromLogs(String logsStr){
        ArrayList<String[]> logSectionArraysInList = new ArrayList<>();
        if(logsStr == null ||logsStr.isEmpty()){
            return null;
        }
        String[] logStrArray = logsStr.split(";");//拆分后每条项是一条记录
        if(logStrArray.length==0){return null;}

        for (String s :logStrArray) {
            String[] logSections = s.split("#");//一条日志记录，以多个分段组合的形式。
            logSectionArraysInList.add(logSections);
        }
        return logSectionArraysInList;
    }

    /*
    * 额外学习完成时调用，计算当前与初学的时间间隔
    * */
    public static int getMinutesBetweenInitLearningAndNow(String strLogs){
        if (strLogs == null || strLogs.isEmpty()) {
            return 0;//【实际为错误情况，待处理】
        }
        String[] strLogsArray = strLogs.split(";");//每个元素是一条形如N#yyyy-MM-dd HH:mm:ss#false;的记录
        if(strLogsArray.length ==1){
            return 0;//只有建组记录，本次就是初学，错误
        }
        SingleLog firstLog = new SingleLog(strLogsArray[1]);//所有情形都要和初次学习时间做比较计算
        return (int)((System.currentTimeMillis() - firstLog.getTimeInMilli()) / (1000 * 60));//当前时间和初次学习时间相比，已过去多少分钟

    }
}
