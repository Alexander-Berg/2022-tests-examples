package com.yandex.maps.testapp.guidance;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.List;

import android.content.Context;
import android.app.Activity;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yandex.maps.testapp.R;
import com.yandex.runtime.ByteBufferUtils;
import com.yandex.runtime.bindings.Marshalling;
import com.yandex.runtime.bindings.ClassHandler;

class StatisticsAdapter extends BaseAdapter {
    private static Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    private ListView listView;
    private LayoutInflater layoutInflater;
    private Context ctx;
    private ArrayList<String> items = new ArrayList<String>();

    public StatisticsAdapter(Context ctx,
            ListView listView,
            List<Float> percents,
            HashMap<String, List<Float>> quantilies) {
        this.ctx = ctx;
        this.listView = listView;
        this.layoutInflater = (LayoutInflater) ctx.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        for (Map.Entry<String, List<Float>> entry : quantilies.entrySet()) {
            items.add(entry.getKey());

            for (int i = 0; i < percents.size(); i++) {
                items.add(String.format("Quantile %s: %s",
                        percents.get(i), entry.getValue().get(i)));
            }
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                listView,
                false);

        TextView text = (TextView) item.findViewById(android.R.id.text1);
        text.setText((String) getItem(position));
        return item;
    }
}

public class GuidanceStatisticsActivity extends Activity {
    public static final String PERCENTS_EXTRA = "percents";
    public static final String QUANTILES_EXTRA = "quantiles";

    private static Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guidance_statistics);
        setTitle("Statistics");

        ArrayList<Float> percents = (ArrayList<Float>) getIntent()
                .getSerializableExtra(PERCENTS_EXTRA);

        HashMap<String, List<Float>> quantiles = (HashMap<String, List<Float>>) getIntent()
                .getSerializableExtra(QUANTILES_EXTRA);

        ListView listView = (ListView) findViewById(R.id.guidance_statistics_list);
        StatisticsAdapter adapter = new StatisticsAdapter(this, listView, percents, quantiles);
        listView.setAdapter(adapter);
    }
}
