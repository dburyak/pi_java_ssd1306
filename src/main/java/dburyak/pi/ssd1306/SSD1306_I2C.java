package dburyak.pi.ssd1306;


import static dburyak.pi.ssd1306.Util.asByte;
import static dburyak.pi.ssd1306.Util.hex;
import static dburyak.pi.ssd1306.Util.notNull;
import static dburyak.pi.ssd1306.Util.runSync;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.pi4j.wiringpi.I2C;


/**
 * Project : pi_java_ssd1306<p>
 * SSD1306 device output connected over I2C.
 * <p><b>Created on:</b> <i>7:26:01 PM Mar 27, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
@ThreadSafe
public final class SSD1306_I2C implements SSD1306Connection {

    /**
     * Default system logger.
     * <p><b>Created on:</b> <i>7:29:14 PM Mar 27, 2017</i>
     */
    private static final Logger LOG = notNull(LogManager.getFormatterLogger(SSD1306_I2C.class));

    /**
     * First "address" byte for sending commands.
     * <p><b>Created on:</b> <i>7:35:12 PM Mar 27, 2017</i>
     */
    private static final byte ADDR_COMMAND = 0x00;

    /**
     * First "data" byte for sending data.
     * <p><b>Created on:</b> <i>7:36:16 PM Mar 27, 2017</i>
     */
    private static final byte ADDR_DATA = 0x40;

    /**
     * Default I2C bus.
     * <p><b>Created on:</b> <i>9:38:34 PM Apr 18, 2017</i>
     */
    private static final int I2C_BUS_DFLT = 1;

    /**
     * Default I2C address.
     * <p><b>Created on:</b> <i>9:38:43 PM Apr 18, 2017</i>
     */
    private static final int I2C_ADDR_DFLT = 0x3C;


    /**
     * Underlying P4J I2C device to write bytes to.
     * <p><b>Created on:</b> <i>7:37:08 PM Mar 27, 2017</i>
     */
    @GuardedBy("lock")
    private final I2CDevice i2c;

    /**
     * Buffer of two bytes for sending commands to I2C device. First byte is for command "register", second one is for
     * command code.
     * <p><b>Created on:</b> <i>7:59:16 PM Mar 27, 2017</i>
     */
    @GuardedBy("lock")
    private final byte[] cmdBytes = new byte[2];

    /**
     * Buffer of two bytes for sending single data byte to I2C device. First byte is data "register", second one is for
     * data byte itself.
     * <p><b>Created on:</b> <i>8:08:00 PM Mar 27, 2017</i>
     */
    @GuardedBy("lock")
    private final byte[] dataBytes = new byte[2];

    /**
     * Lock for underlying I2C device access synchronization.
     * <p><b>Created on:</b> <i>12:18:32 PM Mar 28, 2017</i>
     */
    private final Lock lock = new ReentrantLock();

    /**
     * I2C bus of this connection.
     * <p><b>Created on:</b> <i>8:01:52 PM Apr 23, 2017</i>
     */
    private final int bus;

    /**
     * I2C address of this connection.
     * <p><b>Created on:</b> <i>8:02:03 PM Apr 23, 2017</i>
     */
    private final int addr;


    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.SSD1306I2COutput.<p>
     * <p><b>PRE-conditions:</b> non-null {@code i2c}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>7:26:01 PM Mar 27, 2017</i>
     * 
     * @param i2c
     *            underlying Pi4J I2C device
     * @param bus
     *            I2C bus of this connection
     * @param addr
     *            I2C address of this connection
     */
    private SSD1306_I2C(final I2CDevice i2c, final int bus, final int addr) {
        this.i2c = i2c;
        this.bus = bus;
        this.addr = addr;
        cmdBytes[0] = ADDR_COMMAND;
        dataBytes[0] = ADDR_DATA;
    }

    /**
     * Create new {@link SSD1306_I2C} instance with <em>default</em> I2C bus and <em>default</em> I2C address.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O I2C configuration calls
     * <br><b>Created on:</b> <i>9:39:26 PM Apr 18, 2017</i>
     * 
     * @return new {@link SSD1306_I2C} instance
     * @throws IOException
     *             if I/O exception occurred when accessing default I2C bus and default I2C address
     * @throws UnsupportedBusNumberException
     *             if default I2C bus is not supported
     */
    public static final SSD1306_I2C newInstance() throws IOException, UnsupportedBusNumberException {
        return newInstance(I2C_BUS_DFLT, I2C_ADDR_DFLT);
    }

    /**
     * Create new {@link SSD1306_I2C} instance with specified I2C bus <em>default</em> I2C address.
     * <p><b>PRE-conditions:</b> valid I2C {@code bus}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O I2C configuration calls
     * <br><b>Created on:</b> <i>11:02:12 PM Apr 18, 2017</i>
     * 
     * @param bus
     *            I2C bus
     * @return new {@link SSD1306_I2C} instance
     * @throws IOException
     *             if I/O exception occurred when accessing specified I2C {@code bus} and default I2C address
     * @throws UnsupportedBusNumberException
     *             if illegal I2C {@code bus} specified
     */
    public static final SSD1306_I2C newInstance(final int bus) throws IOException, UnsupportedBusNumberException {
        return newInstance(bus, I2C_ADDR_DFLT);
    }

    /**
     * Create new {@link SSD1306_I2C} instance with specified I2C bus and I2C address.
     * <p><b>PRE-conditions:</b> valid I2C {@code bus}, valid I2C {@code addr}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O I2C configuration calls
     * <br><b>Created on:</b> <i>9:15:09 PM Apr 18, 2017</i>
     * 
     * @param bus
     *            I2C bus
     * @param addr
     *            I2C address
     * @return new {@link SSD1306_I2C} instance
     * @throws IOException
     *             if I/O exception occurred when accessing specified I2C {@code bus} and I2C {@code addr}
     * @throws UnsupportedBusNumberException
     *             if illegal I2C {@code bus} specified
     */
    @SuppressWarnings({ "nls", "boxing" })
    public static final SSD1306_I2C newInstance(final int bus, final int addr)
        throws IOException, UnsupportedBusNumberException {

        try {
            final I2CDevice i2c = notNull(I2CFactory.getInstance(bus).getDevice(addr));
            return newInstance(i2c, bus, addr);
        } catch (final IOException e) {
            LOG.error("IO exception when accessing I2C : bus = [%d] ; addr = [%d]", bus, addr, e);
            throw e;
        } catch (final UnsupportedBusNumberException e) {
            LOG.error("wrong I2C bus number : bus = [%d] ; addr = [%d]", bus, addr, e);
            throw e;
        }
    }

    /**
     * Create new {@link SSD1306_I2C} instance with specified {@link I2C} device.
     * <p><b>PRE-conditions:</b> non-null {@code i2c}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>10:28:32 PM Apr 18, 2017</i>
     * 
     * @param i2c
     *            I2C device to communicate over
     * @param bus
     *            I2C bus of the connection
     * @param addr
     *            I2C address of the connection
     * @return new {@link SSD1306_I2C} instance
     */
    public static final SSD1306_I2C newInstance(final I2CDevice i2c, final int bus, final int addr) {
        return new SSD1306_I2C(i2c, bus, addr);
    }

    /**
     * Write buffer to underlying Pi4J device and handle exceptions.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>8:10:58 PM Mar 27, 2017</i>
     * 
     * @param buffer
     *            bytes to be written
     */
    @SuppressWarnings("nls")
    private final void writeBufferSafe(final byte[] buffer) {
        try {
            i2c.write(buffer);
        } catch (final IOException e) {
            LOG.error("I2C device write failed", e);
        }
    }

    /**
     * Write command to this SSD1306 device.
     * <p><b>PRE-conditions:</b> non-null {@code cmd}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>7:26:01 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#command(Command)
     * @param cmd
     *            command to be written
     */
    @SuppressWarnings({ "nls", "boxing" })
    @Override
    public final void command(final Command cmd) {
        LOG.trace("sending command to SSD1306 I2C : i2c = [%d:%s] ; cmd = [%s]", bus, hex(asByte(addr)), cmd);
        runSync(lock, () -> {
            cmdBytes[1] = cmd.code();
            writeBufferSafe(cmdBytes);
        });
    }

    /**
     * Write single byte in command mode.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>10:34:25 PM Mar 29, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#command(byte)
     * @param value
     *            single byte to be written
     */
    @SuppressWarnings({ "nls", "boxing" })
    @Override
    public final void command(final byte value) {
        LOG.trace("sending value in command mode to SSD1306 I2C : i2c = [%d:%s] ; value = [%s]",
            bus, hex(asByte(addr)), hex(value));
        runSync(lock, () -> {
            cmdBytes[1] = value;
            writeBufferSafe(cmdBytes);
        });
    }

    /**
     * Write single byte of data to this SSD1306 device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>7:26:01 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#data(byte)
     * @param singleByte
     *            data byte to be written
     */
    @SuppressWarnings({ "boxing", "nls" })
    @Override
    public final void data(final byte singleByte) {
        LOG.trace("sending single byte to SSD1306 I2C : i2c = [%d:%s] ; byte = [%s]",
            bus, hex(asByte(addr)), hex(singleByte));
        runSync(lock, () -> {
            dataBytes[1] = singleByte;
            writeBufferSafe(dataBytes);
        });
    }

    /**
     * Write data buffer to this SSD1306 device.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>7:26:01 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#data(byte[])
     * @param buffer
     *            data buffer to be written
     */
    @SuppressWarnings({ "nls", "boxing" })
    @Override
    public final void data(final byte[] buffer) {
        LOG.trace("sending buffer to SSD1306 I2C : i2c = [%d:%s] ; buffer = [%s]",
            bus, hex(asByte(addr)), Arrays.toString(buffer));

        final byte[] addressed = new byte[buffer.length + 1]; // first extra byte for address
        addressed[0] = ADDR_DATA;
        System.arraycopy(buffer, 0, addressed, 1, buffer.length); // remaining bytes are payload
        runSync(lock, () -> {
            writeBufferSafe(addressed);
        });
    }

    /**
     * Get string representation of this I2C hardware connection.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-empty {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>8:05:35 PM Apr 23, 2017</i>
     * 
     * @see java.lang.Object#toString()
     * @return string representation of this object
     */
    @SuppressWarnings("nls")
    @Override
    public final String toString() {
        return notNull(new StringBuilder()
            .append("{bus=[").append(bus)
            .append("],addr=[").append(addr)
            .append("]}")
            .toString());
    }

}
