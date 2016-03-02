package jp.ne.hatena.hackugyo.procon.event;

import com.github.kubode.rxeventbus.Event;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class RequestDataDeleteEvent extends Event{
    public final List<Memo> memos;

    public RequestDataDeleteEvent(Memo... memo) {
        this.memos = ArrayUtils.asList(memo);
    }
}
