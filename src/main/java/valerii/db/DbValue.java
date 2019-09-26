package valerii.db;

/**
 * Structure to hold a value type and value itself. Used in db operations
 */
public class DbValue {

    private DbFieldType type;
    private Object value;

    public DbValue(DbFieldType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public DbFieldType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
