package valerii.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import valerii.Error;
import valerii.db.*;
import valerii.exception.BusinessException;
import valerii.exception.TransferException;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 * @author vliutyi
 */
class AccountTest {

    private IDbProvider provider;

    @BeforeEach
    void setUp() {
        provider = mock(IDbProvider.class);
        DbProvider.setProvider(provider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByIdOK() throws SQLException {
        Map<String, DbValue> resultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);

        Account account = Account.getById(1);

        assertNotNull(account);
        assertEquals(1, account.getId());
        assertEquals(1, account.getClientId());
        assertEquals(100, account.getAmount());
        assertEquals(Currency.RUB, account.getCurrency());

        ArgumentCaptor<Map<String, DbValue>> inputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), inputDataCaptor.capture());

        Map<String, DbValue> inputData = inputDataCaptor.getValue();
        assertEquals(1, inputData.size(), "Wrong number of input arguments");
        assertTrue(inputData.containsKey("id"));
        DbValue dbValue = inputData.entrySet().iterator().next().getValue();
        assertEquals(DbFieldType.INTEGER, dbValue.getType());
        assertEquals(1, dbValue.getValue());

        verifyNoMoreInteractions(provider);
    }

    @Test
    void getByIdNotFound() throws SQLException {
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(Collections.emptyMap());

        assertNull(Account.getById(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByClientFound() throws SQLException {
        Map<String, DbValue> resultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);

        Account account = Account.getByClient(1);

        assertNotNull(account);
        assertEquals(1, account.getId());
        assertEquals(1, account.getClientId());
        assertEquals(100, account.getAmount());
        assertEquals(Currency.RUB, account.getCurrency());

        ArgumentCaptor<Map<String, DbValue>> inputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), inputDataCaptor.capture());

        Map<String, DbValue> inputData = inputDataCaptor.getValue();
        assertEquals(1, inputData.size(), "Wrong number of input arguments");
        assertTrue(inputData.containsKey("client_id"));
        DbValue dbValue = inputData.entrySet().iterator().next().getValue();
        assertEquals(DbFieldType.INTEGER, dbValue.getType());
        assertEquals(1, dbValue.getValue());

        verifyNoMoreInteractions(provider);
    }

    @Test
    void getByClientNotFound() throws SQLException {
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(Collections.emptyMap());

        assertNull(Account.getByClient(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createOK() throws SQLException, BusinessException {
        Map<String, DbValue> clientResultSet = new HashMap<>();
        clientResultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        clientResultSet.put("name", new DbValue(DbFieldType.STRING, "Bob"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(clientResultSet);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(Collections.emptyMap());
        when(provider.insert(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(10);

        Account account = Account.create(1, Currency.RUB);

        assertNotNull(account);
        assertEquals(10, account.getId());
        assertEquals(1, account.getClientId());
        assertEquals(Currency.RUB, account.getCurrency());
        assertEquals(0, account.getAmount());
        assertTrue(account.getCreatedDate().isAfter(LocalDateTime.now().minusMinutes(1)));

        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), any());
        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());
        ArgumentCaptor<Map<String, DbValue>> insertInputArgsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).insert(any(), eq(Table.ACCOUNT.getTableName()), insertInputArgsCaptor.capture());
        Map<String, DbValue> insertData = insertInputArgsCaptor.getValue();
        assertEquals(4, insertData.size());
        assertTrue(insertData.containsKey("client_id"));
        assertEquals(DbFieldType.INTEGER, insertData.get("client_id").getType());
        assertEquals(1, insertData.get("client_id").getValue());
        assertTrue(insertData.containsKey("currency"));
        assertEquals(DbFieldType.STRING, insertData.get("currency").getType());
        assertEquals(Currency.RUB.toString(), insertData.get("currency").getValue());
        assertTrue(insertData.containsKey("amount"));
        assertEquals(DbFieldType.INTEGER, insertData.get("amount").getType());
        assertEquals(0, insertData.get("amount").getValue());
        assertTrue(insertData.containsKey("created_date"));
        assertEquals(DbFieldType.DATE_TIME, insertData.get("created_date").getType());

        verifyNoMoreInteractions(provider);
    }

    @Test
    void createWithUnknownClientFails() throws SQLException {
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(Collections.emptyMap());

        BusinessException exception = assertThrows(BusinessException.class, () -> Account.create(1, Currency.RUB));
        assertEquals(Error.ERR_001.getCode(), exception.getError().getCode());
    }

    @Test
    void createIfClientHasAccountFails() throws SQLException, BusinessException {
        Map<String, DbValue> clientResultSet = new HashMap<>();
        clientResultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        clientResultSet.put("name", new DbValue(DbFieldType.STRING, "Bob"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(clientResultSet);

        Map<String, DbValue> clientAccountResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(clientAccountResultSet);

        BusinessException exception = assertThrows(BusinessException.class, () -> Account.create(1, Currency.RUB));
        assertEquals(Error.ERR_008.getCode(), exception.getError().getCode());
    }

    @Test
    void createIfInsertFailed() throws SQLException, BusinessException {
        Map<String, DbValue> clientResultSet = new HashMap<>();
        clientResultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        clientResultSet.put("name", new DbValue(DbFieldType.STRING, "Bob"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(clientResultSet);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(Collections.emptyMap());
        when(provider.insert(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(-1);

        Account account = Account.create(1, Currency.RUB);

        assertNull(account);

        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), any());
        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());
        verify(provider).insert(any(), eq(Table.ACCOUNT.getTableName()), any());
        verifyNoMoreInteractions(provider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAmountOK() throws SQLException, TransferException {
        Map<String, DbValue> resultSet = createResultSetForAccount(1, 2, 100, Currency.USD);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);
        when(provider.update(any(), eq(Table.ACCOUNT.getTableName()), eq(1), any())).thenReturn(1);

        Account account = Account.getById(1);
        assertTrue(account.updateAmount(10));
        assertEquals(110, account.getAmount(), "Wrong amount in updated account");

        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());
        verify(provider).selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any());
        ArgumentCaptor<Map<String, DbValue>> updateInputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).update(any(), eq(Table.ACCOUNT.getTableName()), eq(1), updateInputDataCaptor.capture());
        Map<String, DbValue> updateInputData = updateInputDataCaptor.getValue();
        assertEquals(1, updateInputData.size());
        assertTrue(updateInputData.containsKey("amount"));
        assertEquals(110, updateInputData.get("amount").getValue());

        verifyNoMoreInteractions(provider);
    }

    @Test
    void updateAmountInsufficientAmountFails() throws SQLException, TransferException {
        Map<String, DbValue> resultSet = createResultSetForAccount(1, 1, 100, Currency.EUR);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);

        Account account = Account.getById(1);
        TransferException exception = assertThrows(TransferException.class, () -> account.updateAmount(-200));
        assertEquals(Error.ERR_014.getCode(), exception.getError().getCode());

        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());
        verify(provider).selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any());
        verifyNoMoreInteractions(provider);
    }

    @Test
    void updateAmountUpdateFailed() throws SQLException, TransferException {
        Map<String, DbValue> resultSet = createResultSetForAccount(2, 1, 50, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(resultSet);
        when(provider.update(any(), eq(Table.ACCOUNT.getTableName()), eq(2), any())).thenReturn(-1);

        Account account = Account.getById(2);
        assertFalse(account.updateAmount(10));
        assertEquals(50, account.getAmount(), "Wrong amount in not updated account");

        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());
        verify(provider).selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any());
        verify(provider).update(any(), eq(Table.ACCOUNT.getTableName()), eq(2), any());
        verifyNoMoreInteractions(provider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void transferToExistingAccountOK() throws SQLException, TransferException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);
        Map<String, DbValue> dstAccResultSet = createResultSetForAccount(2, 2, 100, Currency.RUB);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet, dstAccResultSet);
        when(provider.update(any(), eq(Table.ACCOUNT.getTableName()), anyInt(), any())).thenReturn(1);

        Account srcAccount = Account.getById(1);
        srcAccount.transferTo(2, 10);

        assertEquals(90, srcAccount.getAmount(), "Wrong amount in src account");
        verify(provider).select(any(), eq(Table.ACCOUNT.getTableName()), any());

        // check src and dst accounts were locked
        ArgumentCaptor<Map<String, DbValue>> lockInputCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider, times(2)).selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), lockInputCaptor.capture());
        List<Map<String, DbValue>> lockInput = lockInputCaptor.getAllValues();
        assertEquals(2, lockInput.size(), "Incorrect number of locks");
        assertEquals(1, lockInput.get(0).get("id").getValue()); //src account lock
        assertEquals(2, lockInput.get(1).get("id").getValue()); //dst account lock

        // check src account was updated correctly
        ArgumentCaptor<Map<String, DbValue>> srcUpdateInputCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).update(any(), eq(Table.ACCOUNT.getTableName()), eq(1), srcUpdateInputCaptor.capture());
        assertTrue(srcUpdateInputCaptor.getValue().containsKey("amount"));
        assertEquals(90, srcUpdateInputCaptor.getValue().get("amount").getValue(), "Wrong amount for src account update");

        // check dst account was updated correctly
        ArgumentCaptor<Map<String, DbValue>> dstUpdateInputCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).update(any(), eq(Table.ACCOUNT.getTableName()), eq(2), dstUpdateInputCaptor.capture());
        assertTrue(dstUpdateInputCaptor.getValue().containsKey("amount"));
        assertEquals(110, dstUpdateInputCaptor.getValue().get("amount").getValue(), "Wrong amount for dst account update");

        verifyNoMoreInteractions(provider);
    }

    @Test
    public void transferToSameAccountFails() throws SQLException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);

        TransferException exception = assertThrows(TransferException.class, () -> Account.getById(1).transferTo(1, 10));
        assertEquals(Error.ERR_020.getCode(), exception.getError().getCode(), "Wrong exception while transfer to same account");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void transferToNonExistingAccountFails() throws SQLException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet, Collections.emptyMap());

        TransferException exception = assertThrows(TransferException.class, () -> Account.getById(1).transferTo(2, 10));
        assertEquals(Error.ERR_019.getCode(), exception.getError().getCode(), "Wrong exception while transfer to non-existing account");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void transferWithDifferentCurrenciesFails() throws SQLException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        Map<String, DbValue> dstAccResultSet = createResultSetForAccount(2, 2, 100, Currency.EUR);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet, dstAccResultSet);

        TransferException exception = assertThrows(TransferException.class, () -> Account.getById(1).transferTo(2, 10));
        assertEquals(Error.ERR_021.getCode(), exception.getError().getCode(), "Wrong exception while transfer to non-existing account");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void transferWithSrcAccountUpdateError() throws SQLException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);
        Map<String, DbValue> dstAccResultSet = createResultSetForAccount(2, 2, 100, Currency.RUB);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet, dstAccResultSet);
        when(provider.update(any(), eq(Table.ACCOUNT.getTableName()), anyInt(), any())).thenReturn(0);

        Account srcAccount = Account.getById(1);
        TransferException exception = assertThrows(TransferException.class, () -> srcAccount.transferTo(2, 10));
        assertEquals(Error.ERR_024.getCode(), exception.getError().getCode(), "Wrong exception when src account update failed");
        assertEquals(100, srcAccount.getAmount());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void transferWithDstAccountUpdateError() throws SQLException {
        Map<String, DbValue> srcAccResultSet = createResultSetForAccount(1, 1, 100, Currency.RUB);
        when(provider.select(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet);
        Map<String, DbValue> dstAccResultSet = createResultSetForAccount(2, 2, 100, Currency.RUB);
        when(provider.selectForUpdate(any(), eq(Table.ACCOUNT.getTableName()), any())).thenReturn(srcAccResultSet, dstAccResultSet);
        when(provider.update(any(), eq(Table.ACCOUNT.getTableName()), anyInt(), any())).thenReturn(1, 0);

        Account srcAccount = Account.getById(1);
        TransferException exception = assertThrows(TransferException.class, () -> srcAccount.transferTo(2, 10));
        assertEquals(Error.ERR_023.getCode(), exception.getError().getCode(), "Wrong exception when dst account update failed");
        assertEquals(100, srcAccount.getAmount());
    }


    private Map<String, DbValue> createResultSetForAccount(int id, int clientId, int amount, Currency currency) {
        Map<String, DbValue> resultSet = new HashMap<>();
        resultSet.put("id", new DbValue(DbFieldType.INTEGER, id));
        resultSet.put("client_id", new DbValue(DbFieldType.INTEGER, clientId));
        resultSet.put("amount", new DbValue(DbFieldType.INTEGER, amount));
        resultSet.put("currency", new DbValue(DbFieldType.STRING, currency.toString()));
        resultSet.put("created_date", new DbValue(DbFieldType.DATE_TIME, Timestamp.valueOf(LocalDateTime.now())));

        return resultSet;
    }
}