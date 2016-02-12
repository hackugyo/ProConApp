package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.util.LogUtils;

/**
 * Created by kwatanabe on 16/02/12.
 */
public class ChatThemeRepository {
    private DatabaseHelper dbHelper;
    private Dao<ChatTheme, Integer> chatThemeDao;


    public ChatThemeRepository(Context context) {
        dbHelper = DatabaseHelper.getHelper(context);
        try {
            chatThemeDao = dbHelper.getDao(ChatTheme.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void onResume(Context context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper.getHelper(context);
            try {
                chatThemeDao = dbHelper.getDao(ChatTheme.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPause() {
        DatabaseHelper.releaseHelper();
        dbHelper = null;
    }
    public boolean save(ChatTheme chatTheme) {
        Dao.CreateOrUpdateStatus result;
        try {
            result = chatThemeDao.createOrUpdate(chatTheme);
        } catch (SQLException e) {
            LogUtils.e("cannot save.", e);
            return false;
        }
        return result.isCreated() || result.isUpdated();
    }

    public int delete(ChatTheme chatTheme) {
        try {
            int i = chatThemeDao.delete(chatTheme);
            return i;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public List<ChatTheme> findAll() {
        QueryBuilder<ChatTheme, Integer> qb = chatThemeDao.queryBuilder();
        qb.orderBy(ChatTheme.TIMESTAMP_FIELD, true); // TODO 20160210 by own order

        PreparedQuery<ChatTheme> preparedQuery = null;
        try {
            preparedQuery = qb.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            return chatThemeDao.query(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<ChatTheme>();
    }
}
