package jp.ne.hatena.hackugyo.procon;

import android.content.Context;
import android.os.Handler;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;

import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.io.ImprovedTextCrawler;
import jp.ne.hatena.hackugyo.procon.model.CitationResource;
import jp.ne.hatena.hackugyo.procon.model.CitationResourceRepository;
import jp.ne.hatena.hackugyo.procon.model.Memo;
import jp.ne.hatena.hackugyo.procon.model.MemoRepository;
import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.FileUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Created by kwatanabe on 16/02/12.
 */
public class MainActivityHelper {
    private final MainActivityHelper self = this;
    private final ImprovedTextCrawler textCrawler;
    private final RecyclerView.Adapter adapter;
    private final List<Memo> memos;
    private final Handler handler;
    private final MemoRepository memoRepository;

    MainActivityHelper(ImprovedTextCrawler crawler, RecyclerView.Adapter adapter, List<Memo> memos, MemoRepository memoRepository) {
        this.textCrawler = crawler;
        this.adapter = adapter;
        this.memos = memos;
        this.memoRepository = memoRepository;
        this.handler = new Handler();
    }

    void loadPreviewAsync() {
        loadPreviewAsync(Observable.from(ArrayUtils.reverse(this.memos)));
    }

    void forceReloadPreviewAsync(Memo memo) {
        if (!memo.isForUrl()) return;
        memo.setMemo(null);
        memo.setLoaded(false);
        memo.setSourceContent(null);
        memo.resetSubCitationResources();
        loadPreviewAsync(memo);
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
                        boolean result = memo.isForUrl() && StringUtils.isEmpty(memo.getMemo());
                        if (!result) memo.setLoaded(true); // あまり行儀よくないが，group化するのめんどうだったのでここで弾いたものはtrueにしてしまう
                        return result;
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
                                        sourceContent = convertTwitterHtml(sourceContent.getHtmlCode(), finalUrl);
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
                .subscribe(
                        new Action1<Pair<Memo, SourceContent>>() {
                            @Override
                            public void call(Pair<Memo, SourceContent> pair) {
                                final long id = pair.first.getId();
                                SourceContent sourceContent = pair.second;
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
                                    final String content = previewText(sourceContent);
                                    memo.setMemo(content);
                                    memo.setSourceContent(sourceContent);
                                    if (sourceContent != null && ArrayUtils.any(sourceContent.getImages()) && UrlUtils.isTwitterUrl(sourceContent.getFinalUrl())) {
                                        memo.addCitationResource(sourceContent.getImages().get(0));
                                    }
                                    memo.setLoaded(true);
                                    if (self.memoRepository == null || self.memoRepository.isClosed()) {
                                        Context context = AppApplication.getContext();
                                        MemoRepository memoRepository = new MemoRepository(context);
                                        memoRepository.save(memo);
                                        memoRepository.onPause();
                                    } else {
                                        self.memoRepository.save(memo);
                                    }

                                    final int i = memos.indexOf(memo);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (i < adapter.getItemCount()){
                                                adapter.notifyItemChanged(i);
                                            }
                                        }
                                    });
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                if (throwable instanceof IllegalFormatCodePointException) {
                                    LogUtils.w(throwable.getMessage());
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

    private SourceContent convertTwitterHtml(String html, String finalUrl) {

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

        if (BuildConfig.DEBUG) {
            String fileName = StringUtils.build(ArrayUtils.last(finalUrl.split("/")), ".html");
            FileUtils.saveFile(AppApplication.getContext(), fileName, html);
        }

        // 本文
        Elements elementsByClass = document.getElementsByClass("main-tweet-container");
        Elements select =  (elementsByClass.size() == 0 ? document : elementsByClass.last()).select("div[class=dir-ltr]"); //document.getElementsByClass("dir-ltr");
        if (select.size() == 0) return sourceContent;
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
                                                    if (StringUtils.isSame(child.attr("data-query-source"), "hashtag_click")) {
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

        return sourceContent;
    }

    private static Func1<String, Boolean> sIgnoreUrlFilter = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String s) {
            return !StringUtils.isEmpty(s) && !UrlUtils.isValidWebUrl(StringUtils.stripLast(s));
        }
    };

    static List<String> createNewCitationResources(List<Memo> memos, CitationResourceRepository repo) {

        Observable<String> memoObservable = Observable
                .from(memos)
                .map(new Func1<Memo, String>() {

                    @Override
                    public String call(Memo memo) {
                        return memo.getCitationResource();
                    }
                })
                .filter(sIgnoreUrlFilter);
        // 気を利かせて、候補なしのときはすべての議題を検索して、候補を提示する
        if (memoObservable.count().toBlocking().single() < 1) {
            memoObservable = Observable.merge(
                    memoObservable,
                    Observable
                            .from(repo.findAllWithoutIsolated())
                            .map(new Func1<CitationResource, String>() {
                                @Override
                                public String call(CitationResource citationResource) {
                                    return citationResource.getName();
                                }
                            })
                            .filter(sIgnoreUrlFilter)
            );
        }
        return memoObservable
                .distinct()
                .toSortedList().toBlocking().single();
    }

    static Observable<Pair<Long, StringBuilder>> createExportationObservable (final long themeId, Observable<Memo> memosObservable) {
        return memosObservable
                .map(new Func1<Memo, StringBuilder>() {
                    @Override
                    public StringBuilder call(Memo memo) {
                        String text = memo.getMemo().replaceAll(StringUtils.getCRLF(), StringUtils.getCRLF() + "> ");
                        StringBuilder stringBuilder = new StringBuilder(StringUtils.getCRLF())
                                .append("<!--- ")  // <!-- ではなく<!---とするとよいらしい
                                .append(memo.isPro() ? "賛成派" : "反対派").append(" -->")
                                .append(StringUtils.getCRLF());
                        if (memo.isReply()) {
                            stringBuilder
                                    .append("> @")
                                    .append(memo.replyTo())
                                    .append(StringUtils.getCRLF());
                        }
                        stringBuilder
                                .append("> ")
                                .append(text);
                        if (StringUtils.isPresent(memo.getCitationResource())) {
                            stringBuilder
                                    .append(StringUtils.getCRLF())
                                    .append("> ")
                                    .append(StringUtils.getCRLF())
                                    .append("> -- ")
                                    .append(memo.getCitationResource());
                            if (StringUtils.isPresent(memo.getPages())) {
                                stringBuilder.append(", ")
                                        .append(memo.hasManyPages() ? "pp. " : "p. ")
                                        .append(memo.getPages());
                            }
                        }
                        return stringBuilder;
                    }
                })
                .reduce(new StringBuilder(),
                        new Func2<StringBuilder, StringBuilder, StringBuilder>() {
                            @Override
                            public StringBuilder call(StringBuilder stringBuilder, StringBuilder stringBuilder2) {
                                return stringBuilder.append(StringUtils.getCRLF()).append(stringBuilder2);
                            }
                        })
                .map(new Func1<StringBuilder, Pair<Long, StringBuilder>>() {
                    @Override
                    public Pair<Long, StringBuilder> call(StringBuilder stringBuilder) {
                        return Pair.create(themeId, stringBuilder);
                    }
                });
    }

    static Observable<Pair<Long, StringBuilder>> createExportationCitationsObservable(final long themeId,
                                                                                      Observable<Memo> prosObservable,
                                                                                      Observable<Memo> consObservable) {
        Observable<StringBuilder> pros = prosObservable
                .map(new Func1<Memo, StringBuilder>() {
                    @Override
                    public StringBuilder call(Memo memo) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder
                                .append("* ")
                                .append(memo.getCitationResource());
                        return stringBuilder;
                    }
                });
        Observable<StringBuilder> cons = consObservable
                .map(new Func1<Memo, StringBuilder>() {
                    @Override
                    public StringBuilder call(Memo memo) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder
                                .append("* ")
                                .append(memo.getCitationResource());
                        return stringBuilder;
                    }
                });
        Observable<StringBuilder> prosHeader = Observable.just(new StringBuilder("# 賛成派").append(StringUtils.getCRLF()));
        Observable<StringBuilder> consHeader = Observable.just(new StringBuilder(StringUtils.getCRLF()).append("# 反対派").append(StringUtils.getCRLF()));
        return Observable
                .concat(
                prosHeader, pros,
                consHeader, cons
        )
                .observeOn(Schedulers.computation())
                .reduce(new StringBuilder(),
                        new Func2<StringBuilder, StringBuilder, StringBuilder>() {
                            @Override
                            public StringBuilder call(StringBuilder stringBuilder, StringBuilder stringBuilder2) {
                                return stringBuilder.append(StringUtils.getCRLF()).append(stringBuilder2);
                            }
                        })
                .map(new Func1<StringBuilder, Pair<Long, StringBuilder>>() {
                    @Override
                    public Pair<Long, StringBuilder> call(StringBuilder stringBuilder) {
                        return Pair.create(themeId, stringBuilder);
                    }
                });
    }

}
