package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.StatementBuilder;

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


    public MemoRepository(Context context) {
        dbHelper = DatabaseHelper.getHelper(context);
        try {
            memoDao = dbHelper.getDao(Memo.class);
            citationResourceDao = dbHelper.getDao(CitationResource.class);
            memoCitationResourceDao = dbHelper.getDao(MemoCitationResource.class);
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
        return lookupCitationResource(
                Arrays.asList(children.toArray(new Memo[children.size()]))
        );
    }

    /*******************************
     * 中間テーブル用のクエリ
     *******************************/

    private int deleteMemoCitationResource(Memo memo) throws SQLException {
        DeleteBuilder<MemoCitationResource, Integer> queryBuilder = makeDeleteMiddleForMemoQuery();
        PreparedDelete<MemoCitationResource> prepare = queryBuilder.prepare();
        prepare.setArgumentHolderValue(0, memo);
        return memoCitationResourceDao.delete(prepare);
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

    private PreparedQuery<Memo> memosForCitationResourceQuery = null;
    private PreparedQuery<CitationResource> citationResourcesForMemoQuery = null;

    private List<Memo> lookupMemosForCitationResource(CitationResource citationResource) throws SQLException {
        if (memosForCitationResourceQuery == null) {
            memosForCitationResourceQuery = makeMemosForCitationResourceQuery();
        }
        memosForCitationResourceQuery.setArgumentHolderValue(0, citationResource);
        return memoDao.query(memosForCitationResourceQuery);
    }

    private List<CitationResource> lookupCitationResourcesForMemo(Memo memo) throws SQLException {
        if (citationResourcesForMemoQuery == null) {
            citationResourcesForMemoQuery = makeCitationResourcesForMemoQuery();
        }
        citationResourcesForMemoQuery.setArgumentHolderValue(0, memo);
        return citationResourceDao.query(citationResourcesForMemoQuery);
    }

    /**
     * Build our query for CitationResource objects that match a Memo.
     */
    private PreparedQuery<CitationResource> makeCitationResourcesForMemoQuery() throws SQLException {

        // build our outer query for CitationResource objects
        QueryBuilder<CitationResource, Integer> citationResourceQb = citationResourceDao.queryBuilder();
        // where the id matches in the post-id from the inner query
        citationResourceQb.where().in(CitationResource.ID_FIELD_NAME, makeMiddleForMemoQuery());
        return citationResourceQb.prepare();
    }

    /**
     * Build our query for Memo objects that match a CitationResource
     */
    private PreparedQuery<Memo> makeMemosForCitationResourceQuery() throws SQLException {
        QueryBuilder<Memo, Integer> memoQb = memoDao.queryBuilder();
        memoQb.where().in(CitationResource.ID_FIELD_NAME, makeMiddleForMemoQuery());
        return memoQb.prepare();
    }
    /**
     * メモをセットしてもらえれば、中間テーブルのレコードを返すというクエリのビルダ。
     * @return
     * @throws SQLException
     */
    private QueryBuilder<MemoCitationResource, Integer> makeMiddleForMemoQuery() throws SQLException {
        QueryBuilder<MemoCitationResource, Integer> memoCitationResourceQb = memoCitationResourceDao.queryBuilder();
        memoCitationResourceQb.selectColumns(MemoCitationResource.CITATION_RESOURCE_ID_FIELD_NAME);
        makeMiddleForMemoQuery(memoCitationResourceQb);
        return memoCitationResourceQb;
    }

    private DeleteBuilder<MemoCitationResource, Integer> makeDeleteMiddleForMemoQuery() throws SQLException {
        DeleteBuilder<MemoCitationResource, Integer> memoCitationResourceQb = memoCitationResourceDao.deleteBuilder();
        makeMiddleForMemoQuery(memoCitationResourceQb);
        return memoCitationResourceQb;
    }


    private static void makeMiddleForMemoQuery(StatementBuilder<MemoCitationResource, Integer> memoCitationResourceQb) throws SQLException {
        SelectArg memoSelectArg = new SelectArg();
        memoCitationResourceQb.where().eq(MemoCitationResource.MEMO_ID_FIELD_NAME, memoSelectArg);
    }

    /**
     * 引用元資料をセットしてもらえれば、中間テーブルのレコードを返すというクエリのビルダ。
     * @return
     * @throws SQLException
     */
    private QueryBuilder<MemoCitationResource, Integer> makeMiddleForCitationResourceQuery() throws SQLException {
        QueryBuilder<MemoCitationResource, Integer> memoCitationResourceQb = memoCitationResourceDao.queryBuilder();
        memoCitationResourceQb.selectColumns(MemoCitationResource.MEMO_ID_FIELD_NAME);
        makeMiddleForCitationResourceQuery(memoCitationResourceQb);
        return memoCitationResourceQb;
    }

    private DeleteBuilder<MemoCitationResource, Integer> makeDeleteMiddleForCitationResourceQuery() throws SQLException {
        DeleteBuilder<MemoCitationResource, Integer> memoCitationResourceQb = memoCitationResourceDao.deleteBuilder();
        makeMiddleForCitationResourceQuery(memoCitationResourceQb);
        return memoCitationResourceQb;
    }
    private static void makeMiddleForCitationResourceQuery(StatementBuilder<MemoCitationResource, Integer> memoCitationResourceQb) throws SQLException {
        SelectArg citationResourceSelectArg = new SelectArg();
        memoCitationResourceQb.where().eq(MemoCitationResource.CITATION_RESOURCE_ID_FIELD_NAME, citationResourceSelectArg);
    }

}
