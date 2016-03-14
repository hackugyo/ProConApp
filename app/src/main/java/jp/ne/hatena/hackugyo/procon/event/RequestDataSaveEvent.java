package jp.ne.hatena.hackugyo.procon.event;

import com.github.kubode.rxeventbus.Event;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class RequestDataSaveEvent extends Event{
    public final List<Memo> memos;

    public RequestDataSaveEvent(Memo... memo) {
        this.memos = ArrayUtils.asList(memo);
    }

    public RequestDataSaveEvent(List<Memo> sortedMemos) {
        this.memos = ArrayUtils.copy(sortedMemos);
    }
}
