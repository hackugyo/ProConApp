package jp.ne.hatena.hackugyo.procon.model;

import android.util.Patterns;

import com.github.davidmoten.util.MapWithIndex;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.leocardz.link.preview.library.SourceContent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;
import jp.ne.hatena.hackugyo.procon.util.UrlUtils;
import rx.Observable;
import rx.functions.Func1;

@DatabaseTable(tableName = "Memo")
public class Memo {
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String POSITION_FIELD = "position";

    @DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
    Long id;

    @DatabaseField(useGetSet = true)
    String memo;

    Calendar date;
    @DatabaseField(columnName = TIMESTAMP_FIELD)
    Long dateInMillis;

    @DatabaseField
    boolean isPro;

    @DatabaseField(useGetSet = true)
    String pages;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private ChatTheme chatTheme;

    @DatabaseField(canBeNull = false, columnName = POSITION_FIELD)
    Long position;

    /**
     * 同メモ内のどのメモへの応答かを保持する
     */
    @DatabaseField
    Long replyTo;

    private boolean isLoaded = false;
    private SourceContent mSourceContent;
    private List<CitationResource> citationResources;
    private boolean isRemoved = false;
    private boolean isChanged = false;
    private boolean mReplyTo;

    public Memo() { // no-arg constructor
        // Empty
    }

    public Memo(Calendar date, String memo, boolean isPro) {

        this.memo = memo;
        if (memo == null) this.memo = "";
        if (date != null) this.date = date;
        dateInMillis = (date == null ? 0L : this.date.getTimeInMillis());
        this.isPro = isPro;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? "" : memo;
    }


    public String getMemo() {
        //memo
        if (this.memo == null) return "";
        return memo;
    }

    public String getDate() {
        //Date のString への変換
        if (date == null) {
            // 遅延評価（Lazy Evaluation)
            if (dateInMillis != null) {
                date = Calendar.getInstance();
                date.setTimeInMillis(dateInMillis);
            }
        }
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd\tHH:mm:ss\t");
        return sdf.format(date.getTime());
    }

    public long getId() {
        return id == null ? 0 : id;
    }

    public boolean isPro() {
        return isPro;
    }

    public boolean hasManyPages() {
        return (pages != null && pages.contains("-"));
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }


    public String getCitationResource() {
        return ArrayUtils.any(citationResources) ? citationResources.get(0).name : "";
    }

    public boolean isForUrl() {
        return Patterns.WEB_URL.matcher(getCitationResource()).matches();
    }

    public boolean isWithPhoto() {
        return !isForUrl() && getCitationResources() != null &&
                Observable.from(getCitationResources())
                        .filter(new Func1<CitationResource, Boolean>() {
                            @Override
                            public Boolean call(CitationResource citationResource) {
                                return UrlUtils.isValidUri(citationResource.getName());
                            }
                        })
                        .firstOrDefault(null).toBlocking().single() != null;
    }

    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public SourceContent getSourceContent() {
        return mSourceContent;
    }

    public void setSourceContent(SourceContent sourceContent) {
        mSourceContent = sourceContent;
    }

    public ChatTheme getChatTheme() {
        return chatTheme;
    }

    public void setChatTheme(ChatTheme chatTheme) {
        this.chatTheme = chatTheme;
    }

    public void setCitationResources(List<CitationResource> citationResources) {
        this.citationResources = citationResources;
    }

    public List<CitationResource> getCitationResources() {
        return citationResources;
    }

    public void addCitationResource(String resource) {
        if (StringUtils.isEmpty(resource)) return;
        if (this.citationResources == null) this.citationResources = new ArrayList<>();
        this.citationResources.add(new CitationResource(resource));
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public String getImageUrl() {
        if (getCitationResources() == null) return null;
        SourceContent sourceContent = getSourceContent();
        String imageUrl = null;
        if (sourceContent == null) {
            Observable<CitationResource> from = Observable.from(getCitationResources());
            if (isForUrl()) {
                from = from.skip(1);
            }
            imageUrl = from.filter(new Func1<CitationResource, Boolean>() {
                @Override
                public Boolean call(CitationResource citationResource) {
                    return UrlUtils.isValidUri(citationResource.getName());
                }
            })
                    .map(new Func1<CitationResource, String>() {
                        @Override
                        public String call(CitationResource citationResource) {
                            return citationResource.getName();
                        }
                    })
                    .firstOrDefault(null)
                    .toBlocking()
                    .single();
        } else if (ArrayUtils.any(sourceContent.getImages()) && UrlUtils.isTwitterUrl(sourceContent.getFinalUrl())) {
            imageUrl = getSourceContent().getImages().get(0);
        }
        return imageUrl;
    }

    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public void resetSubCitationResources() {
        if (ArrayUtils.any(this.citationResources)) {
            CitationResource citationResource = this.citationResources.get(0);
            this.citationResources.clear();
            this.citationResources.add(citationResource);
        }
    }

    /**
     * 渡されたすべてのメモに順序を設定します。
     * @param sortedMemos
     * @return 変更があった場合true、なければfalse
     */
    public static boolean setPositions(Observable<Memo> sortedMemos) {
        sortedMemos
                .compose(MapWithIndex.<Memo>instance())
                .map(new Func1<MapWithIndex.Indexed<Memo>, Memo>() {
                    @Override
                    public Memo call(MapWithIndex.Indexed<Memo> memoIndexed) {
                        Memo memo = memoIndexed.value();
                        if (memo.position == null || memo.position != memoIndexed.index()) {
                            memo.isChanged = true;
                            memo.position = memoIndexed.index();
                        }
                        return memo;
                    }
                }).subscribe();
        Memo single = sortedMemos.firstOrDefault(null, new Func1<Memo, Boolean>() {
            @Override
            public Boolean call(Memo memo) {
                return memo.isChanged;
            }
        }).toBlocking().single();
        return single != null;
    }


    @Override
    public String toString() {
        String s = super.toString();
        return StringUtils.build(s, "{",
                "id: ", StringUtils.valueOf(id),
                ", position: ", StringUtils.valueOf(position),
                ", memo: ", memo,
                ", date: ", getDate(),
                ", isPro: ", StringUtils.valueOf(isPro()),
                ", pages: ", pages,
                ", replyTo(id): ", StringUtils.valueOf(replyTo),
                ", chatTheme: ", StringUtils.valueOf(chatTheme.getId()),
                "}");
    }

    public boolean isReply() {
        return replyTo != null;
    }

    public Long replyTo() {
        return replyTo;
    }
}
