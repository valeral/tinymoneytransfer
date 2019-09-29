package valerii;

import org.junit.jupiter.api.Test;
import valerii.domain.Currency;
import valerii.resources.transport.TAccount;
import valerii.resources.transport.TError;
import valerii.resources.transport.TTransferData;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author vliutyi
 */
public class TransfersResourceIT extends ITTestBase {

    @Test
    public void transferToAnotherAccountOK() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.EUR.toString(), 100, Currency.EUR.toString(), 100);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(srcAccount.getId(), dstAccount.getId(), 30);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Wrong response code");

        assertEquals(70, getAccountInfo(srcAccount).getAmount(), "Wrong amount in src account");
        assertEquals(130, getAccountInfo(dstAccount).getAmount(), "Wrong amount in dst account");
    }

    @Test
    public void transferWithoutMandatoryDataReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.EUR.toString(), 200, Currency.EUR.toString(), 200);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(null, dstAccount.getId(), 50);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_015.getCode(), error.getCode(), "Wrong error code");

        response = postTransfer(srcAccount.getId(), null, 50);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Wrong response code");
        error = response.readEntity(TError.class);
        assertEquals(Error.ERR_016.getCode(), error.getCode(), "Wrong error code");

        response = postTransfer(srcAccount.getId(), dstAccount.getId(), null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Wrong response code");
        error = response.readEntity(TError.class);
        assertEquals(Error.ERR_017.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferWithNegativeAmountReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.EUR.toString(), 200, Currency.EUR.toString(), 200);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(srcAccount.getId(), dstAccount.getId(), -50);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Wrong response code");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_022.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferToFromNonExistentAccountsReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.EUR.toString(), 200, Currency.EUR.toString(), 200);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(666, dstAccount.getId(), 50);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_018.getCode(), error.getCode());

        response = postTransfer(srcAccount.getId(), 666, 50);

        assertEquals(422, response.getStatus(), "Wrong response code");
        error = response.readEntity(TError.class);
        assertEquals(Error.ERR_019.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferToSameAccountReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.EUR.toString(), 200, Currency.EUR.toString(), 200);
        TAccount srcAccount = accounts[0];

        Response response = postTransfer(srcAccount.getId(), srcAccount.getId(), 50);

        assertEquals(422, response.getStatus(), "Wrong response code");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_020.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferToAccountWithDifferentCurrencyReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.USD.toString(), 200, Currency.EUR.toString(), 200);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(srcAccount.getId(), dstAccount.getId(), 50);

        assertEquals(422, response.getStatus(), "Wrong response code");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_021.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferWithInsufficientAmountReturnError() {
        TAccount[] accounts = create2AccountsWithAmount(Currency.USD.toString(), 10, Currency.USD.toString(), 200);
        TAccount srcAccount = accounts[0];
        TAccount dstAccount = accounts[1];

        Response response = postTransfer(srcAccount.getId(), dstAccount.getId(), 50);

        assertEquals(422, response.getStatus(), "Wrong response code");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_014.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void transferInParallelOK() throws InterruptedException {
        TAccount[] accounts = create2AccountsWithAmount(Currency.USD.toString(), 500, Currency.USD.toString(), 500);
        TAccount account1 = accounts[0];
        TAccount account2 = accounts[1];

        ExecutorService service = Executors.newCachedThreadPool();
        // 20 threads each debited with 1 account1 and transfers 10 from account1 to account2: 20 debited, 200 withdrawn in total
        // another 20 threads withdraw 2 from account2 and transfer amount 5 from account2 to account1: 40 + 100 withdrawn in total
        for (int i = 0; i < 20; i++) {
            service.execute(() -> postDebitWithdrawAccount(account1, 1));
            service.execute(() -> postDebitWithdrawAccount(account2, -2));
            service.execute(() -> postTransfer(account1.getId(), account2.getId(), 10));
            service.execute(() -> postTransfer(account2.getId(), account1.getId(), 5));
        }

        // wait for all threads to complete
        service.shutdown();
        service.awaitTermination(5, TimeUnit.SECONDS);

        // checks result amount on account1: 500 + 20 - 200 + 100 = 420
        TAccount updatedAccount = getAccountInfo(account1);
        assertEquals(420, updatedAccount.getAmount(), "Wrong amount on account1 after parallel transfer");

        // checks result amount on account2: 500 - 40 + 200 - 100 = 640
        updatedAccount = getAccountInfo(account2);
        assertEquals(640, updatedAccount.getAmount(), "Wrong amount on account2 after parallel transfer");
    }

    private TAccount[] create2AccountsWithAmount(String srcCurrency, Integer srcAmount, String dstCurrency, Integer dstAccount) {
        TAccount[] accounts = new TAccount[2];
        accounts[0] = createAccountForNewClient(srcCurrency);
        postDebitWithdrawAccount(accounts[0], srcAmount);

        accounts[1] = createAccountForNewClient(dstCurrency);
        postDebitWithdrawAccount(accounts[1], dstAccount);

        return accounts;
    }

    private Response postTransfer(Integer srcAccountId, Integer dstAccountId, Integer amount) {
        Invocation.Builder builder = webTarget.get().path(ENDPOINT_TRANSFERS).request(MediaType.APPLICATION_JSON);
        return builder.put(Entity.json(new TTransferData(srcAccountId, dstAccountId, amount)));
    }
}
