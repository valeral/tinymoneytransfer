package valerii;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import valerii.db.DbProvider;
import valerii.db.H2Provider;
import valerii.resources.transport.TAccount;
import valerii.resources.transport.TClient;
import valerii.resources.transport.TDebitWithdraw;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author vliutyi
 */
public class ITTestBase {

    static final String ENDPOINT_CLIENTS = "clients";
    static final String ENDPOINT_TRANSFERS = "transfers";
    private static final String ENDPOINT_ROOT = "http://localhost:9999/api";

    private static Server server;
    static WebTarget webTarget = ClientBuilder.newClient().target(ENDPOINT_ROOT);

    @BeforeAll
    public static void startServer() throws Exception {
        server = Main.initServer(9999);
        DbProvider.setProvider(new H2Provider());
        DbProvider.createDBTables();

        server.start();
    }

    @AfterAll
    public static void stopServer() throws Exception {
        server.stop();
        DbProvider.dropDBTables();
    }

    Response postNewClient(String name) {
        Invocation.Builder builder = webTarget.path(ENDPOINT_CLIENTS).request(MediaType.APPLICATION_JSON);
        return builder.post(Entity.json(new TClient(name)));
    }

    Response postNewAccount(int clientId, String currency) {
        Invocation.Builder builder = webTarget.path(ENDPOINT_CLIENTS + "/" + clientId + "/account").request(MediaType.APPLICATION_JSON);
        return builder.post(Entity.json(new TAccount(clientId, currency)));
    }

    Response postDebitWithdrawAccount(TAccount account, Integer amount) {
        return postDebitWithdrawAccount(account.getClientId(), account.getId(), amount);
    }

    Response postDebitWithdrawAccount(int clientId, int accountId, Integer amount) {
        Invocation.Builder builder = webTarget.path(createURLForClientAccount(clientId, accountId)).request(MediaType.APPLICATION_JSON);
        return builder.put(Entity.json(new TDebitWithdraw(amount)));
    }

    TAccount createAccountForNewClient(String currency) {
        Response clientResponse = postNewClient(createUniqueName());
        TClient client = clientResponse.readEntity(TClient.class);
        Response accountResponse = postNewAccount(client.getId(), currency);
        return accountResponse.readEntity(TAccount.class);
    }

    TAccount getAccountInfo(TAccount account) {
        Invocation.Builder builder = webTarget.path(account.getHref()).request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return response.readEntity(TAccount.class);
    }

    String createUniqueName() {
        return "Client" + new Random().nextInt(1000);
    }

    String createURLForClientAccount(int clientId, int accountId) {
        return ENDPOINT_CLIENTS + "/" + clientId + "/account/" + accountId;
    }
}
