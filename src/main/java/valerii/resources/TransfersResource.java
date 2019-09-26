package valerii.resources;

import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.Error;
import valerii.domain.Account;
import valerii.resources.ResourceExecutor.Worker;
import valerii.resources.transport.TError;
import valerii.resources.transport.TTransferData;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Endpoint for all operations related to transfer between accounts
 *
 * @author vliutyi
 */
@Path("/transfers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TransfersResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransfersResource.class);

    @PUT
    @ManagedAsync
    public void transfer(@Suspended final AsyncResponse asyncResponse, TTransferData transferData) {
        ResourceExecutor.getExecutor().execute(new Worker(asyncResponse, () -> {

            Optional<TError> validateError = validateTransferData(transferData);

            if (validateError.isPresent()) {
                LOGGER.error(validateError.get().getMsg());
                return Response.status(Response.Status.BAD_REQUEST).entity(validateError.get()).build();
            }

            Account srcAccount = Account.getById(transferData.getSrcAccountId());

            if (srcAccount == null) {
                TError error = new TError(Error.ERR_018);
                LOGGER.error(error.getMsg());
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }

            srcAccount.transferTo(transferData.getDstAccountId(), transferData.getAmount());

            return Response.ok().build();
        }
        ));
    }

    private Optional<TError> validateTransferData(TTransferData transferData) {
        if (transferData == null) {
            return Optional.of(new TError(Error.ERR_015));
        }

        if (transferData.getSrcAccountId() == null) {
            return Optional.of(new TError(Error.ERR_015));
        }

        if (transferData.getDstAccountId() == null) {
            return Optional.of(new TError(Error.ERR_016));
        }

        if (transferData.getAmount() == null) {
            return Optional.of(new TError(Error.ERR_017));
        }

        if (transferData.getAmount() <= 0) {
            return Optional.of(new TError(Error.ERR_022));
        }

        return Optional.empty();
    }
}
