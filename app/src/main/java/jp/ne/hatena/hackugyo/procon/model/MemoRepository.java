package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.ArrayUtils;
import jp.ne.hatena.hackugyo.procon.util.LogUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by kwatanabe on 16/02/09.
 */
public class MemoRepository {
    private DatabaseHelper dbHelper;
    private Dao<Memo, Integer> memoDao;
    private Dao<CitationResource, Integer> citationResourceDao;
    private Dao<MemoCitationResource, Integer> memoCitationResourceDao;
    private MiddleTableQueryBuilder<MemoCitationResource> memoCitationResourceQueryBuilder;


    public MemoRepository(Context context) {
        dbHelper = DatabaseHelper.getHelper(context);
        try {
            memoDao = dbHelper.getDao(Memo.class);
            citationResourceDao = dbHelper.getDao(CitationResource.class);
            memoCitationResourceDao = dbHelper.getDao(MemoCitationResource.class);
            memoCitationResourceQueryBuilder = new MiddleTableQueryBuilder<MemoCitationResource>(memoCitationResourceDao, MemoCitationResource.MEMO_ID_FIELD_NAME, MemoCitationResource.CITATION_RESOURCE_ID_FIELD_NAME);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void onResume(Context context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper.getHelper(context);
            try {
                memoDao = dbHelper.getDao(Memo.class);
                citationResourceDao = dbHelper.getDao(CitationResource.class);
                memoCitationResourceDao = dbHelper.getDao(MemoCitationResource.class);
                memoCitationResourceQueryBuilder = new MiddleTableQueryBuilder<MemoCitationResource>(memoCitationResourceDao, MemoCitationResource.MEMO_ID_FIELD_NAME, MemoCitationResource.CITATION_RESOURCE_ID_FIELD_NAME);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPause() {
        DatabaseHelper.releaseHelper();
        dbHelper = null;
    }

    public boolean save(Memo memo) {
        Dao.CreateOrUpdateStatus result;
        try {
            result = memoDao.createOrUpdate(memo);
            deleteMemoCitationResource(memo);
            if (ArrayUtils.any(memo.getCitationResources())) {
                for (CitationResource cr : memo.getCitationResources()) {
                    citationResourceDao.createOrUpdate(cr);
                    memoCitationResourceDao.create(new MemoCitationResource(memo, cr));
                }
            }
        } catch (SQLException e) {
            LogUtils.e("cannot save.", e);
            return false;
        }
        return result.isCreated() || result.isUpdated();
    }

    public int delete(Memo memo) {
        // helperからDaoクラスの取得
        // DaoクラスからこのMemoインスタンス自身を消去する
        try {
            int i = memoDao.delete(memo);
            if (i == 1) {
                deleteMemoCitationResource(memo);
                return i;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Memo> findAll() {
        QueryBuilder<Memo, Integer> qb = memoDao.queryBuilder();
        qb.orderBy(Memo.TIMESTAMP_FIELD, true); // TODO 20160210 by own order

        PreparedQuery<Memo> preparedQuery = null;
        try {
            preparedQuery = qb.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            return lookupCitationResource(memoDao.query(preparedQuery));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<Memo>();
    }

    public List<Memo> loadFromChatTheme(ChatTheme chatTheme) {
        if (chatTheme == null) return null;
        ForeignCollection<Memo> children = chatTheme.getChildren();
        if (children == null) return new ArrayList<>();
        return lookupCitationResource(
                Arrays.asList(children.toArray(new Memo[children.size()]))
        );
    }

    /*******************************
     * 中間テーブル用のクエリ
     *******************************/

    private int deleteMemoCitationResource(Memo memo) throws SQLException {
        return memoCitationResourceQueryBuilder.deleteMiddleForFirst(memo);
    }

    private List<Memo> lookupCitationResource(List<Memo> memos) {
        return Observable.from(memos)
                .observeOn(Schedulers.io())
                .map(new Func1<Memo, Memo>() {
                    @Override
                    public Memo call(Memo memo) {
                        try {
                            memo.setCitationResources(lookupCitationResourcesForMemo(memo));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        return memo;
                    }
                })
                .toList()
                .toBlocking()
                .single();
    }

    private PreparedQuery<CitationResource> citationResourcesForMemoQuery = null;

    private List<CitationResource> lookupCitationResourcesForMemo(Memo memo) throws SQLException {
        if (citationResourcesForMemoQuery == null) {
            citationResourcesForMemoQuery = memoCitationResourceQueryBuilder.makeSecondsForFirstQuery(citationResourceDao, CitationResource.ID_FIELD_NAME);
        }
        citationResourcesForMemoQuery.setArgumentHolderValue(0, memo);
        return citationResourceDao.query(citationResourcesForMemoQuery);
    }

}
