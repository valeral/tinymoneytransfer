package valerii.mutex;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author vliutyi
 */

@Deprecated
public class BiIntegerMutex {

    private final Map<BiMutex, WeakReference<BiMutex>> mutexMap = new WeakHashMap<>();

    /**
     * Get a mutex object for the given (non-null) 2 ids.
     */
    public BiMutex getMutex(Integer id1, Integer id2) {
        if (id1 == null || id2 == null) {
            throw new NullPointerException();
        }

        BiMutex key = new BiMutexImpl(id1, id2);
        synchronized (mutexMap) {
            return mutexMap.computeIfAbsent(key, k -> new WeakReference<>(key)).get();
        }
    }

    private interface BiMutex {
    }

    private static class BiMutexImpl implements BiMutex {
        private final Integer id1;
        private final Integer id2;

        BiMutexImpl(Integer id1, Integer id2) {
            this.id1 = id1;
            this.id2 = id2;
        }

        private Integer getId1() {
            return id1;
        }

        private Integer getId2() {
            return id2;
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this.getClass() == o.getClass()) {
                BiMutexImpl otherMutex = ((BiMutexImpl)o);
                return this.getId1().equals(otherMutex.getId1()) && this.getId2().equals(otherMutex.getId2())
                        || this.getId1().equals(otherMutex.getId2()) && this.getId2().equals(otherMutex.getId1());
            }
            return false;
        }

        public int hashCode() {
            return id1.hashCode() + id2.hashCode();
        }
    }
}
