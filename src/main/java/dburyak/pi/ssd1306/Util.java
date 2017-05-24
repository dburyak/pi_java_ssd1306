package dburyak.pi.ssd1306;


import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Project : pi_java_ssd1306<p>
 * <p><b>Created on:</b> <i>7:29:43 PM Mar 27, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
final class Util {

    /**
     * Default system logger.
     * <p><b>Created on:</b> <i>9:46:52 PM Mar 29, 2017</i>
     */
    private static final Logger LOG = notNull(LogManager.getFormatterLogger(Util.class));


    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.Util.<p>
     * Should never be called.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>7:30:13 PM Mar 27, 2017</i>
     */
    @SuppressWarnings("nls")
    private Util() {
        throw new AssertionError("not supposed to be called");
    }

    /**
     * Validate that object is not null.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>6:13:55 PM Mar 27, 2017</i>
     * 
     * @param <T>
     *            type of the object to be validated
     * @param argName
     *            name of the variable in calling code
     * @param obj
     *            object to be validated
     * @return original object
     * @throws IllegalArgumentException
     *             if {@code obj} is null
     */
    @SuppressWarnings("nls")
    static final <T> T notNull(final String argName, final @Nullable T obj) {
        if (obj == null) {
            throw new IllegalArgumentException(argName + " is null");
        }
        return obj;
    }

    /**
     * Validate that object is not null.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:13:43 AM Mar 28, 2017</i>
     * 
     * @param <T>
     *            type of the object to be validated
     * @param obj
     *            object to be validated
     * @return original object
     * @throws IllegalArgumentException
     *             if {@code obj} is null
     */
    @SuppressWarnings("nls")
    static final <T> T notNull(final @Nullable T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object is null");
        }
        return obj;
    }

    /**
     * Validate that logical statement is true.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>1:15:15 AM Apr 5, 2017</i>
     * 
     * @param predicate
     *            logical statement to be validated
     * @return true if {@code predicate} is true
     * @throws IllegalArgumentException
     *             if {@code predicate} is false
     */
    @SuppressWarnings("nls")
    static final boolean isTrue(final boolean predicate) {
        if (!predicate) {
            throw new IllegalArgumentException("condition not met");
        }
        return predicate;
    }

    /**
     * Run task synchronously on current thread using provided {@code lock} for synchronization.
     * <p><b>PRE-conditions:</b> non-null {@code lock}, non-null {@code task}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> task is run on current thread
     * <br><b>Created on:</b> <i>12:34:07 PM Mar 28, 2017</i>
     * 
     * @param lock
     *            lock to be used for synchronization
     * @param task
     *            task to run
     */
    static final void runSync(final Lock lock, final Runnable task) {
        try {
            lock.lock();
            task.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Call value supplier function synchronously on the current thread. Provided {@code lock} is used for
     * synchronization.
     * <p><b>PRE-conditions:</b> non-null {@code lock}, non-null {@code supplier}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> supplier is called on the current thread
     * <br><b>Created on:</b> <i>10:47:43 PM Apr 3, 2017</i>
     * 
     * @param <R>
     *            type of the {@code supplier} result
     * @param lock
     *            lock for synchronization
     * @param supplier
     *            result supplier to be called
     * @return result produced by {@code supplier}
     */
    static final @Nullable <R> R callSync(final Lock lock, final Supplier<R> supplier) {
        try {
            lock.lock();
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Convert unsigned int to byte. Note that resulting byte may be signed due to specifics of Java primitives system.
     * <p><b>PRE-conditions:</b> 0 &lt;= {@code intValue} &lt;= 255
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>6:53:52 AM Mar 26, 2017</i>
     * 
     * @param intValue
     *            int value to be converted
     * @return converted byte value
     */
    @SuppressWarnings({ "boxing", "nls" })
    static final byte asByte(final int intValue) {
        if (intValue < 0 || 255 < intValue) {
            LOG.error("converting int that exceeds byte range : intValue = [%d]", intValue);
        }
        return (byte) intValue;
    }

    /**
     * Convert signed byte to unsigned int representation.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> 0 &lt;= {@code result} &lt;= 255
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>7:12:05 AM Mar 26, 2017</i>
     * 
     * @param byteValue
     *            byte value to be converted
     * @return converted int value
     */
    static final int asUnsigned(final byte byteValue) {
        // 0xFF mask removes most significant bits, effectively represents signed byte as unsigned int
        return 0xFF & byteValue;
    }

    /**
     * Get unsigned hex string of byte value.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-empty {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>7:32:27 AM Mar 26, 2017</i>
     * 
     * @param byteValue
     *            byte value to get hex of
     * @return string with byte converted to hexadecimal
     */
    @SuppressWarnings({ "boxing", "nls" })
    static final String hex(final byte byteValue) {
        return "0x" + String.format("%02x", asUnsigned(byteValue)).toUpperCase();
    }


}
