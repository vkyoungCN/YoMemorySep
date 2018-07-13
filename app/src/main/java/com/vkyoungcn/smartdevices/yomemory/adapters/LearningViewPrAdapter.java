package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.vkyoungcn.smartdevices.yomemory.fragments.SingleItemLearningFragment;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.ArrayList;
import java.util.List;

public class LearningViewPrAdapter extends FragmentStatePagerAdapter {
//    private static final String TAG = "LearningViewPrAdapter";
    private List<SingleItem> singleItems;
    private ArrayList<Byte> restChances;
    public SingleItemLearningFragment currentFragment;

    public LearningViewPrAdapter(FragmentManager fm, List<SingleItem> singleItems, ArrayList<Byte> restChances) {
        super(fm);
        this.singleItems = singleItems;
        this.restChances = restChances;
    }

    @Override
    public Fragment getItem(int position) {
        return SingleItemLearningFragment.newInstance(singleItems.get(position),restChances.get(position));
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
