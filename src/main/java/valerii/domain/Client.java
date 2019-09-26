package valerii.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.Error;
import valerii.db.DbFieldType;
import valerii.db.DbProvider;
import valerii.db.DbValue;
import valerii.db.Table;
import valerii.exception.BusinessException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Business object holds all operations on client
 *
 * @author vliutyi
 */
public class Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(Account.class);

    private int id;
    private String name;

    private Client(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Search client by client id
     *
     * @param clientId client id to search
     * @return Client instance or null if client with given id is not found
     * @throws SQLException in case of DB errors
     */
    public static Client getById(int clientId) throws SQLException {
        Map<String, DbValue> select = new HashMap<>(1);
        select.put("id", new DbValue(DbFieldType.INTEGER, clientId));

        Map<String, DbValue> resultSet = DbProvider.select(Table.CLIENT.getTableName(), select);

        if (resultSet.isEmpty()) {
            return null;
        }

        int id = (int)resultSet.get("id").getValue();
        String name = (String)resultSet.get("name").getValue();

        return new Client(id, name);
    }

    /**
     * Search client by client name
     * @param clientName client name to search
     * @return Client instance or null if client with given id is not found
     * @throws SQLException in case of DB errors
     */
    public static Client getByName(String clientName) throws SQLException {
        Map<String, DbValue> select = new HashMap<>(1);
        select.put("name", new DbValue(DbFieldType.STRING, clientName));

        Map<String, DbValue> resultSet = DbProvider.select(Table.CLIENT.getTableName(), select);

        if (resultSet.isEmpty()) {
            return null;
        }

        int id = (int)resultSet.get("id").getValue();
        String name = (String)resultSet.get("name").getValue();

        return new Client(id, name);
    }

    /**
     * Create new client
     *
     * @param clientName name of the client to create
     * @return Client instance or null if client cannot be created
     * @throws SQLException in case of DB errors
     * @throws BusinessException in case of business logic errors
     */
    public static Client create(String clientName) throws SQLException, BusinessException {

        if (getByName(clientName) != null) {
            LOGGER.error(Error.ERR_005.getMsg());
            throw new BusinessException(Error.ERR_005);
        }

        Map<String, DbValue> values = new HashMap<>();
        values.put("name", new DbValue(DbFieldType.STRING, clientName));

        int id = DbProvider.insert(Table.CLIENT.getTableName(), values);

        if (id < 0) {
            return null;
        }

        return new Client(id, clientName);
    }
}
