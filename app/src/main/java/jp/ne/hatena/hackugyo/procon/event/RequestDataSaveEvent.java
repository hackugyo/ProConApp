package jp.ne.hatena.hackugyo.procon.event;

import com.github.kubode.rxeventbus.Event;

import jp.ne.hatena.hackugyo.procon.model.Memo;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class RequestDataSaveEvent extends Event{
    public final Memo memo;

    public RequestDataSaveEvent(Memo memo) {
        this.memo = memo;
    }
}
