package jp.ne.hatena.hackugyo.procon;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;

import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

import java.util.List;

import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by kwatanabe on 16/02/12.
 */
public class MainActivityHelper {

    private final ImprovedTextCrawler textCrawler;
    private final RecyclerView.Adapter adapter;
    private final List<Memo> memos;
    private final Handler handler;

    MainActivityHelper(ImprovedTextCrawler crawler, RecyclerView.Adapter adapter, List<Memo> memos) {
        this.textCrawler = crawler;
        this.adapter = adapter;
        this.memos = memos;
        this.handler = new Handler();
    }

    void loadPreview() {
        Observable.from(memos)
                .filter(new Func1<Memo, Boolean>() {
                    @Override
                    public Boolean call(Memo memo) {
                        return memo.isForUrl() && StringUtils.isEmpty(memo.getMemo());
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(new Func1<Memo, Pair<Memo, String>>() {
                    @Override
                    public Pair<Memo, String> call(Memo memo) {
                        String url = memo.getCitationResource();
                        SourceContent sourceContent = textCrawler.extractFrom(url, TextCrawler.NONE);
                        String previewText = preview(url, sourceContent);
                        return Pair.create(memo, previewText);
                    }
                })
                .subscribe(new Action1<Pair<Memo, String>>() {
                    @Override
                    public void call(Pair<Memo, String> memoStringPair) {
                        final long id = memoStringPair.first.getId();
                        final String content = memoStringPair.second;
                        Memo memo = Observable.from(memos)
                                .first(new Func1<Memo, Boolean>() {
                                    @Override
                                    public Boolean call(Memo memo) {
                                        return memo.getId() == id;
                                    }
                                })
                                .toBlocking()
                                .single();
                        if (memo != null) {
                            memo.setMemo(content);
                            memo.setLoaded(true);
                            final int i = memos.indexOf(memo);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyItemChanged(i);
                                }
                            });
                        }

                    }
                });
    }

    private String preview(String originalUrl, SourceContent sourceContent) {

        if (sourceContent.getFinalUrl().equals("")) {
            // 失敗
            return null;
        } else {
            String result;
            if (UrlUtils.isTwitterUrl(originalUrl)) {
                result = sourceContent.getHtmlCode();
            } else {
                result = sourceContent.getDescription();
            }
            return result;
        }
    }
}
