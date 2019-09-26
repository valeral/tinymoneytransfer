package valerii;

/**
 * List of all error codes used in business logic
 *
 * @author vliutyi
 */
public enum Error {

    ERR_001(1, "Client not found"),
    ERR_002(2, "Account not found"),
    ERR_003(3, "Internal SQL error"),
    ERR_004(4, "Client cannot be created"),
    ERR_005(5, "Client already exists"),
    ERR_006(6, "Client name must be provided"),
    ERR_007(7, "Currency must be provided to create an account"),
    ERR_008(8, "Client already has an account"),
    ERR_009(9, "Account cannot be created"),
    ERR_010(10, "Provided currency for account not supported"),
    ERR_011(11, "Client is not the owner of this account"),
    ERR_012(12, "Amount must be provided to update an account"),
    ERR_013(13, "Account cannot be updated"),
    ERR_014(14, "Insufficient amount to withdraw from account"),
    ERR_015(15, "Source account must be provided"),
    ERR_016(16, "Destination account must be provided"),
    ERR_017(17, "Amount to transfer must be provided"),
    ERR_018(18, "Source account not found"),
    ERR_019(19, "Destination account not found"),
    ERR_020(20, "Transfer must be done between different accounts"),
    ERR_021(21, "Currencies in accounts do not match"),
    ERR_022(22, "Amount to transfer must be a positive number"),
    ERR_023(23, "Error occurred while updating destination account"),
    ERR_024(24, "Error occurred while updating source account"),
    ERR_025(25, "Unexpected server error"),
    ;

    private int code;
    private String msg;

    Error(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
