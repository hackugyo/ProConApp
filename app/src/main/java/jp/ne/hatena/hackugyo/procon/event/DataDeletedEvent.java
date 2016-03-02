package jp.ne.hatena.hackugyo.procon.event;

import android.support.v4.util.Pair;

import com.github.kubode.rxeventbus.Event;

import java.util.ArrayList;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class DataDeletedEvent extends Event{


    public final ArrayList<Pair<Long, Boolean>> pairs;

    public DataDeletedEvent(ArrayList<Pair<Long, Boolean>> pairs) {
        this.pairs = pairs;
    }
}
