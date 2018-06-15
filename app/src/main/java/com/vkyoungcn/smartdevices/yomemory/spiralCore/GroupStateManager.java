package com.vkyoungcn.smartdevices.yomemory.spiralCore;

import android.content.Context;

import com.vkyoungcn.learningtools.models.DBRwaGroup;
import com.vkyoungcn.learningtools.models.RvGroup;

import java.util.ArrayList;
import java.util.List;


/*
* 职责：①从DBGroup中的List<Long>，得出本组在当前时点上的记忆存量、衰减率
* 可能还需要负责刷新当前任务下所属分组的情况。
* */

public class GroupStateManager {
    private static final String TAG = "GroupStateManager";
    private Context context = null;

        public GroupStateManager(Context context) {
        this.context = context;
    }

    /*
    * 表征记忆存量的衰减模型（衰减函数）；
    * 提供若干参数，可生成当前的记忆存量值。
    *
    * 基于如下的规则构建核心业务模型：
    * ①识记完成后，遗忘便开始。
    * ②遗忘的规律是先快后慢。
    * ③进行复习会对整体记忆产生两种有益的效果：
    *   a.刷新记忆——将记忆存量重新调整为100%；
    *   b.改变衰减曲线接下来的倾斜幅度，使衰减速率减缓。
    * ④本次复习所产生的曲线抬升效应，取决于本次复习距上次复习/学习的时间间隔。间隔越小，抬升幅度越大，间隔越大，抬升幅度越小。
    * ⑤睡眠期间记忆的衰减放缓。
    *
    * ——每个分组各有一个独立的记忆存量模型；（所以不能是单例）。
    * ——由GSM提供“复习”方法，来对记忆模型参数进行修改。
    * ——
    * */
    private class retainingModel{
        float alpha = 3600;//预置值
        float beta;//时间间隔的函数
        float gama = 0;//睡眠期间的曲线抬升因数(暂不考虑，故暂设0)

        int fakeNum = 2;//伪操作次数，只起预置调整作用，暂时设为2。
        int realNum;//到目前为止已进行的操作次数，【变量】，是ArrayList的长度。

        long t;//时间间隔。【变量】，基于ArrayList最后记录与当前时间进行计算。
        float a = 100;//理想存量，100。

        byte b = 12;//一个用于β计算的中间参数，暂设12。

        private double retainingAmount = 0;//储存结果值。私有。

        public retainingModel(ArrayList<Long> learningLogs) {
            realNum = learningLogs.size();
            t = (System.currentTimeMillis() - learningLogs.get(realNum-1))/(1000*60);

            //以下计算，最合理的方式就是拆分t小于1的部分，1~1440之间的被减数改用3601；这样能使两端都接得起来。
            if(t<=1){
                beta = 3600;
            }else if(t<=1440){
                beta = (3601-((((float)t)/60)*150));
            }else{
                beta =(float)( 1/(Math.pow(2,(((((float)t)/60)-24)/b))));
            }
            double m1 = a*(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum));//分子部分
            double m2 = t+(Math.pow((1+alpha),fakeNum))*(Math.pow((1+beta),realNum))-1;//分母部分
            retainingAmount = m1/m2;
        }


    }

    public byte retainingAmountNow(ArrayList<Long> learningLogs){
            System.currentTimeMillis();


    }


    //逻辑待修正，未初学的，初学记录值0（差值最大）需放在最前。其余的，初学晚（差值最小）的放在前面
    public static List<DBRwaGroup> ascOrderByTimeBetweenInit(List<DBRwaGroup> dbRwaGroups){
        List<DBRwaGroup> resultGroups = new ArrayList<>();
        long currentMillis = System.currentTimeMillis();

        for (int i = 0; i < dbRwaGroups.size(); ) {//不能i++，但size每次减少1。
            DBRwaGroup minRvGroup = dbRwaGroups.get(i);//即使用new也是指针形式，最后都是重复数据（且提示new无意义）

            for (int j = 1; j < dbRwaGroups.size(); j++) {
                //计算指针项的值
//                long timeBewteenInitMinPointer = currentMillis - minRvGroup.getInitLearningLong();
                long remainingMinutesMinPoint = RemainingTimeAmount.getRemainingTimeInMinutes(remainingTimeAmountMinPointer);

                RemainingTimeAmount remainingTimeAmountJ = LogList.getCurrentRemainingTimeForGroup(rvGroups.get(j).getStrGroupLogs());
                long remainingMinutesJ = RemainingTimeAmount.getRemainingTimeInMinutes(remainingTimeAmountJ);

                if(remainingMinutesJ<remainingMinutesMinPoint){
                    minRvGroup = rvGroups.get(j);//指针指向较小者
                }
            }
            rvGroups.remove(rvGroups.indexOf(minRvGroup));//将最小的删除
            try {
                RvGroup gp = (RvGroup) minRvGroup.clone();//克隆方式复制。
                resultRvGroups.add(gp);//一遍检索的最小值加入结果list。
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        return resultRvGroups;
    }

}
