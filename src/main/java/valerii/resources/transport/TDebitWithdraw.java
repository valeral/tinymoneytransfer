package valerii.resources.transport;

/**
 * Transport object holds the data related to debit and withdraw from single account
 *
 * @author vliutyi
 */
public class TDebitWithdraw {

    private Integer amountDiff;

    public TDebitWithdraw() {
    }

    public TDebitWithdraw(Integer amountDiff) {
        this.amountDiff = amountDiff;
    }

    public Integer getAmountDiff() {
        return amountDiff;
    }

    public void setAmountDiff(Integer amountDiff) {
        this.amountDiff = amountDiff;
    }
}
