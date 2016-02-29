package jp.ne.hatena.hackugyo.procon.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.AdapterUtils;
import rx.Observable;

/**
 * 通常のArrayAdapterを{@link android.widget.AutoCompleteTextView}に使うとデータの更新が反映されないため、
 * 専用のものを用意した。
 *
 * @see <a href="参考リンク>http://stackoverflow.com/a/16064372</a>
 */
public class AutoCompleteSuggestionArrayAdapter extends ArrayAdapter<String> implements
        Filterable {
    private final AutoCompleteSuggestionArrayAdapter self = this;
    private List<String> currentShowingList;
    private CustomFilter customFilter;


    private final Object mLock = new Object();

    // A copy of the original mObjects array, initialized from and then used instead as soon as
    // the mFilter ArrayFilter is used. mObjects will then only contain the filtered values.
    private List<String> mOriginalValues;

    public AutoCompleteSuggestionArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        currentShowingList = new ArrayList<String>();
    }

    public AutoCompleteSuggestionArrayAdapter(Context context, int textViewResourceId, List<String> citationResources) {
        super(context, textViewResourceId, citationResources);
        currentShowingList = new ArrayList<String>(citationResources);
    }

    @Override
    public void add(String object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(object);
            }
            currentShowingList.add(object);
        }
        notifyDataSetChanged();
    }

    @Override
    public void addAll(Collection<? extends String> collection) {

        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.addAll(collection);
            }
            currentShowingList.addAll(collection);
        }
        notifyDataSetChanged();
    }

    @Override
    public void clear() {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.clear();
            }
            currentShowingList.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return currentShowingList.size();
    }

    @Override
    public String getItem(int position) {
        return currentShowingList.get(position);
    }

    @Override
    public Filter getFilter() {
        // if (true) return super.getFilter();
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
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    Observable<String> adapterContentFrom = AdapterUtils.getAdapterContentFrom(self);
                    mOriginalValues = adapterContentFrom.toList().toBlocking().single();
                }
            }

            if (prefix == null || prefix.length() == 0) {
                ArrayList<String> list;
                synchronized (mLock) {
                    list = new ArrayList<String>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                String prefixString = prefix.toString().toLowerCase();
                ArrayList<String> values;
                synchronized (mLock) {
                    values = new ArrayList<String>(mOriginalValues);
                }

                final int count = values.size();
                final ArrayList<String> newValues = new ArrayList<String>();

                for (int i = 0; i < count; i++) {
                    final String value = values.get(i);
                    final String valueText = value.toString().toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        final int wordCount = words.length;

                        // Start at index 0, in case valueText starts with space(s)
                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            currentShowingList = (List<String>) results.values;
            if (results != null && results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

    }

}