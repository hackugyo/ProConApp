package jp.ne.hatena.hackugyo.procon.event;

import com.github.kubode.rxeventbus.RxEventBus;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class RxBusProvider {
    private static final RxEventBus BUS = new RxEventBus();

    private RxBusProvider() {
        // No instances.
    }

    public static RxEventBus getInstance() {
        return BUS;
    }
}