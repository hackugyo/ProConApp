package jp.ne.hatena.hackugyo.procon.model;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.hackugyo.procon.MainActivity;

/**
 * Created by kwatanabe on 16/02/12.
 */
public class CitationResourceRepository {
    private DatabaseHelper dbHelper;
    private Dao<CitationResource, Integer> citationResourceDao;
    public CitationResourceRepository(Context context) {
        dbHelper = DatabaseHelper.getHelper(context);
        try {
            citationResourceDao = dbHelper.getDao(CitationResource.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }




    public void onResume(Context context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper.getHelper(context);
            try {
                citationResourceDao = dbHelper.getDao(CitationResource.class);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void onPause() {
        DatabaseHelper.releaseHelper();
        dbHelper = null;
    }

    public List<CitationResource> findAll() {
        QueryBuilder<CitationResource, Integer> qb = citationResourceDao.queryBuilder();
        qb.orderBy(CitationResource.ID_FIELD_NAME, true); // TODO 20160210 by own order

        PreparedQuery<CitationResource> preparedQuery = null;
        try {
            preparedQuery = qb.prepare();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            return citationResourceDao.query(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<CitationResource>();
    }
}
