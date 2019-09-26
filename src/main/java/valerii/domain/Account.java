package valerii.domain;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.Error;
import valerii.db.DbFieldType;
import valerii.db.DbProvider;
import valerii.db.DbValue;
import valerii.db.Table;
import valerii.exception.BusinessException;
import valerii.exception.TransferException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Business object that holds logic to operate on client account
 *
 * @author vliutyi
 */
public class Account {

    private static final Logger LOGGER = LoggerFactory.getLogger(Account.class);

    private static final Object TRANSFER_LOCK_OBJECT = new Object();
    private int id;
    private int clientId;
    private Currency currency;
    private int amount;
    private LocalDateTime createdDate;

    private Account(int id, int clientId, Currency currency, int amount, LocalDateTime createdDate) {
        this.id = id;
        this.clientId = clientId;
        this.currency = currency;
        this.amount = amount;
        this.createdDate = createdDate;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getAmount() {
        return amount;
    }

    private void setAmount(int amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Search account by its id
     * @param accountId account id to search
     * @return account instance or null if not found
     * @throws SQLException in case of DB errors
     */
    public static Account getById(int accountId) throws SQLException {
        return getById(accountId, false);
    }

    /**
     * Search and try to lock DB record for further update. If other thread already holds the lock
     * this will be suspended until other thread close transaction.
     * @param accountId account id to search
     * @return account instance or null if not found
     * @throws SQLException in case of DB errors
     */
    private static Account lockById(int accountId) throws SQLException {
        return getById(accountId, true);
    }

    /**
     * private version of getById() method with needLock flag to decide if locking of the record for further updates is needed or not
     */
    private static Account getById(int accountId, boolean needLock) throws SQLException {
        Map<String, DbValue> values = new HashMap<>();
        values.put("id", new DbValue(DbFieldType.INTEGER, accountId));

        Map<String, DbValue> resultSet;

        if (needLock) {
            resultSet = DbProvider.selectForUpdate(Table.ACCOUNT.getTableName(), values);
        } else {
            resultSet = DbProvider.select(Table.ACCOUNT.getTableName(), values);
        }

        if(resultSet.isEmpty()) {
            return null;
        }

        int id = (int)resultSet.get("id").getValue();
        int clientId = (int)resultSet.get("client_id").getValue();
        int amount = (int)resultSet.get("amount").getValue();
        Currency currency = Currency.valueOf((String)resultSet.get("currency").getValue());
        Timestamp date = (Timestamp)resultSet.get("created_date").getValue();

        return new Account(id, clientId, currency, amount, date.toLocalDateTime());
    }

    // for simplicity let client can have only one account
    public static Account getByClient(int clientId) throws SQLException {
        Map<String, DbValue> values = new HashMap<>();
        values.put("client_id", new DbValue(DbFieldType.INTEGER, clientId));

        Map<String, DbValue> resultSet = DbProvider.select(Table.ACCOUNT.getTableName(), values);

        if(resultSet.isEmpty()) {
            return null;
        }

        int id = (int)resultSet.get("id").getValue();
        int amount = (int)resultSet.get("amount").getValue();
        Currency currency = Currency.valueOf((String)resultSet.get("currency").getValue());
        Timestamp date = (Timestamp)resultSet.get("created_date").getValue();

        return new Account(id, clientId, currency, amount, date.toLocalDateTime());
    }

    /**
     * Create new account for client. For simplicity lets assume a client can have only one account
     *
     * @param clientId client id of the account owner
     * @param currency currency of the the account
     * @return instance of the Account or null if account cannot be created
     * @throws SQLException in case of DB errors
     */
    public static Account create(int clientId, Currency currency) throws SQLException, BusinessException {

        if (Client.getById(clientId) == null) {
            LOGGER.error(Error.ERR_001.getMsg());
            throw new BusinessException(Error.ERR_001);
        }

        // for simplicity let client can have only one account
        if (getByClient(clientId) != null) {
            LOGGER.error(Error.ERR_008.getMsg());
            throw new BusinessException(Error.ERR_008);
        }

        Map<String, DbValue> values = new HashMap<>();
        values.put("client_id", new DbValue(DbFieldType.INTEGER, clientId));
        values.put("currency", new DbValue(DbFieldType.STRING, currency.toString()));
        values.put("amount", new DbValue(DbFieldType.INTEGER, 0));
        LocalDateTime createdDate = LocalDateTime.now();
        values.put("created_date", new DbValue(DbFieldType.DATE_TIME, createdDate));

        int id = DbProvider.insert(Table.ACCOUNT.getTableName(), values);

        if (id < 0) {
            return null;
        }

        return new Account(id, clientId, currency, 0, createdDate);
    }

    /**
     * Change current amount in account by provided amountDiff. Public version of the method locks
     * account DB record to avoid simultaneous update from different threads.
     *
     * @param amountDiff amount to add (positive number) or withdraw (negative value)
     * @return returns true if update was successful, false otherwise
     * @throws SQLException in case of DB errors
     * @throws TransferException in case of business logic errors
     */
    public boolean updateAmount(int amountDiff) throws SQLException, TransferException {
        return updateAmount(amountDiff, true);
    }

    /**
     * Private version of updateAmount() has a boolean flag to decide if locking of the record is needed.
     */
    private boolean updateAmount(int amountDiff, boolean needLock) throws SQLException, TransferException {
        Account account;
        // after lock is acquired data can differ from the one in current object
        if (needLock) {
            account = Account.lockById(getId());
        } else {
            account = this;
        }

        int newAmount = account.getAmount() + amountDiff;
        if (newAmount < 0) {
            throw new TransferException(Error.ERR_014);
        }

        Map<String, DbValue> update = new HashMap<>();
        update.put("amount", new DbValue(DbFieldType.INTEGER, newAmount));

        int rowsUpdated = DbProvider.update(Table.ACCOUNT.getTableName(), getId(), update);

        boolean success = rowsUpdated == 1;
        if (success) {
            account.setAmount(newAmount);
            //sync amount in current instance with actual quantity
            if (needLock) {
                setAmount(newAmount);
            }
        }

        return success;
    }

    /**
     * Transfer given amount from this account to another
     * @param dstAccountId destination account id
     * @param amount amount to transfer
     * @throws SQLException in case of DB errors
     * @throws TransferException in case of business logic errors
     */
    public void transferTo(int dstAccountId, int amount) throws SQLException, TransferException {

        if (getId() == dstAccountId) {
            LOGGER.error(Error.ERR_020.getMsg());
            throw new TransferException(Error.ERR_020);
        }
        // lock both accounts records for side updates
        // to lock src account record we need to get its data again (data can differ from the one in current object)
        Account srcAccountLocked;
        Account dstAccount;
        // synchronize locking of both records to avoid deadlock on DB
        // we can get either both lock or none (will wait for lock is released)
        synchronized (TRANSFER_LOCK_OBJECT) {
            srcAccountLocked = Account.lockById(getId());
            dstAccount = Account.lockById(dstAccountId);
        }

        if (dstAccount == null) {
            LOGGER.error(Error.ERR_019.getMsg());
            throw new TransferException(Error.ERR_019);
        }

        if (srcAccountLocked.getCurrency() != dstAccount.getCurrency()) {
            LOGGER.error(Error.ERR_021.getMsg());
            throw new TransferException(Error.ERR_021);
        }

        // update accounts without locking as we already have locked it
        if (srcAccountLocked.updateAmount(-amount, false)) {
            if (!dstAccount.updateAmount(amount, false)) {
                // rollback to original amount
                // value in DB will be rolled back by aborted transaction
                // but to have the DB in consistent state rollback this value manually
                srcAccountLocked.updateAmount(amount, false);
                LOGGER.error(Error.ERR_023.getMsg());
                throw new TransferException(Error.ERR_023);
            }
            // sync amount with current instance
            setAmount(srcAccountLocked.getAmount());
        } else {
            LOGGER.error(Error.ERR_024.getMsg());
            throw new TransferException(Error.ERR_024);
        }
    }
}