package valerii;

import org.junit.jupiter.api.Test;
import valerii.domain.Currency;
import valerii.resources.transport.TAccount;
import valerii.resources.transport.TClient;
import valerii.resources.transport.TError;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author vliutyi
 */
public class ClientsAccountResourceIT extends ITTestBase {

    @Test
    public void createAccountOK() {
        Response clientResponse = postNewClient(createUniqueName());
        TClient client = clientResponse.readEntity(TClient.class);
        Response accountResponse = postNewAccount(client.getId(), Currency.RUB.toString());

        assertEquals(Response.Status.CREATED.getStatusCode(), accountResponse.getStatus(), "Wrong response status");
        TAccount account = accountResponse.readEntity(TAccount.class);
        assertTrue(account.getId() > 0, "Wrong account id");
        assertEquals(client.getId(), account.getClientId(), "Wrong client id");
        assertEquals(Currency.RUB.toString(), account.getCurrency(), "Wrong currency");
        assertEquals(0, account.getAmount(), "Wrong amount");
    }

    @Test
    public void createAccountWithoutCurrencyReturnError() {
        Response clientResponse = postNewClient(createUniqueName());
        TClient client = clientResponse.readEntity(TClient.class);
        Response accountResponse = postNewAccount(client.getId(), null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), accountResponse.getStatus(), "Wrong response status");
        TError error = accountResponse.readEntity(TError.class);
        assertEquals(Error.ERR_007.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void createAccountWithWrongCurrencyReturnError() {
        Response clientResponse = postNewClient(createUniqueName());
        TClient client = clientResponse.readEntity(TClient.class);
        Response accountResponse = postNewAccount(client.getId(), "BTC");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), accountResponse.getStatus(), "Wrong response status");
        TError error = accountResponse.readEntity(TError.class);
        assertEquals(Error.ERR_010.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void createAccountForUnknownClientReturnError() {
        Response accountResponse = postNewAccount(666, Currency.EUR.toString());

        assertEquals(422, accountResponse.getStatus(), "Wrong response status");
        TError error = accountResponse.readEntity(TError.class);
        assertEquals(Error.ERR_001.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void createDuplicateAccountForClientReturnError() {
        TAccount createdAccount = createAccountForNewClient(Currency.USD.toString());
        Response accountResponse = postNewAccount(createdAccount.getClientId(), Currency.USD.toString());

        assertEquals(422, accountResponse.getStatus(), "Wrong response status");
        TError error = accountResponse.readEntity(TError.class);
        assertEquals(Error.ERR_008.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void accountInfoOK() {
        TAccount createdAccount = createAccountForNewClient(Currency.USD.toString());
        //use "href" from created instance
        Invocation.Builder builder = webTarget.get().path(createdAccount.getHref()).request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Wrong response status");
        TAccount account = response.readEntity(TAccount.class);
        assertEquals(createdAccount.getId(), account.getId(), "Wrong account id");
        assertEquals(createdAccount.getClientId(), account.getClientId(), "Wrong client id");
        assertEquals(createdAccount.getCurrency(), account.getCurrency(), "Wrong currency");
        assertEquals(createdAccount.getAmount(), account.getAmount(), "Wrong amount");
        assertEquals(createdAccount.getHref(), account.getHref(), "Wrong href");
    }

    @Test
    public void accountInfoForUnknownClientReturnError() {
        TAccount account = createAccountForNewClient(Currency.RUB.toString());
        Invocation.Builder builder = webTarget.get().path(createURLForClientAccount(666, account.getId())).request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_001.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void accountInfoForNonexistentAccountReturnError() {
        TAccount account = createAccountForNewClient(Currency.RUB.toString());
        Invocation.Builder builder = webTarget.get().path(createURLForClientAccount(account.getClientId(), 666)).request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_002.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void debitWithdrawAccountOK() {
        TAccount account = createAccountForNewClient(Currency.RUB.toString());
        Response response = postDebitWithdrawAccount(account, 100);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        TAccount updatedAccount = response.readEntity(TAccount.class);
        assertEquals(100, updatedAccount.getAmount(), "Wrong amount in updated account");
        assertEquals(account.getId(), updatedAccount.getId(), "Wrong id in updated account");
        assertEquals(account.getClientId(), updatedAccount.getClientId(), "Wrong client id in updated account");
        assertEquals(account.getCurrency(), updatedAccount.getCurrency(), "Wrong currency in updated account");
        assertEquals(account.getHref(), updatedAccount.getHref(), "Wrong href in updated account");

        response = postDebitWithdrawAccount(account, -50);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Wrong response status");
        updatedAccount = response.readEntity(TAccount.class);
        assertEquals(50, updatedAccount.getAmount(), "Wrong amount in updated account");
    }

    @Test
    public void debitWithdrawAccountWithWrongAmountReturnError() {
        TAccount account = createAccountForNewClient(Currency.USD.toString());
        Response response = postDebitWithdrawAccount(account, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_012.getCode(), error.getCode(), "Wrong error code");
    }
    
    @Test
    public void debitNonExistentAccountReturnError() {
        TAccount account = createAccountForNewClient(Currency.USD.toString());
        Response response = postDebitWithdrawAccount(account.getClientId(), 666, 100);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_002.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void debitAccountOfWrongClientReturnError() {
        TAccount account = createAccountForNewClient(Currency.USD.toString());
        Response response = postDebitWithdrawAccount(666, account.getId(), 100);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_011.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void withdrawAccountInsufficientAmountReturnError() {
        TAccount account = createAccountForNewClient(Currency.USD.toString());
        Response response = postDebitWithdrawAccount(account, -100);

        assertEquals(422, response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_014.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void debitWithdrawInParallelOK() throws InterruptedException {
        TAccount account = createAccountForNewClient(Currency.EUR.toString());
        // initial amount on account is 100
        postDebitWithdrawAccount(account, 100);

        ExecutorService service = Executors.newCachedThreadPool();
        // create 30 threads each increases amount by 10, 300 in total
        // another 30 threads decrease amount by 5, -150 in total
        for (int i = 0; i < 30; i++) {
            service.execute(() -> postDebitWithdrawAccount(account, 10));
            service.execute(() -> postDebitWithdrawAccount(account, -5));
        }

        // wait for all threads to complete
        service.shutdown();
        service.awaitTermination(5, TimeUnit.SECONDS);

        // checks result amount on account: 100 + 300 - 150 = 250
        TAccount updatedAccount = getAccountInfo(account);

        assertEquals(250, updatedAccount.getAmount(), account.getId(), "Wrong amount after parallel update");
    }
}