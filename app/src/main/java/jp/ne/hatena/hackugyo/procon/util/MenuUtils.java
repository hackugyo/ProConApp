package jp.ne.hatena.hackugyo.procon.util;

import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by kwatanabe on 16/02/16.
 */
public class MenuUtils {
    private MenuUtils() {

    }

    public static Observable<MenuItem> getMenuItemObservableFrom(final Menu menu) {
        // リスト操作によって破壊されてしまう場合があるので、まずリストを取り出す
        List<MenuItem> single = Observable.range(0, menu.size())
                .map(new Func1<Integer, MenuItem>() {
                    @Override
                    public MenuItem call(Integer index) {
                        return menu.size() <= index ? null : menu.getItem(index);
                    }
                })
                .toList()
                .toBlocking()
                .single();
        return Observable.from(single).filter(new Func1<MenuItem, Boolean>() {
                    @Override
                    public Boolean call(MenuItem menuItem) {
                        return menuItem != null;
                    }
                });

    }
}
