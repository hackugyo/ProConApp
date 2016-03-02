package jp.ne.hatena.hackugyo.procon.event;

/**
 * Created by kwatanabe on 16/03/02.
 */
public interface Subscribable<T> {
    void onEventAsync(T event);
    void unsubscribe();
}
