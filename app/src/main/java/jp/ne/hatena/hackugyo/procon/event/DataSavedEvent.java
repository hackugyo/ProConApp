package jp.ne.hatena.hackugyo.procon.event;

import android.support.v4.util.Pair;

import com.github.kubode.rxeventbus.Event;

import java.util.ArrayList;

import jp.ne.hatena.hackugyo.procon.model.Memo;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class DataSavedEvent extends Event {

    public final ArrayList<Pair<Memo, Boolean>> pairs;

    public DataSavedEvent(Memo memo, boolean isSuccess) {
        this.pairs = new ArrayList<>();
        this.pairs.add(Pair.create(memo, isSuccess));
    }
    public DataSavedEvent(ArrayList<Pair<Memo, Boolean>> pairs) {
        this.pairs = pairs;
    }
}
