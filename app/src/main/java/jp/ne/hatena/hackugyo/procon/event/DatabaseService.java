package jp.ne.hatena.hackugyo.procon.event;

import android.content.Context;
import android.support.v4.util.Pair;

import com.github.kubode.rxeventbus.RxEventBus;

import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by kwatanabe on 16/03/02.
 */
public class DatabaseService implements Subscribable<RequestDataSaveEvent> {

    private final Context context;
    private final CompositeSubscription mCompositeSubscription;
    private final RxEventBus bus;
    private final MemoRepository memoRepository;

    public DatabaseService(Context context, RxEventBus bus, MemoRepository memoRepository) {
        this.context = context;
        this.memoRepository = memoRepository;
        this.bus = bus;
        mCompositeSubscription = new CompositeSubscription();
        mCompositeSubscription.add(bus.subscribe(
                RequestDataSaveEvent.class,
                new Action1<RequestDataSaveEvent>() {
                    @Override
                    public void call(RequestDataSaveEvent requestDataSaveEvent) {
                        onEventAsync(requestDataSaveEvent);
                    }
                }, Schedulers.io())
        );
        mCompositeSubscription.add(bus.subscribe(
                RequestDataDeleteEvent.class,
                new Action1<RequestDataDeleteEvent>() {
                    @Override
                    public void call(RequestDataDeleteEvent requestDataDeleteEvent) {
                        onEventAsync(requestDataDeleteEvent);
                    }
                }, Schedulers.io())
        );
    }

    @Override
    public void unsubscribe() {
        mCompositeSubscription.unsubscribe();
    }


    @Override
    public void onEventAsync(RequestDataSaveEvent event) {
        List<Memo> memos = event.memos;

        ArrayList<Pair<Memo, Boolean>> pairs = new ArrayList<>();
        for (Memo memo : memos) {
            boolean save = memoRepository.save(memo); // ここでmemoインスタンスに必要ならidがふられる
            pairs.add(Pair.create(memo, save));
        }
        bus.post(new DataSavedEvent(pairs));

    }

    public void onEventAsync(RequestDataDeleteEvent event) {
        ArrayList<Pair<Long, Boolean>> pairs = new ArrayList<>();
        for (Memo memo : event.memos) {
            long deletingMemoId = memo.getId();
            pairs.add(Pair.create(deletingMemoId, memoRepository.delete(memo) == 1));
        }

        bus.post(new DataDeletedEvent(pairs));
    }
}
