package valerii.resources.transport;

import valerii.domain.Client;

/**
 * Transport object holds the data related to client
 */
public class TClient {

    private Integer id;
    private String name;
    private String href;

    public TClient() {
    }

    public TClient(String name) {
        this.name = name;
    }

    public TClient(Integer id, String name, String href) {
        this.id = id;
        this.name = name;
        this.href = href;
    }

    public TClient(Client client, String href) {
        this.id = client.getId();
        this.name = client.getName();
        this.href = href;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
