package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.concurrent.atomic.AtomicInteger;

import jp.ne.hatena.hackugyo.procon.BuildConfig;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;

/**
 * Created by kwatanabe on 15/09/07.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    /**
     * データベースのファイル名。SQLiteではデータベースは単一のファイルとして管理される。
     */
    private static final String DATABASE_NAME = "procon_app.db";

    /**
     * データベースのヴァージョン。今後、テーブルの追加や変更をした際には、このヴァージョンを更新することで、アプリにDBが更新されたことを伝える。
     */
    public static final int DATABASE_VERSION = BuildConfig.DB_VERSION;

    /**
     * いま、データベースとアプリとの接続が何カ所で行われているかのカウンタ。
     */
    private static AtomicInteger sHelperReferenceCount = new AtomicInteger(0);

    /**
     * データベースとの接続を扱うヘルパー。アプリ内で単一のインスタンスになるよう、staticとする。
     */
    private static volatile DatabaseHelper sHelper;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            // エンティティを指定してCREATE TABLE（テーブルを作成）します
            TableUtils.createTable(connectionSource, ChatTheme.class);
            TableUtils.createTable(connectionSource, Memo.class);
            TableUtils.createTable(connectionSource, CitationResource.class);
            TableUtils.createTable(connectionSource, MemoCitationResource.class); // メモ n : n 引用元文献 を実現する中間テーブル
        } catch (java.sql.SQLException e) {
            LogUtils.e("データベースを作成できませんでした", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // DBヴァージョンが上がった際の、DBのアップグレード処理
//        try {
//            while(++oldVersion <= newVersion) {
//                switch (oldVersion) {
//                    case 2: // DB ver. 2にあがった
//                        break;
//                    default:
//                        break;
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    public static synchronized DatabaseHelper getHelper(Context context) {
        if (sHelperReferenceCount.getAndIncrement() == 0) {
            sHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        }
        return sHelper;
    }

    public static synchronized void releaseHelper() {
        if (sHelperReferenceCount.decrementAndGet() <= 0) {
            sHelperReferenceCount.set(0);
            OpenHelperManager.releaseHelper();
        }
    }

    public static synchronized void destroyHelper() {
        OpenHelperManager.releaseHelper();
        sHelperReferenceCount.set(0);
    }

    @Override
    public void close() {
        super.close();
    }

}
