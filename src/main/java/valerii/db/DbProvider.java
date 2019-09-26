package valerii.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Facade for all DB interactions. All calls redirected to actual DBProvider that was set
 *
 * @author vliutyi
 */
public class DbProvider {

    // actual DB provider
    private static IDbProvider provider;
    // stores thread specific db connection
    private static ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

    /**
     * Set DB provider to use
     * @param provider instance of IDBProvider
     */
    public static void setProvider(IDbProvider provider) {
        DbProvider.provider = provider;
    }

    /**
     * Returns db connection specific to current thread
     * @return thread specific Connection object
     */
    private static Connection getConnection() {
        return threadConnection.get();
    }

    /**
     * Sets connection for current thread
     * @param connection the db connection instance
     */
    public static void setThreadConnection(Connection connection) {
        threadConnection.set(connection);
    }

    /**
     * Gets new connection from DB connection pool
     * @return new connection object
     */
    public static Connection newDBConnection() {
        return provider.newDBConnection();
    }

    /**
     * Executes scripts that create all needed table in DB
     * @throws SQLException in case of DB script errors
     */
    public static void createDBTables() throws SQLException {
        provider.createDBTables();
    }

    /**
     * Drops all tables that were created in createdDBTables() call
     * @throws SQLException in case of DB script errors
     */
    public static void dropDBTables() throws SQLException {
        provider.dropDBTables();
    }

    /**
     * Inserts values into DB. Values are stored in a map that contains field name as a key and DbValue as a type and value to be inserted
     * @param table table name to insert data
     * @param values a map with field names and values to insert
     * @return the id of the insterted record or -1 in case of insert wasn't performed
     * @throws SQLException in case of DB errors while inserting
     */
    public static int insert(String table, Map<String, DbValue> values) throws SQLException {
        return provider.insert(getConnection(), table, values);
    }

    /**
     * Selects data from DB table. Select criteria are stored in a map that contains field name as a key and DbValue as a type and value
     * @param table queried table name
     * @param values query criteria
     * @return a map with result data set. The map format is the same as criteria map
     * @throws SQLException in case of DB errors
     */
    public static Map<String, DbValue> select(String table, Map<String, DbValue> values) throws SQLException {
        return provider.select(getConnection(), table, values);
    }

    /**
     * Selects data from DB table and locks the records for further update. Select criteria are stored in a map that contains field name as a key and DbValue as a type and value
     * @param table queried table name
     * @param values query criteria
     * @return a map with result data set. The map format is the same as criteria map
     * @throws SQLException in case of DB errors
     */
    public static Map<String, DbValue> selectForUpdate(String table, Map<String, DbValue> values) throws SQLException {
        return provider.selectForUpdate(getConnection(), table, values);
    }

    /**
     * Updates single record in DB specified by its id. New values for update are stored in a map that contains field name as a key and DbValue as a type and value
     * @param table table name to be updated
     * @param update map of field name and values to update
     * @return number of updated record. As we update only 1 record this will contain either 1 or 0 in case of the updated wasn't performed
     * @throws SQLException in case of DB errors
     */
    public static int update(String table, int id, Map<String, DbValue> update) throws SQLException {
        return provider.update(getConnection(), table, id, update);
    }
}
