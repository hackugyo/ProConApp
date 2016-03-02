package jp.ne.hatena.hackugyo.procon.event;

import com.github.kubode.rxeventbus.Event;

import jp.ne.hatena.hackugyo.procon.model.Memo;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class DataSavedEvent extends Event {
    public final Memo savedMemo;
    public final boolean isSuccess;

    public DataSavedEvent(Memo memo, boolean isSuccess) {
        this.savedMemo = memo;
        this.isSuccess = isSuccess;
    }
}
