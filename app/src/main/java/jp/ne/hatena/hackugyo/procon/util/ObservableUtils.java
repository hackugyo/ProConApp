package jp.ne.hatena.hackugyo.procon.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;

/**
 * Created by kwatanabe on 15/12/28.
 */
public class ObservableUtils {

    private ObservableUtils() {

    }

    public static <T,R> R reduce(R initial, Iterable<T> iterable, Func2<R, T, R> func) {
        return Observable.from(iterable)
                .reduce(initial, func)
                .toBlocking()
                .single();
    }



    public static <T,R> R reduce(R initial, Observable<T> observable, Func2<R, T, R> func) {
        return observable
                .reduce(initial, func)
                .toBlocking()
                .single();
    }

    /**
     * Observableの中身を指定のgrouperでgroupByして、HashMapに入れて返します。
     * @param observable
     * @param grouper
     * @param <K> groupByのキーの型
     * @param <V> groupByの値の型
     * @return HashMap（キーごとに対応する値のListが入っている）
     * @see <a href="http://stackoverflow.com/a/33234243">参考リンク</a>
     */
    public static <K, V> HashMap<K, List<V>> groupBy(Observable<V> observable, Func1<V, K> grouper) {
        HashMap<K, List<V>> result = observable.groupBy(grouper)
                .flatMap(new Func1<GroupedObservable<K, V>, Observable<AbstractMap.SimpleEntry<K, List<V>>>>() {
                    @Override
                    public Observable<AbstractMap.SimpleEntry<K, List<V>>> call(final GroupedObservable<K, V> grouped) {
                        return grouped.toList().map(new Func1<List<V>, AbstractMap.SimpleEntry<K, List<V>>>() {
                            @Override
                            public AbstractMap.SimpleEntry<K, List<V>> call(List<V> list) {
                                return new AbstractMap.SimpleEntry<>(grouped.getKey(), list);
                            }
                        });
                    }
                })
                .reduce(new HashMap<K, List<V>>(),
                        new Func2<HashMap<K, List<V>>, AbstractMap.SimpleEntry<K, List<V>>, HashMap<K, List<V>>>() {
                            @Override
                            public HashMap<K, List<V>> call(HashMap<K, List<V>> result, AbstractMap.SimpleEntry<K, List<V>> entry) {
                                result.put(entry.getKey(), entry.getValue());
                                return result;
                            }
                        })
                .toBlocking()
                .single();
        return result;
    }

    public static <K, V, T> Observable<HashMap<K, V>> toHashMap(Observable<T> observable, Func1<T, Map.Entry<K, V>> entryMaker) {
        return observable
                .map(entryMaker)
                .reduce(
                        new HashMap<K, V>(),
                        new Func2<HashMap<K, V>, Map.Entry<K, V>, HashMap<K, V>>() {
                            @Override
                            public HashMap<K, V> call(HashMap<K, V> result, Map.Entry<K, V> entry) {
                                result.put(entry.getKey(), entry.getValue());
                                return result;
                            }
                        });
        // TODO 20160118 #toMap()でいいかも
    }
}
