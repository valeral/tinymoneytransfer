package valerii.db;

/**
 * All supported DB table names
 */
public enum Table {
    CLIENT("client"),
    ACCOUNT("account");
    //CLIENT_ACCOUNT("client_account");

    private String tableName;

    Table(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}
