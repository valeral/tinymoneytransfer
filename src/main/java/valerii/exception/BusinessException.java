package valerii.exception;

import valerii.Error;

/**
 * Class represents exceptions related to common business logic
 */
public class BusinessException extends Exception {

    private Error error;

    public BusinessException(Error error) {
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
