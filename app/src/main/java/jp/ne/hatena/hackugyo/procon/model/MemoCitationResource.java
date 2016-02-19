package jp.ne.hatena.hackugyo.procon.model;

import com.j256.ormlite.field.DatabaseField;

/**
 * 個々のメモと引用リソースとを多対多でつなぐための中間テーブル
 */
public class MemoCitationResource implements MiddleModel {

    public final static String MEMO_ID_FIELD_NAME = "memo_id";
    public final static String CITATION_RESOURCE_ID_FIELD_NAME = "citation_resource_id";

    // this id is generated by the database and set on the object when it is passed to the create method
    @DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
    int id;


    // This is a foreign object which just stores the id from the User object in this table.
    @DatabaseField(foreign = true, columnName = MEMO_ID_FIELD_NAME)
    Memo memo;

    // This is a foreign object which just stores the id from the Post object in this table.
    @DatabaseField(foreign = true, columnName = CITATION_RESOURCE_ID_FIELD_NAME)
    CitationResource citationResource;

     MemoCitationResource() {
        // for ormlite
    }

    public MemoCitationResource(Memo memo, CitationResource citationResource) {
        this.memo = memo;
        this.citationResource = citationResource;
    }
}
