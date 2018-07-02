package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.vkyoungcn.smartdevices.yomemory.fragments.SingleItemLearningFragment;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.List;

public class LearningViewPrAdapter extends FragmentStatePagerAdapter {
//    private static final String TAG = "LearningViewPrAdapter";
    private List<SingleItem> singleItems;
    public SingleItemLearningFragment currentFragment;

    public LearningViewPrAdapter(FragmentManager fm, List<SingleItem> singleItems) {
        super(fm);
        this.singleItems = singleItems;
    }

    @Override
    public Fragment getItem(int position) {
        return SingleItemLearningFragment.newInstance(singleItems.get(position));
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
