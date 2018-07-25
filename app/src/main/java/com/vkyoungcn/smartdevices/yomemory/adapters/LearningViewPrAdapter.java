package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.vkyoungcn.smartdevices.yomemory.fragments.SingleItemLearningFragment;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.ArrayList;
import java.util.List;
/*
 * 作者：杨胜 @中国海洋大学
 * 别名：杨镇时
 * author：Victor Young@ Ocean University of China
 * email: yangsheng@ouc.edu.cn
 * 2018.08.01
 * */
public class LearningViewPrAdapter extends FragmentStatePagerAdapter {
//* 学习页LearningActivity中用于展示学习卡片（横向ViewPager形式，加载的fragment中以CardView为主控件）
    //    private static final String TAG = "LearningViewPrAdapter";
    private List<SingleItem> singleItems;
    private ArrayList<Byte> restChances;//各卡片对应的提示次数（学习业务中的一个业务逻辑）
    private ArrayList<String> initTextList;//各卡片对应的“初始”（加载时应预先显示的词）
    public SingleItemLearningFragment currentFragment;

    public LearningViewPrAdapter(FragmentManager fm, List<SingleItem> singleItems, ArrayList<Byte> restChances, ArrayList<String> initTextList) {
        super(fm);
        this.singleItems = singleItems;
        this.restChances = restChances;
        this.initTextList = initTextList;
    }

    @Override
    public Fragment getItem(int position) {
        return SingleItemLearningFragment.newInstance(singleItems.get(position),restChances.get(position),initTextList.get(position));
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        this.currentFragment = (SingleItemLearningFragment)object;
        super.setPrimaryItem(container, position, object);

    }

    @Override
    public int getCount() {
        return singleItems.size();
    }
}
