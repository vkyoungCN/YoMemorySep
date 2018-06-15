package com.vkyoungcn.smartdevices.yomemory.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vkyoungcn.smartdevices.yomemory.R;
import com.vkyoungcn.smartdevices.yomemory.models.RvSingleItem;
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.ArrayList;
import java.util.List;

public class ItemsOfMissionRvAdapter extends RecyclerView.Adapter<ItemsOfMissionRvAdapter.ViewHolder> {
    private static final String TAG = "ItemsOfMissionRvAdapter";

    private List<RvSingleItem> items = new ArrayList<>();
    private Context context;

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView singleItemId;
        private final TextView itemName;
        private final TextView phonetic;
        private final TextView translations;

        private final TextView isChose;
        private final TextView isLearned;
        private final TextView priority;
        private final TextView groupId;
        private final TextView errorTimes;

        private final ImageView goEdit;

        private ViewHolder(View itemView) {
            super(itemView);
            singleItemId = itemView.findViewById(R.id.rv_id_itemOfMission);
            itemName = itemView.findViewById(R.id.rv_name_itemOfMission);
            phonetic = itemView.findViewById(R.id.rv_ex1_itemOfMission);
            translations = itemView.findViewById(R.id.rv_ex2_itemOfMission);

            isChose = itemView.findViewById(R.id.rv_is_chose_itemOfMission);
            isLearned = itemView.findViewById(R.id.rv_is_learned_itemOfMission);
            groupId = itemView.findViewById(R.id.rv_tv_group_id_itemOfMission);
            priority = itemView.findViewById(R.id.rv_priority_itemOfMission);
            errorTimes = itemView.findViewById(R.id.rv_spell_error_times_itemOfMission);

            goEdit = itemView.findViewById(R.id.rv_edit2_itemOfMission);
            goEdit.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.rv_edit2_itemOfMission:
                    //暂时没有动作
                    Toast.makeText(context, "转到单项状态页，施工中……", Toast.LENGTH_SHORT).show();
            }
        }

        private TextView getSingleItemId() {
            return singleItemId;
        }

        private TextView getItemName() {
            return itemName;
        }

        private TextView getPhonetic() {
            return phonetic;
        }

        private TextView getTranslations() {
            return translations;
        }

        public ImageView getGoEdit() {
            return goEdit;
        }

        public TextView getErrorTimes() {
            return errorTimes;
        }

        public TextView getIsChose() {
            return isChose;
        }

        public TextView getIsLearned() {
            return isLearned;
        }

        public TextView getPriority() {
            return priority;
        }

        public TextView getGroupId() {
            return groupId;
        }
    }

    public ItemsOfMissionRvAdapter(List<RvSingleItem> items, Context context) {
        this.items = items;
//        ItemTableNameSuffix = itemTableNameSuffix;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_row_items_of_mission,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemsOfMissionRvAdapter.ViewHolder holder, int position) {
        RvSingleItem item = items.get(position);

        holder.getSingleItemId().setText(String.valueOf(item.getId()));
        holder.getItemName().setText(item.getName());
        holder.getPhonetic().setText(item.getPhonetic());
        holder.getTranslations().setText(item.getTranslations());

        holder.getIsChose().setText(item.getIsChose());
        holder.getIsLearned().setText(item.getIsLearned());
        holder.getGroupId().setText(String.valueOf(item.getGroupId()));
        holder.getPriority().setText(String.valueOf(item.getPriority()));
        holder.getErrorTimes().setText(String.valueOf(item.getFailedSpelling_times()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
