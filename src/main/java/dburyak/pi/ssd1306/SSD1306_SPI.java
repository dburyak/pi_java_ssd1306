package dburyak.pi.ssd1306;


import static com.pi4j.io.gpio.PinState.HIGH;
import static com.pi4j.io.gpio.PinState.LOW;
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

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.spi.SpiDevice;


/**
 * Project : pi_java_ssd1306<p>
 * SSD1306 device connected over SPI.
 * <p><b>Created on:</b> <i>8:27:23 PM Mar 27, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
@ThreadSafe
public final class SSD1306_SPI implements SSD1306Connection {

    /**
     * Default system logger.
     * <p><b>Created on:</b> <i>8:28:22 PM Mar 27, 2017</i>
     */
    private static final Logger LOG = notNull(LogManager.getFormatterLogger(SSD1306_SPI.class));


    /**
     * Underlying Pi4J spi device.
     * <p><b>Created on:</b> <i>8:30:12 PM Mar 27, 2017</i>
     */
    @GuardedBy("lock")
    private final SpiDevice spi;

    /**
     * SPI "DC" pin.
     * <p><b>Created on:</b> <i>8:33:17 PM Mar 27, 2017</i>
     */
    @GuardedBy("lock")
    private final GpioPinDigitalOutput dcPin;

    /**
     * Lock for controlling access to underlying Pi4J SPI device.
     * <p><b>Created on:</b> <i>12:22:54 PM Mar 28, 2017</i>
     */
    private final Lock lock = new ReentrantLock();


    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.SSD1306SPIOutput.<p>
     * <p><b>PRE-conditions:</b> non-null {@code spi}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>8:30:28 PM Mar 27, 2017</i>
     * 
     * @param spi
     *            underlying Pi4J spi device
     * @param dcPin
     *            provisioned spi "DC" pin
     */
    private SSD1306_SPI(final SpiDevice spi, final GpioPinDigitalOutput dcPin) {
        this.spi = spi;
        this.dcPin = dcPin;
    }

    /**
     * Create new {@link SSD1306_SPI} instance with specified {@link SpiDevice} and SPI "DC" pin.
     * <p><b>PRE-conditions:</b> non-null {@code spi}, non-null {@code dcPin}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>10:42:09 PM Apr 18, 2017</i>
     * 
     * @param spi
     *            configured SPI device
     * @param dcPin
     *            provisioned SPI "DC" pin
     * @return new {@link SSD1306_SPI} instance
     */
    public static final SSD1306_SPI newInstance(final SpiDevice spi, final GpioPinDigitalOutput dcPin) {
        return new SSD1306_SPI(spi, dcPin);
    }

    /**
     * Write single byte to associated SPI device and handle exception.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>9:54:56 AM Mar 28, 2017</i>
     * 
     * @param singleByte
     *            single byte to be written
     */
    @SuppressWarnings("nls")
    private final void writeSafe(final byte singleByte) {
        try {
            spi.write(singleByte);
        } catch (final IOException e) {
            LOG.error("SPI device single byte write failed", e);
        }
    }

    /**
     * Write byte buffer to associated SPI device and handle exception.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>9:55:40 AM Mar 28, 2017</i>
     * 
     * @param buffer
     *            byte buffer to be written
     */
    @SuppressWarnings("nls")
    private final void writeSafe(final byte[] buffer) {
        try {
            spi.write(buffer);
        } catch (final IOException e) {
            LOG.error("SPI device buffer write failed", e);
        }
    }

    /**
     * Write command to this device.
     * <p><b>PRE-conditions:</b> non-null {@code cmd}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>8:27:23 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#command(dburyak.pi.ssd1306.Command)
     * @param cmd
     *            command to be written
     */
    @SuppressWarnings("nls")
    @Override
    public final void command(final Command cmd) {
        LOG.trace("sending command to SSD1306 SPI : spi = [%s] ; cmd = [%s]", spi, cmd);
        runSync(lock, () -> {
            dcPin.setState(LOW);
            writeSafe(cmd.code());
        });
    }

    /**
     * Write single byte in command mode to this device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>10:38:31 PM Mar 29, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#command(byte)
     * @param value
     *            byte value to be sent
     */
    @SuppressWarnings("nls")
    @Override
    public final void command(final byte value) {
        LOG.trace("sending value in command mode to SSD1306 SPI : spi = [%s] ; cmd = [%s]", spi, hex(value));
        runSync(lock, () -> {
            dcPin.setState(LOW);
            writeSafe(value);
        });
    }

    /**
     * Write single data byte to this device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>8:27:23 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#data(byte)
     * @param singleByte
     *            single byte to be written
     */
    @SuppressWarnings({ "nls" })
    @Override
    public final void data(final byte singleByte) {
        LOG.trace("sending single byte to SSD1306 SPI : spi = [%s] ; byte = [%d]", spi, hex(singleByte));
        runSync(lock, () -> {
            dcPin.setState(HIGH);
            writeSafe(singleByte);
        });
    }

    /**
     * Write data buffer to this device.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>8:27:23 PM Mar 27, 2017</i>
     * 
     * @see dburyak.pi.ssd1306.SSD1306Connection#data(byte[])
     * @param buffer
     *            data buffer to be written
     */
    @SuppressWarnings("nls")
    @Override
    public final void data(final byte[] buffer) {
        LOG.trace("sending buffer to SSD1306 SPI : spi = [%s] ; buffer = [%s]", spi, Arrays.toString(buffer));
        runSync(lock, () -> {
            dcPin.setState(HIGH);
            writeSafe(buffer);
        });
    }

    /**
     * Get string representation of this SPI communication device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-empty {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>8:11:35 PM Apr 23, 2017</i>
     * 
     * @see java.lang.Object#toString()
     * @return string representation of this object
     */
    @SuppressWarnings("nls")
    @Override
    public final String toString() {
        return notNull(new StringBuilder()
            .append("{spi=[").append(spi)
            .append("],dcPin=[").append(dcPin.getPin())
            .append("]}")
            .toString());
    }

}
