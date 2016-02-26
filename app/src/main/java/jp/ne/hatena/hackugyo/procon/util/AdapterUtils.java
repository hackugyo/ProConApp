package jp.ne.hatena.hackugyo.procon.util;

import android.widget.ListAdapter;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by kwatanabe on 16/02/26.
 */
public class AdapterUtils {
    private AdapterUtils() {

    }

    public static <T> Observable<T> getAdapterContentFrom(final ListAdapter adapter) {
        return Observable.range(0, adapter.getCount())
                .map(new Func1<Integer, T>() {
                    @Override
                    public T call(Integer position) {
                        return (T) adapter.getItem(position);
                    }
                });
    }
}
