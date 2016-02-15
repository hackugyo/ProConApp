package jp.ne.hatena.hackugyo.procon.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.stmt.StatementBuilder;

import java.sql.SQLException;

/**
 * 多対多の中間テーブルを管理するクエリを準備するクラス。
 * @see <a href="https://github.com/j256/ormlite-jdbc/blob/master/src/test/java/com/j256/ormlite/examples/manytomany/ManyToManyMain.java">参考リンク</a>
 */
public class MiddleTableQueryBuilder<T extends MiddleModel> {

    private final Dao<T, Integer> middleTableDao;
    private final String idNameForFirst;
    private final String idNameForSecond;
    private PreparedDelete<T> deleteMiddleForFirstQuery;
    private PreparedDelete<T> deleteMiddleForSecondQuery;

    public MiddleTableQueryBuilder(Dao<T, Integer> dao, String idNameForFirst, String idNameForSecond ) {
        middleTableDao = dao;
        this.idNameForFirst = idNameForFirst;
        this.idNameForSecond = idNameForSecond;
    }

    public <U> int deleteMiddleForFirst(U first) throws SQLException {
        if (deleteMiddleForFirstQuery == null) {
            DeleteBuilder<T, Integer> queryBuilder = makeDeleteMiddleForFirstQuery();
            deleteMiddleForFirstQuery = queryBuilder.prepare();
        }
        deleteMiddleForFirstQuery.setArgumentHolderValue(0, first);
        return middleTableDao.delete(deleteMiddleForFirstQuery);
    }

    public <U> int deleteMiddleForSecond(U second) throws SQLException {
        if (deleteMiddleForSecondQuery == null) {
            DeleteBuilder<T, Integer> queryBuilder = makeDeleteMiddleForSecondQuery();
            deleteMiddleForSecondQuery = queryBuilder.prepare();
        }
        deleteMiddleForSecondQuery.setArgumentHolderValue(0, second);
        return middleTableDao.delete(deleteMiddleForSecondQuery);
    }


    /**
     * Build our query for CitationResource objects that match a Memo.
     */
    public <U> PreparedQuery<U> makeSecondsForFirstQuery(Dao<U, Integer> secondDao, String secondDaoIdFieldName) throws SQLException {
        QueryBuilder<U, Integer> secondQb = secondDao.queryBuilder();
        secondQb.where().in(secondDaoIdFieldName, makeSelectMiddleForFirstQuery());
        return secondQb.prepare();
    }

    public <U> PreparedQuery<U> makeSecondsForSecondQuery(Dao<U, Integer> firstDao, String firstDaoIdFieldName) throws SQLException {
        QueryBuilder<U, Integer> firstQb = firstDao.queryBuilder();
        firstQb.where().in(firstDaoIdFieldName, makeSelectMiddleForSecondQuery());
        return firstQb.prepare();
    }

    private QueryBuilder<T, Integer> makeSelectMiddleForFirstQuery() throws SQLException {
        QueryBuilder<T, Integer> middleQb = middleTableDao.queryBuilder();
        middleQb.selectColumns(idNameForSecond);
        makeMiddleForFirstQuery(middleQb);
        return middleQb;

    }
    private DeleteBuilder<T, Integer> makeDeleteMiddleForFirstQuery() throws SQLException {
        DeleteBuilder<T, Integer> middleQb = middleTableDao.deleteBuilder();
        makeMiddleForFirstQuery(middleQb);
        return middleQb;
    }


    private void makeMiddleForFirstQuery(StatementBuilder<T, Integer> middleQb) throws SQLException {
        SelectArg memoSelectArg = new SelectArg();
        middleQb.where().eq(idNameForFirst, memoSelectArg);
    }

    private QueryBuilder<T, Integer> makeSelectMiddleForSecondQuery() throws SQLException {
        QueryBuilder<T, Integer> middleQb = middleTableDao.queryBuilder();
        middleQb.selectColumns(idNameForFirst);
        makeMiddleForSecondQuery(middleQb);
        return middleQb;
    }


    private DeleteBuilder<T, Integer> makeDeleteMiddleForSecondQuery() throws SQLException {
        DeleteBuilder<T, Integer> middleQb = middleTableDao.deleteBuilder();
        makeMiddleForSecondQuery(middleQb);
        return middleQb;
    }

    private void makeMiddleForSecondQuery(StatementBuilder<T, Integer> middleQb) throws SQLException {
        SelectArg memoSelectArg = new SelectArg();
        middleQb.where().eq(idNameForSecond, memoSelectArg);
    }

}
