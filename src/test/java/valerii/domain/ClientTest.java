package valerii.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import valerii.Error;
import valerii.db.*;
import valerii.exception.BusinessException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 * @author vliutyi
 */
class ClientTest {

    private IDbProvider provider;

    @BeforeEach
    void setUp() {
        provider = mock(IDbProvider.class);
        DbProvider.setProvider(provider);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByIdFound() throws SQLException {
        Map<String, DbValue> resultSet = new HashMap<>();
        resultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        resultSet.put("name", new DbValue(DbFieldType.STRING, "Bob"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(resultSet);

        Client client = Client.getById(1);

        assertNotNull(client);
        assertEquals(1, client.getId());
        assertEquals("Bob", client.getName());

        ArgumentCaptor<Map<String, DbValue>> inputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), inputDataCaptor.capture());

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
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(Collections.emptyMap());

        assertNull(Client.getById(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getByNameFound() throws SQLException {
        Map<String, DbValue> resultSet = new HashMap<>();
        resultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        resultSet.put("name", new DbValue(DbFieldType.STRING, "Bob"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(resultSet);

        Client client = Client.getByName("Bob");

        assertNotNull(client);
        assertEquals(1, client.getId());
        assertEquals("Bob", client.getName());

        ArgumentCaptor<Map<String, DbValue>> inputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), inputDataCaptor.capture());

        Map<String, DbValue> inputData = inputDataCaptor.getValue();
        assertEquals(1, inputData.size(), "Wrong number of input arguments");
        assertTrue(inputData.containsKey("name"));
        DbValue dbValue = inputData.entrySet().iterator().next().getValue();
        assertEquals(DbFieldType.STRING, dbValue.getType());
        assertEquals("Bob", dbValue.getValue());
    }

    @Test
    void getByNameNotFound() throws SQLException {
        Map<String, DbValue> resultSet = Collections.emptyMap();
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(resultSet);

        assertNull(Client.getByName("Alice"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createWithUniqueNameOK() throws SQLException, BusinessException {
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(Collections.emptyMap());
        when(provider.insert(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(1);

        Client client = Client.create("Alice");

        assertNotNull(client);
        assertEquals(1, client.getId());
        assertEquals("Alice", client.getName());

        // prove that duplicate name check was performed
        ArgumentCaptor<Map<String, DbValue>> selectInputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), selectInputDataCaptor.capture());
        Map<String, DbValue> inputData = selectInputDataCaptor.getValue();
        assertEquals(1, inputData.size(), "Wrong number of select input arguments");
        assertTrue(inputData.containsKey("name"));
        DbValue dbValue = inputData.entrySet().iterator().next().getValue();
        assertEquals(DbFieldType.STRING, dbValue.getType());
        assertEquals("Alice", dbValue.getValue());

        ArgumentCaptor<Map<String, DbValue>> insertInputDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(provider).insert(any(), eq(Table.CLIENT.getTableName()), insertInputDataCaptor.capture());

        Map<String, DbValue> insertInputData = insertInputDataCaptor.getValue();
        assertEquals(1, insertInputData.size(), "Wrong number of insert input arguments");
        assertTrue(insertInputData.containsKey("name"));
        dbValue = insertInputData.entrySet().iterator().next().getValue();
        assertEquals(DbFieldType.STRING, dbValue.getType());
        assertEquals("Alice", dbValue.getValue());

        verifyNoMoreInteractions(provider);
    }

    @Test
    void createWithSameNameFailed() throws SQLException, BusinessException {
        Map<String, DbValue> resultSet = new HashMap<>();
        resultSet.put("id", new DbValue(DbFieldType.INTEGER, 1));
        resultSet.put("name", new DbValue(DbFieldType.STRING, "Alice"));
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(resultSet);

        BusinessException exception = assertThrows(BusinessException.class, () -> Client.create("Alice"));
        assertEquals(Error.ERR_005.getCode(), exception.getError().getCode(), "Wrong error in exception");

        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), any());
        verifyNoMoreInteractions(provider);
    }

    @Test
    void createIfUnableToInsert() throws SQLException, BusinessException {
        when(provider.select(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(Collections.emptyMap());
        when(provider.insert(any(), eq(Table.CLIENT.getTableName()), any())).thenReturn(-1);

        Client client = Client.create("Alice");

        assertNull(client);

        verify(provider).select(any(), eq(Table.CLIENT.getTableName()), any());
        verify(provider).insert(any(), eq(Table.CLIENT.getTableName()), any());
        verifyNoMoreInteractions(provider);
    }
}