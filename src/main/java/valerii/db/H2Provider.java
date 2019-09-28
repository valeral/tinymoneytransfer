package valerii.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Specific provider for H2 database implements IDbProvider interface
 *
 * @author vliutyi
 */
public class H2Provider implements IDbProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2Provider.class);

    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MVCC=false";
    private static final String DB_USER = "me";
    private static final String DB_PASSWORD = "none";

    @Override
    public void createDBTables() throws SQLException {
        try (Connection connection = newDBConnection()) {

            String createClientQuery = "CREATE TABLE client(id int auto_increment primary key, name varchar(255) NOT NULL, UNIQUE KEY unique_name (name))";
            PreparedStatement clientStmt = connection.prepareStatement(createClientQuery);
            clientStmt.executeUpdate();
            clientStmt.close();

            String createAccountQuery = "CREATE TABLE account(id int auto_increment primary key, " +
                    "client_id int NOT NULL, " +
                    "currency varchar(3) NOT NULL, " +
                    "amount int NOT NULL DEFAULT 0, " +
                    "created_date TIMESTAMP DEFAULT NOW() NOT NULL, " +
                    "FOREIGN KEY (client_id) references client(id), " +
                    // TODO remove if client can have many accounts
                    "UNIQUE KEY unique_client (client_id) )";
            PreparedStatement accountStmt = connection.prepareStatement(createAccountQuery);
            accountStmt.executeUpdate();
            accountStmt.close();

//            String createClientAccountQuery = "CREATE TABLE client_account(" +
//                    "client_id int NOT NULL, " +
//                    "account_id int NOT NULL, " +
//                    "UNIQUE KEY unique_client_account (client_id, account_id), " +
//                    "FOREIGN KEY (client_id) references client(id), " +
//                    "FOREIGN KEY (account_id) references account(id) )";
//            PreparedStatement clientAccountStmt = connection.prepareStatement(createClientAccountQuery);
//            clientAccountStmt.executeUpdate();
//            clientAccountStmt.close();
        }
    }

    @Override
    public void dropDBTables() throws SQLException {
        try (Connection connection = newDBConnection()) {
            PreparedStatement statement = connection.prepareStatement("DROP TABLE account");
            statement.executeUpdate();
            statement.close();
            statement = connection.prepareStatement("DROP TABLE client");
            statement.executeUpdate();
            statement.close();
        }
    }

    @Override
    public int insert(Connection connection, String table, Map<String, DbValue> values) throws SQLException {

        String fieldNames = String.join(",", values.keySet());
        String valuePlaceHolders = Stream.generate(() -> "?")
                .limit(values.size())
                .collect(Collectors.joining(","));

        String insertQuery = "INSERT INTO " + table + " (" + fieldNames + ") values" + "(" + valuePlaceHolders + ")";
        int id;

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {

            setPlaceHolders(insertStmt, values);
            insertStmt.executeUpdate();
            ResultSet resultSet = insertStmt.getGeneratedKeys();

            if (!resultSet.next()) {
                return -1;
            }
            id = resultSet.getInt(1);
        }

        return id;
    }

    @Override
    public Map<String, DbValue> select(Connection connection, String table, Map<String, DbValue> values) throws SQLException {
        return select(connection, table, values, false);
    }

    @Override
    public Map<String, DbValue> selectForUpdate(Connection connection, String table, Map<String, DbValue> values) throws SQLException {
        return select(connection, table, values, true);
    }

    private Map<String, DbValue> select(Connection connection, String table, Map<String, DbValue> values, boolean forUpdate) throws SQLException {
        Map<String, DbValue> returnData;

        String selectQuery = "SELECT * FROM " + table + " WHERE " + makeKeyValueList(values);

        if (forUpdate) {
            selectQuery = selectQuery.concat(" FOR UPDATE");
        }

        try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {

            setPlaceHolders(selectStatement, values);
            ResultSet resultSet = selectStatement.executeQuery();

            if (!resultSet.next()) {
                return Collections.emptyMap();
            }

            int columnCount = resultSet.getMetaData().getColumnCount();
            returnData = new HashMap<>(columnCount);

            for (int i = 1; i <= columnCount; i++) {
                String name = resultSet.getMetaData().getColumnName(i).toLowerCase();
                DbFieldType type = mapToDbFieldType(resultSet.getMetaData().getColumnType(i));
                Object value = getColumnValue(resultSet, i, type);

                returnData.put(name, new DbValue(type, value));
            }
        }

        return returnData;
    }

    @Override
    public int update(Connection connection, String tableName, int id, Map<String, DbValue> update) throws SQLException {

        String updateQuery = "UPDATE " + tableName + " SET " + makeKeyValueList(update) + " WHERE id = ?";
        int rowsUpdated;

        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
            update.put("id", new DbValue(DbFieldType.INTEGER, id));

            setPlaceHolders(updateStatement, update);
            rowsUpdated = updateStatement.executeUpdate();
        }

        return rowsUpdated;
    }

    private String makeKeyValueList(Map<String, DbValue> values) {
        String where;

        if (values.size() == 1) {
            where = values.keySet().iterator().next() + "= ?";
        } else {
            where = String.join("= ?, ", values.keySet());
            where = where.substring(0, where.length() - 2); // remove last comma and whitespace
        }

        return where;
    }

    private Object getColumnValue(ResultSet resultSet, int i, DbFieldType type) throws SQLException {
        switch (type) {
            case STRING:
                return resultSet.getString(i);
            case INTEGER:
                return resultSet.getInt(i);
            case DATE_TIME:
                return resultSet.getTimestamp(i);
            default:
                throw new IllegalArgumentException("Unsupported column type" + type);
        }
    }

    private DbFieldType mapToDbFieldType(int columnType) {
        switch (columnType) {
            case Types.INTEGER:
                return DbFieldType.INTEGER;
            case Types.VARCHAR:
                return DbFieldType.STRING;
            case Types.TIMESTAMP:
                return DbFieldType.DATE_TIME;
            default:
                throw new IllegalArgumentException("Unsupported column type " + columnType);
        }
    }

    private void setPlaceHolders(PreparedStatement stmt, Map<String, DbValue> values) throws SQLException {
        int i = 1;
        for (Map.Entry<String, DbValue> entry : values.entrySet()) {
            setPlaceHolder(stmt, i++, entry.getValue());
        }
    }

    private void setPlaceHolder(PreparedStatement stmt, int i, DbValue value) throws SQLException {
        switch (value.getType()) {
            case STRING:
                stmt.setString(i, (String) value.getValue());
                break;
            case INTEGER:
                stmt.setInt(i, (int) value.getValue());
                break;
            case DATE_TIME:
                stmt.setTimestamp(i, Timestamp.valueOf((LocalDateTime) value.getValue()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported column type " + value.getType());
        }
    }

    @Override
    public Connection newDBConnection() {
        Connection dbConnection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return dbConnection;
    }
}
