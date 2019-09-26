package valerii.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * The interface specific DB provider must implement
 *
 * @author vliutyi
 */
public interface IDbProvider {

    void createDBTables() throws SQLException;
    void dropDBTables() throws SQLException;

    Connection newDBConnection();

    int insert(Connection connection, String table, Map<String, DbValue> values) throws SQLException;

    Map<String, DbValue> select(Connection connection, String table, Map<String, DbValue> values) throws SQLException;
    Map<String, DbValue> selectForUpdate(Connection connection, String table, Map<String, DbValue> values) throws SQLException;

    int update(Connection connection, String tableName, int id, Map<String, DbValue> update) throws SQLException;

}
