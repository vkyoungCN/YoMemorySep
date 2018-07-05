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
import com.vkyoungcn.smartdevices.yomemory.models.SingleItem;

import java.util.ArrayList;
import java.util.List;

public class ItemsOfMissionRvAdapter extends RecyclerView.Adapter<ItemsOfMissionRvAdapter.ViewHolder> {
    private static final String TAG = "ItemsOfMissionRvAdapter";

    private List<SingleItem> items = new ArrayList<>();
    private Context context;

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView singleItemId;
        private final TextView itemName;
        private final TextView phonetic;
        private final TextView translations;

        private final ImageView goDetail;

        private ViewHolder(View itemView) {
            super(itemView);
            singleItemId = itemView.findViewById(R.id.rv_id_itemOfMission);
            itemName = itemView.findViewById(R.id.rv_name_itemOfMission);
            phonetic = itemView.findViewById(R.id.rv_phonetic_itemOfMission);
            translations = itemView.findViewById(R.id.rv_translation_itemOfMission);

            goDetail = itemView.findViewById(R.id.rv_goDetail_itemOfMission);
            goDetail.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.rv_goDetail_itemOfMission:
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

        public ImageView getGoDetail() {
            return goDetail;
        }

    }

    public ItemsOfMissionRvAdapter(List<SingleItem> items, Context context) {
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
        SingleItem item = items.get(position);

        holder.getSingleItemId().setText(String.valueOf(item.getId()));
        holder.getItemName().setText(item.getName());
        holder.getPhonetic().setText(item.getPhonetic());
        holder.getTranslations().setText(item.getTranslations());

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
