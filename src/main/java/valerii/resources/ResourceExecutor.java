package valerii.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valerii.Error;
import valerii.db.DbProvider;
import valerii.exception.BusinessException;
import valerii.exception.TransferException;
import valerii.resources.transport.TError;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class provides thread-pool executor and holds the logic (worker) to call operation in multi-thread environment
 *
 * @author vliutyi
 */
class ResourceExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceExecutor.class);

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    static ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Worker that runs in specific thread and call operation logic within DB context.
     * Operation logic is called inside single db transaction. Transaction is committed after successful operation.
     * Transaction is aborted if operation raises an exception.
     */
    public static class Worker implements Runnable {

        AsyncResponse asyncResponse;
        Callable<Response> method;

        Worker(AsyncResponse asyncResponse, Callable<Response> method) {
            this.asyncResponse = asyncResponse;
            this.method = method;
        }

        @Override
        public void run() {
            Response response;

            // get new DB connection and bind it to current thread that executes operation
            try(Connection connection = DbProvider.newDBConnection()) {
                connection.setAutoCommit(false);
                DbProvider.setThreadConnection(connection);
                // actual invocation of operation
                response = method.call();
                // commit DB state after successful operation
                connection.commit();
            } catch (SQLException e) {
                LOGGER.error(e.getMessage());
                response = Response.serverError().entity(new TError(Error.ERR_003)).build();
            } catch (TransferException e) {
                LOGGER.error(e.getMessage());
                response = Response.status(422, "Transfer failed").entity(new TError(e.getError())).build();
            } catch (BusinessException e) {
                LOGGER.error(e.getMessage());
                response = Response.status(422, "Business constraints violation").entity(new TError(e.getError())).build();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                response = Response.serverError().entity(new TError(Error.ERR_025)).build();
            }

            asyncResponse.resume(response);
        }
    }
}
