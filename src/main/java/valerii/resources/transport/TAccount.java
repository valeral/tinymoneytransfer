package valerii.resources.transport;

import valerii.domain.Account;

/**
 * Transport object holds the data related to client account
 */
public class TAccount {

    private Integer id;
    private Integer clientId;
    private String currency;
    private Integer amount;
    private String href;

    public TAccount() {
    }

    public TAccount(Integer clientId, String currency) {
        this.clientId = clientId;
        this.currency = currency;
    }

    public TAccount(Account account, String href) {
        this.id = account.getId();
        this.clientId = account.getClientId();
        this.currency = account.getCurrency().toString();
        this.amount = account.getAmount();
        this.href = href;
    }

    public TAccount(Integer id, Integer clientId, String currency, Integer amount, String href) {
        this.id = id;
        this.clientId = clientId;
        this.currency = currency;
        this.amount = amount;
        this.href = href;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
