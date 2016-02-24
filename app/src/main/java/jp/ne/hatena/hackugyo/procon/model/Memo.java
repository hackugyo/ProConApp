package jp.ne.hatena.hackugyo.procon.model;

import android.util.Patterns;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.leocardz.link.preview.library.SourceContent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.StringUtils;

@DatabaseTable(tableName = "Memo")
public class Memo {
    public static final String TIMESTAMP_FIELD = "timestamp";

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

    private boolean isLoaded = false;
    private SourceContent mSourceContent;
    private List<CitationResource> citationResources;
    private boolean mRemoved;
    private boolean isRemoved = false;

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

}
