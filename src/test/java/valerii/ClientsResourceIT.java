package valerii;

import org.junit.jupiter.api.Test;
import valerii.resources.transport.TClient;
import valerii.resources.transport.TError;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author vliutyi
 */
public class ClientsResourceIT extends ITTestBase {

    @Test
    public void createClientOK() {
        String name = createUniqueName();
        Response response = postNewClient(name);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Wrong response status");

        TClient createdClient = response.readEntity(TClient.class);
        assertEquals(name, createdClient.getName(), "Wrong client name");
        assertNotNull(createdClient.getId(), "No client id");
        assertTrue(createdClient.getId() > 0, "Wrong client id");
    }

    @Test
    public void createClientWithNoNameReturnsError() {
        Response response = postNewClient(null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(), "Wrong response status");

        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_006.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void createDuplicateClientReturnError() {
        String name = createUniqueName();
        postNewClient(name);
        Response response = postNewClient(name);

        assertEquals(422, response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_005.getCode(), error.getCode(), "Wrong error code");
    }

    @Test
    public void clientInfoOK() {
        Response createResponse = postNewClient(createUniqueName());
        TClient createdClient = createResponse.readEntity(TClient.class);
        // request info by "href" provided by server
        Invocation.Builder builder = webTarget.path(createdClient.getHref()).request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Wrong response status");
        TClient client = response.readEntity(TClient.class);
        assertEquals(createdClient.getId(), client.getId(), "Wrong client id");
        assertEquals(createdClient.getName(), client.getName(), "Wrong client name");
        assertEquals(createdClient.getHref(), client.getHref(),"Wrong href");
    }

    @Test
    public void clientInfoNotFound() {
        Invocation.Builder builder = webTarget.path(ENDPOINT_CLIENTS + "/666").request(MediaType.APPLICATION_JSON);
        Response response = builder.get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus(), "Wrong response status");
        TError error = response.readEntity(TError.class);
        assertEquals(Error.ERR_001.getCode(), error.getCode(), "Wrong error code");
    }
}
