package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kwatanabe on 16/02/09.
 */
public class MemoRepository {
    private DatabaseHelper dbHelper;
    private Dao<Memo, Integer> memoDao;


    public MemoRepository(Context context) {
        dbHelper = DatabaseHelper.getHelper(context);
        try {
            memoDao = dbHelper.getDao(Memo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void onResume(Context context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper.getHelper(context);
            try {
                memoDao = dbHelper.getDao(Memo.class);
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
        } catch (SQLException e) {
            Log.e("MapMemo", "cannot save.", e);
            return false;
        }
        return result.isCreated() || result.isUpdated();
    }

    public int delete(Memo memo) {
        // helperからDaoクラスの取得
        // DaoクラスからこのMemoインスタンス自身を消去する
        try {
            return memoDao.delete(memo);
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
            return memoDao.query(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<Memo>();
    }
}
