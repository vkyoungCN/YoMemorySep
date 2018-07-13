package com.vkyoungcn.smartdevices.yomemory.stripeProgressBar;

/*
 * 作者1：杨胜@中国海洋大学图书馆
 * 作者2：杨镇时@中国海洋大学
 * author：Victor Young @Ocean University of China
 * email: yangsheng@ouc.edu.cn
 *
* 为条纹列表储存状态信息——正确、错误、空、当前。
* 以便将所有条纹构成数组，便于绘制计算。
*
* 只存放状态；其长度、绘制坐标等简单计算即可。
* */
public class SingleStripe {
    public static final int SINGLE_STRIPE_CORRECT = 5201;
    public static final int SINGLE_STRIPE_UN_CORRECT = 5202;
    public static final int SINGLE_STRIPE_EMPTY = 5203;
    public static final int SINGLE_STRIPE_CURRENT = 5204;

    private int state = SINGLE_STRIPE_EMPTY ;//默认

    public SingleStripe() {
    }

    public SingleStripe(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
