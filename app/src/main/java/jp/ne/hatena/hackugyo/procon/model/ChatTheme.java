package jp.ne.hatena.hackugyo.procon.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import jp.ne.hatena.hackugyo.procon.util.StringUtils;

/**
 * Created by kwatanabe on 16/02/12.
 */
public class ChatTheme {
    public static final String ID_FIELD = "id";
    public static final String TIMESTAMP_FIELD = "timestamp";

    @DatabaseField(columnName =  ID_FIELD, generatedId = true, allowGeneratedIdInsert = true)
    Long id;

    @DatabaseField(useGetSet = true)
    String title;

    Calendar date;
    @DatabaseField(columnName = TIMESTAMP_FIELD)
    Long dateInMillis;

    @ForeignCollectionField(eager = false, orderColumnName = Memo.POSITION_FIELD)
    private ForeignCollection<Memo> children;

    public ChatTheme() {
        // for OrmLite
    }

    public ChatTheme(String title) {
        this.title = title;
        if (date == null) date = Calendar.getInstance();
        dateInMillis = this.date.getTimeInMillis();
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ForeignCollection<Memo> getChildren() {
        return children;
    }

    @Deprecated
    private void addChildren(ForeignCollection<Memo> children) {
        throw new UnsupportedOperationException();
    }

    public String getDate() {
        //Date のString への変換
        if (date == null) {
            // 遅延評価
            if (dateInMillis != null) {
                date = Calendar.getInstance();
                date.setTimeInMillis(dateInMillis);
            }
        }
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd\tHH:mm:ss\t");
        return sdf.format(date.getTime());
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return StringUtils.build(
                super.toString(),
                "{id: ", StringUtils.valueOf(id),
                ", title: ", title,
                "}"
        );
    }
}
