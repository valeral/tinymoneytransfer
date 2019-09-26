package valerii.resources.transport;

import valerii.Error;

/**
 * Transport object holds the error occurred during operations
 *
 * @author vliutyi
 */
public class TError {

    private int code;
    private String msg;

    public TError() {
    }

    public TError(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public TError(Error error) {
        this.code = error.getCode();
        this.msg = error.getMsg();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
