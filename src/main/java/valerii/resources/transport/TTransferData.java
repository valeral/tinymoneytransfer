package valerii.resources.transport;

/**
 * Transport object holds the data related to transfer operations between 2 accounts
 *
 * @author vliutyi
 */
public class TTransferData {

    private Integer srcAccountId;
    private Integer dstAccountId;
    private Integer amount;

    public TTransferData() {
    }

    public TTransferData(Integer srcAccountId, Integer dstAccountId, Integer amount) {
        this.srcAccountId = srcAccountId;
        this.dstAccountId = dstAccountId;
        this.amount = amount;
    }

    public Integer getSrcAccountId() {
        return srcAccountId;
    }

    public void setSrcAccountId(Integer srcAccountId) {
        this.srcAccountId = srcAccountId;
    }

    public Integer getDstAccountId() {
        return dstAccountId;
    }

    public void setDstAccountId(Integer dstAccountId) {
        this.dstAccountId = dstAccountId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
