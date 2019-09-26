package valerii.resources;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.Error;
import valerii.domain.Account;
import valerii.domain.Client;
import valerii.domain.Currency;
import valerii.resources.ResourceExecutor.Worker;
import valerii.resources.transport.TAccount;
import valerii.resources.transport.TClient;
import valerii.resources.transport.TDebitWithdraw;
import valerii.resources.transport.TError;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Optional;

import static valerii.resources.ClientAccountResource.RESOURCE_NAME;

/**
 * Endpoint for all operations related to a client and his account
 *
 * @author vliutyi
 */
//TODO use swagger?
@Path(RESOURCE_NAME)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientAccountResource {

    static final String RESOURCE_NAME = "clients";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientAccountResource.class);

    @GET
    @Path("{clientId}")
    @ManagedAsync
    public void clientInfo(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Client client = Client.getById(clientId);

            if (client == null) {
                TError error = new TError(Error.ERR_001);
                LOGGER.error(error.getMsg());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            }

            TClient tClient = new TClient(client, makeHref(client.getId()));
            return Response.ok().entity(tClient).build();
        }
        ));
    }

    @POST
    @ManagedAsync
    public void newClient(@Suspended final AsyncResponse asyncResponse, TClient clientData) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Optional<TError> validateError = validateClientData(clientData);

            if (validateError.isPresent()) {
                LOGGER.error(validateError.get().getMsg());
                return Response.status(Status.BAD_REQUEST).entity(validateError.get()).build();
            }

            Client client = Client.create(clientData.getName());

            if (client == null) {
                TError error = new TError(Error.ERR_004);
                LOGGER.error(error.getMsg());
                return Response.serverError().entity(error).build();
            }

            TClient newClient = new TClient(client, makeHref(client.getId()));
            return Response.status(Status.CREATED).entity(newClient).build();
        }
        ));
    }

    @DELETE
    @Path("{clientId}")
    @ManagedAsync
    public void deleteClient(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {
            //TODO implement
            return Response.status(Status.NOT_IMPLEMENTED).build();
        }
        ));
    }

    @GET
    @Path("{clientId}/account/{accountId}")
    @ManagedAsync
    public void accountInfo(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId, @PathParam("accountId") int accountId) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Client client = Client.getById(clientId);

            if (client == null) {
                TError error = new TError(Error.ERR_001);
                LOGGER.error(error.getMsg());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            }

            Account account = Account.getById(accountId);

            if (account == null) {
                TError error = new TError(Error.ERR_002);
                LOGGER.error(error.getMsg());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            }

            TAccount tAccount = new TAccount(account, makeHref(account.getClientId(), account.getId()));
            return Response.ok().entity(tAccount).build();
        }
        ));
    }

    @POST
    @Path("{clientId}/account")
    @ManagedAsync
    public void openAccount(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId, TAccount accountData) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Optional<TError> validateError = validateAccountData(accountData);

            if (validateError.isPresent()) {
                LOGGER.error(validateError.get().getMsg());
                return Response.status(Status.BAD_REQUEST).entity(validateError.get()).build();
            }

            Account account = Account.create(clientId, Currency.valueOf(accountData.getCurrency()));

            if (account == null) {
                TError error = new TError(Error.ERR_009);
                LOGGER.error(error.getMsg());
                return Response.serverError().entity(error).build();
            }

            TAccount tAccount = new TAccount(account, makeHref(clientId, account.getId()));
            return Response.status(Status.CREATED).entity(tAccount).build();
        }
        ));
    }

    @DELETE
    @Path("{clientId}/account/{accountId}")
    @ManagedAsync
    public void closeAccount(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId, @PathParam("accountId") int accountId) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {
            //TODO implement
            return Response.status(Status.NOT_IMPLEMENTED).build();
        }
        ));
    }

    @PUT
    @Path("{clientId}/account/{accountId}")
    @ManagedAsync
    public void debitWithdrawAccount(@Suspended final AsyncResponse asyncResponse, @PathParam("clientId") int clientId, @PathParam("accountId") int accountId, TDebitWithdraw debitWithdraw) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Optional<TError> validateError = validateDebitWithdraw(debitWithdraw);

            if (validateError.isPresent()) {
                LOGGER.error(validateError.get().getMsg());
                return Response.status(Status.BAD_REQUEST).entity(validateError.get()).build();
            }

            Account account = Account.getById(accountId);

            if (account == null) {
                TError error = new TError(Error.ERR_002);
                LOGGER.error(error.getMsg());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            }

            if (account.getClientId() != clientId) {
                TError error = new TError(Error.ERR_011);
                LOGGER.error(error.getMsg());
                return Response.status(Status.NOT_FOUND).entity(error).build();
            }

            if (!account.updateAmount(debitWithdraw.getAmountDiff())) {
                TError error = new TError(Error.ERR_013);
                LOGGER.error(error.getMsg());
                return Response.serverError().entity(error).build();
            }

            TAccount tAccount = new TAccount(account, makeHref(clientId, account.getId()));
            return Response.ok().entity(tAccount).build();
        }
        ));
    }


    private Optional<TError> validateAccountData(TAccount accountData) {
        if (accountData == null) {
            return Optional.of(new TError(Error.ERR_007));
        }

        if (accountData.getCurrency() == null) {
            return Optional.of(new TError(Error.ERR_007));
        }

        try {
            Currency.valueOf(accountData.getCurrency());
        } catch (IllegalArgumentException ex) {
            return Optional.of(new TError(Error.ERR_010));
        }

        return Optional.empty();
    }

    private Optional<TError> validateClientData(TClient clientData) {
        if (clientData == null) {
            return Optional.of(new TError(Error.ERR_006));
        }

        if (clientData.getName() == null) {
            return Optional.of(new TError(Error.ERR_006));
        }

        return Optional.empty();
    }

    private Optional<TError> validateDebitWithdraw(TDebitWithdraw debitWithdraw) {
        if (debitWithdraw == null || debitWithdraw.getAmountDiff() == null) {
            return Optional.of(new TError(Error.ERR_012));
        }

        return Optional.empty();
    }

    private String makeHref(int clientId) {
        return makeHref(clientId, null);
    }

    private String makeHref(int clientId, Integer accountId) {
        String href = "/" + RESOURCE_NAME + "/" + clientId;
        if (accountId != null) {
            href = href + "/account/" + accountId;
        }

        return href;
    }
}
