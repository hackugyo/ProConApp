package jp.ne.hatena.hackugyo.procon.model;

import android.util.Patterns;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.SimpleDateFormat;
import java.util.Calendar;

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
    private String citationResource;
    @DatabaseField
    boolean isPages;
    @DatabaseField(useGetSet = true)
    String pages;

    private boolean isLoaded = false;

    public Memo() { // no-arg constructor
        // Empty
    }

    public Memo(Calendar date, String memo, boolean isPro) {

        this.memo = memo;
        if (memo == null) this.memo = "";
        if (date != null) this.date = date;
        dateInMillis = (date == null ? 0L : this.date.getTimeInMillis());
        this.isPro = isPro;
        isPages = false;
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

    public boolean isPages() {
        return isPages;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        isPages = (pages != null && pages.contains("-"));
        this.pages = pages;
    }

    public void setCitationResource(String citationResource) {
        this.citationResource = citationResource;
    }

    public String getCitationResource() {
        return citationResource;
    }

    public boolean isForUrl() {
        return Patterns.WEB_URL.matcher(this.citationResource).matches();
    }

    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
}
