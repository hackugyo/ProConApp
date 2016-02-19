package jp.ne.hatena.hackugyo.procon;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;

import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.CitationResource;
import jp.ne.hatena.hackugyo.procon.model.CitationResourceRepository;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
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

    void loadPreviewAsync() {
        loadPreviewAsync(Observable.from(ArrayUtils.reverse(this.memos)));
    }

    void loadPreviewAsync(Memo memo) {
        loadPreviewAsync(Observable.just(memo));
    }

    void loadPreviewAsync(Observable<Memo> memoObservable) {
        List<Memo> toBeLoaded = memoObservable
                .observeOn(Schedulers.io())
                .filter(new Func1<Memo, Boolean>() {
                    @Override
                    public Boolean call(Memo memo) {
                        return memo.isForUrl() && StringUtils.isEmpty(memo.getMemo());
                    }
                })
                .toList()
                .toBlocking()
                .single();
        List<Observable<Pair<Memo, SourceContent>>> observables = new ArrayList<>();
        for (Memo memo : toBeLoaded) {
            observables.add(
                    Observable.just(memo)
                            .subscribeOn(Schedulers.io())
                            .map(new Func1<Memo, Pair<Memo, SourceContent>>() {
                                @Override
                                public Pair<Memo, SourceContent> call(Memo memo) {
                                    String url = memo.getCitationResource();
                                    SourceContent sourceContent = textCrawler.extractFrom(url, TextCrawler.NONE);
                                    String finalUrl = sourceContent.getFinalUrl();
                                    if (UrlUtils.isTwitterUrl(finalUrl)) { // 特定URLポスト以外のTwitterドメインには未対応
                                        sourceContent = convertTwitterHtml(sourceContent.getHtmlCode());
                                        sourceContent.setFinalUrl(finalUrl);
                                    }
                                    return Pair.create(memo, sourceContent);
                                }
                            })
            );
        }

        Observable parallel = Observable.merge(observables);
        parallel
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // #subscribe(Observer o)のoがどこで動くか．
                .subscribe(new Action1<Pair<Memo, SourceContent>>() {
                    @Override
                    public void call(Pair<Memo, SourceContent> pair) {
                        final long id = pair.first.getId();
                        SourceContent sourceContent = pair.second;
                        final String content = previewText(sourceContent);
                        Memo memo = Observable.from(memos)
                                .firstOrDefault(null, new Func1<Memo, Boolean>() {
                                    @Override
                                    public Boolean call(Memo memo) {
                                        return memo.getId() == id;
                                    }
                                })
                                .toBlocking()
                                .single();
                        if (memo != null) {
                            memo.setMemo(content);
                            memo.setSourceContent(sourceContent);
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

    private String previewText(SourceContent sourceContent) {

        if (sourceContent.getFinalUrl().equals("")) {
            // 失敗
            return null;
        } else {
            return StringUtils.isPresent(sourceContent.getDescription()) ? sourceContent.getDescription() :
                    StringUtils.isPresent(sourceContent.getTitle()) ? sourceContent.getTitle() : "";
        }
    }

    private SourceContent convertTwitterHtml(String html) {

        SourceContent sourceContent = new SourceContent();
        Document document = Jsoup.parse(html);

        // 画像
        List<String> imageUrlList =
                Observable
                        .from(document.getElementsByClass("card-photo"))
                        .map(new Func1<Element, String>() {

                            @Override
                            public String call(Element element) {
                                return element.getElementsByTag("img").first().attr("src");
                            }
                        })
                        .toList()
                        .toBlocking()
                        .single();
        // TODO 20160212 Twitterの複数URLには未対応

        sourceContent.setImages(imageUrlList);

        // 本文
        Elements select = document.select("div[class=dir-ltr]"); //document.getElementsByClass("dir-ltr");

        if (select.size() != 0) {
            LogUtils.d(select.outerHtml());
            Element target = Observable.from(select)
                    .lastOrDefault(null, new Func1<Element, Boolean>() {
                        @Override
                        public Boolean call(Element element) {
                            return StringUtils.isPresent(element.ownText());
                        }
                    })
                    .toBlocking()
                    .single();
            if (target != null) {
                List<String> single =
                        Observable.just(target.ownText())
                                .concatWith(
                                        Observable.from(target.children())
                                                .map(new Func1<Element, String>() {
                                                    @Override
                                                    public String call(Element child) {
                                                        if (child.attr("data-query-source", "hashtag_click") != null) {
                                                            return child.ownText(); // ハッシュタグ
                                                        }

                                                        String url = child.attr("data-expanded-url");
                                                        if (StringUtils.isEmpty(url)) {
                                                            url = child.attr("data-url");
                                                        }
                                                        return url;
                                                    }
                                                }))
                                .toList()
                                .toBlocking()
                                .single();
                sourceContent.setDescription(StringUtils.join(single, StringUtils.getCRLF()));
            }
        }
        return sourceContent;
    }

    static List<String> createNewCitationResources(List<Memo> memos, CitationResourceRepository repo) {

        Observable<String> memoObservable = Observable
                .from(memos)
                .map(new Func1<Memo, String>() {

                    @Override
                    public String call(Memo memo) {
                        return memo.getCitationResource();
                    }
                });
        // 気を利かせて、メモが1つ未満のときはすべての議題を検索して候補を提示する
        if (memos.size() <= 1) {
            memoObservable = Observable.merge(
                    memoObservable,
                    Observable
                            .from(repo.findAll())
                            .map(new Func1<CitationResource, String>() {
                                @Override
                                public String call(CitationResource citationResource) {
                                    return citationResource.getName();
                                }
                            }));
        }
        return memoObservable
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !StringUtils.isEmpty(s) && !UrlUtils.isValidUrl(s.replaceAll("\\s+$", ""));
                    }
                })
                .distinct()
                .toList().toBlocking().single();
    }

}