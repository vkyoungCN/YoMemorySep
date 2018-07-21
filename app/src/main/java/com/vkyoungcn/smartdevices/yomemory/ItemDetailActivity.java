package com.vkyoungcn.smartdevices.yomemory;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

public class ItemDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        SingleItem item = getIntent().getParcelableExtra(Constants.STR_ITEM);
        TextView tvItemInfo = findViewById(R.id.tv_singleItemInfo);

        StringBuilder sbd = new StringBuilder();
        sbd.append("分组情况：id=[");
        sbd.append(item.getId());
        sbd.append("], name=[");
        sbd.append(item.getName());
        sbd.append("], isChose=[");
        sbd.append(item.isChose());
        sbd.append("], isLearned=[");
        sbd.append(item.isLearned());
        sbd.append("], groupId=[");
        sbd.append(item.getGroupId());
        sbd.append("], errTime=[");
        sbd.append(item.getFailedSpelling_times());
        sbd.append("], priority=[");
        sbd.append(item.getPriority());

        tvItemInfo.setText(sbd.toString());

    }
}
