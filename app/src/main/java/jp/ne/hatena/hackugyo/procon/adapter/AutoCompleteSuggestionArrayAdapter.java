package jp.ne.hatena.hackugyo.procon.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 通常のArrayAdapterを{@link android.widget.AutoCompleteTextView}に使うとデータの更新が反映されないため、
 * 専用のものを用意した。
 *
 * @see <a href="参考リンク>http://stackoverflow.com/a/16064372</a>
 */
public class AutoCompleteSuggestionArrayAdapter extends ArrayAdapter<String> implements
        Filterable {
    private List<String> list;
    private CustomFilter customFilter;

    public AutoCompleteSuggestionArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        list = new ArrayList<String>();
    }

    public AutoCompleteSuggestionArrayAdapter(Context context, int textViewResourceId, List<String> citationResources) {
        super(context, textViewResourceId, citationResources);
        list = new ArrayList<String>(citationResources);
    }

    @Override
    public void add(String object) {
        list.add(object);
        notifyDataSetChanged();
    }

    @Override
    public void addAll(Collection<? extends String> collection) {
        list.addAll(collection);
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public String getItem(int position) {
        return list.get(position);
    }

    @Override
    public CustomFilter getFilter() {
        if (customFilter == null) {
            customFilter = new CustomFilter();
        }
        return customFilter;
    }

    public void callFiltering(String term) {
        customFilter.performFiltering(term);
    }

    private class CustomFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                results.values = list;
                results.count = list.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            if (results != null && results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

    }

}