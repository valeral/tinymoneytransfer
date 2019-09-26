package valerii.exception;

import valerii.Error;

/**
 * Class represents exceptions related to transfer operations on/between accounts
 */
public class TransferException extends Exception {

    private Error error;

    public TransferException(Error error) {
        this.error = error;
    }

    public Error getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return "ERR_" + error.getCode() + ": " + error.getMsg();
    }
}
