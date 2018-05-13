package dburyak.pi.ssd1306;


import static dburyak.pi.ssd1306.Command.CHARGE_PUMP_DISABLE;
import static dburyak.pi.ssd1306.Command.CHARGE_PUMP_ENABLE;
import static dburyak.pi.ssd1306.Command.DISPLAY_OFF;
import static dburyak.pi.ssd1306.Command.DISPLAY_ON;
import static dburyak.pi.ssd1306.Command.DISPLAY_ON_RESUME;
import static dburyak.pi.ssd1306.Command.MEMORY_ADDRESSING_MODE_HORIZONTAL;
import static dburyak.pi.ssd1306.Command.SCROLL_ACTIVATE;
import static dburyak.pi.ssd1306.Command.SCROLL_DEACTIVATE;
import static dburyak.pi.ssd1306.Command.SCROLL_DIAGONAL_LEFT;
import static dburyak.pi.ssd1306.Command.SCROLL_DIAGONAL_RIGHT;
import static dburyak.pi.ssd1306.Command.SCROLL_HORIZONTAL_LEFT;
import static dburyak.pi.ssd1306.Command.SCROLL_HORIZONTAL_RIGHT;
import static dburyak.pi.ssd1306.Command.SET_CHARGE_PUMP;
import static dburyak.pi.ssd1306.Command.SET_CLOCK_DIV;
import static dburyak.pi.ssd1306.Command.SET_COLUMN_ADDR;
import static dburyak.pi.ssd1306.Command.SET_COM_OUTPUT_SCAN_DIRECTION_REMAPPED;
import static dburyak.pi.ssd1306.Command.SET_COM_PINS_CONFIGURATION;
import static dburyak.pi.ssd1306.Command.SET_CONTRAST;
import static dburyak.pi.ssd1306.Command.SET_DISPLAY_OFFSET;
import static dburyak.pi.ssd1306.Command.SET_DISPLAY_START_LINE_0;
import static dburyak.pi.ssd1306.Command.SET_INVERSE_DISPLAY;
import static dburyak.pi.ssd1306.Command.SET_MEMORY_ADDRESSING_MODE;
import static dburyak.pi.ssd1306.Command.SET_MULTIPLEX;
import static dburyak.pi.ssd1306.Command.SET_NORMAL_DISPLAY;
import static dburyak.pi.ssd1306.Command.SET_PRECHARGE_PERIOD;
import static dburyak.pi.ssd1306.Command.SET_SEGMENT_REMAP_127;
import static dburyak.pi.ssd1306.Command.SET_VCOMH_DESELECT_LEVEL;
import static dburyak.pi.ssd1306.Command.SET_VERTICAL_SCROLL_AREA;
import static dburyak.pi.ssd1306.Display.Dimensions.W128_H32;
import static dburyak.pi.ssd1306.Display.Dimensions.W128_H64;
import static dburyak.pi.ssd1306.Display.Dimensions.W96_H16;
import static dburyak.pi.ssd1306.Display.HorizontalDirection.LEFT;
import static dburyak.pi.ssd1306.Display.OverlayType.FULL;
import static dburyak.pi.ssd1306.Display.OverlayType.NONE;
import static dburyak.pi.ssd1306.Display.OverlayType.OFF_PIXELS;
import static dburyak.pi.ssd1306.Display.OverlayType.ON_PIXELS;
import static dburyak.pi.ssd1306.Display.ScrollFrequency.FRAMES_5;
import static dburyak.pi.ssd1306.Display.VerticalDirection.UP;
import static dburyak.pi.ssd1306.Util.asByte;
import static dburyak.pi.ssd1306.Util.hex;
import static dburyak.pi.ssd1306.Util.isTrue;
import static dburyak.pi.ssd1306.Util.notNull;
import static dburyak.pi.ssd1306.Util.runSync;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.AffineTransformOp.TYPE_BILINEAR;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pi4j.io.gpio.GpioPinDigitalOutput;


/**
 * Project : pi_java_ssd1306<p>
 * SSD1306 OLED display.
 * <p>Based on <a href="https://github.com/adafruit/Adafruit_Python_SSD1306</a>, <a
 * href="https://github.com/ondryaso/pi-ssd1306-java">pi-ssd1306-java</a> and <a
 * href="https://github.com/tuantvu/tuanpi">tuanpi</a>.
 * <p>Is partially thread safe. Since underlying {@link BufferedImage} and {@link Graphics2D} are not thread safe by
 * design, great effort should be put to provide thread safe API for drawing on display and considered as overkill here.
 * However, all methods that access underlying GPIO <em>are</em> thread safe and use separate internal lock. So it's OK
 * to use this object as a lock for client code, particularly for graphics drawing synchronization.
 * <p>To avoid visibility issues, {@link #sync()} should be called on the same thread where graphics drawing is
 * performed without interference with other drawing threads. Otherwise OLED display may result in unpredictable (stale,
 * partial, mixed) screen state.
 * <p><b>Created on:</b> <i>8:34:44 PM Mar 25, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
@NotThreadSafe
public class Display {

    /**
     * Default system logger.
     * <p><b>Created on:</b> <i>4:47:14 AM Mar 14, 2017</i>
     */
    private static final Logger LOG = notNull(LogManager.getFormatterLogger(Display.class));

    /**
     * Threshold for converting RGB pixel to monochrome one. If pixel's sum of REG + GREEN + BLUE exceeds this threshold
     * then pixel is converted to WHITE. Otherwise, it is considered as BLACK.
     * <p>TODO : this needs to be fine tuned or even made available for configuration
     * <p><b>Created on:</b> <i>5:40:10 PM Apr 7, 2017</i>
     */
    private static final int MONOCHROME_THRESHOLD = 64 + 64 + 64;

    /**
     * Default font for drawing text.
     * <p><b>Created on:</b> <i>1:07:38 AM Apr 9, 2017</i>
     */
    @SuppressWarnings("nls")
    private static final Font FONT_DEFAULT = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Default scrolling frequency.
     * <p><b>Created on:</b> <i>5:52:42 PM Apr 21, 2017</i>
     */
    private static final ScrollFrequency SCROLL_FREQ_DEFAULT = FRAMES_5;


    /**
     * Project : pi_java_ssd1306<p>
     * Available display dimensions.
     * <p><b>Created on:</b> <i>7:50:25 AM Mar 26, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static enum Dimensions {
            /**
             * 128x64 display.
             * <p><b>Created on:</b> <i>7:50:45 AM Mar 26, 2017</i>
             */
            W128_H64(128, 64),

            /**
             * 128x32 display.
             * <p><b>Created on:</b> <i>7:50:57 AM Mar 26, 2017</i>
             */
            W128_H32(128, 32),

            /**
             * 96x16 display.
             * <p><b>Created on:</b> <i>7:51:09 AM Mar 26, 2017</i>
             */
            W96_H16(96, 16);

        /**
         * Display width.
         * <p><b>Created on:</b> <i>7:51:22 AM Mar 26, 2017</i>
         */
        private final int width;

        /**
         * Display height.
         * <p><b>Created on:</b> <i>7:51:30 AM Mar 26, 2017</i>
         */
        private final int height;


        /**
         * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.Dimension.<p>
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>7:51:40 AM Mar 26, 2017</i>
         * 
         * @param width
         *            display width
         * @param height
         *            display height
         */
        private Dimensions(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Get width value.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> {@code result} == 128 || {@code result} == 96
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>7:51:59 AM Mar 26, 2017</i>
         * 
         * @return display width
         */
        public final int width() {
            return width;
        }

        /**
         * Get height value.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> {@code result} == 64 || {@code result} == 32 || {@code result} == 16
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>7:52:52 AM Mar 26, 2017</i>
         * 
         * @return height value
         */
        public final int height() {
            return height;
        }
    }

    /**
     * Project : pi_java_ssd1306<p>
     * Display pixel state.
     * <p><b>Created on:</b> <i>4:43:26 PM Mar 27, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    private static enum PixelState {
            /**
             * Lit ON.
             * <p><b>Created on:</b> <i>4:43:55 PM Mar 27, 2017</i>
             */
            ON,

            /**
             * Lit OFF.
             * <p><b>Created on:</b> <i>4:44:09 PM Mar 27, 2017</i>
             */
            OFF;
    }


    /**
     * Indicates whether device has external VCC, or VCC is connected via "charge pump" capacitors to VSS.
     * <p><b>Created on:</b> <i>6:33:10 AM Mar 26, 2017</i>
     */
    private final boolean hasExternalVCC;

    /**
     * Internal image to hold raster and associated {@link Graphics2D} object.
     * <p><b>Created on:</b> <i>5:20:11 AM Mar 26, 2017</i>
     */
    private final BufferedImage img;

    /**
     * Graphics to perform drawing.
     * <p><b>Created on:</b> <i>5:20:27 AM Mar 26, 2017</i>
     */
    private final Graphics2D graphics;

    /**
     * Display dimensions.
     * <p><b>Created on:</b> <i>5:25:42 AM Mar 26, 2017</i>
     */
    private final Dimensions dimensions;

    /**
     * Screen state is represented as byte buffer, each byte represents bit octet, each bit corresponds to pixel on the
     * screen: 0 - pixel is off, 1 - pixel is lit on. This is the number of bit octets, or number of bytes in the screen
     * buffer in other words.
     * <p><b>Created on:</b> <i>5:27:40 AM Mar 26, 2017</i>
     */
    private final int pages;

    /**
     * Hardware reset pin. Is null if device has no such dedicated pin.
     * <p><b>Created on:</b> <i>4:35:57 PM Mar 27, 2017</i>
     */
    @GuardedBy("lockGpio")
    private final @Nullable GpioPinDigitalOutput rstPin;

    /**
     * Output hardware device to write commands and data to. Is thread safe, but is guarded by extra {@code gpioLock} to
     * not interfere communication command sequences.
     * <p><b>Created on:</b> <i>10:09:17 AM Mar 28, 2017</i>
     */
    @GuardedBy("lockGpio")
    private final SSD1306Connection hwConn;

    /**
     * Buffer for holding display pixel matrix state. Each byte is a bit octet and represents 8 pixels column (page) on
     * the display matrix.
     * <p><b>Created on:</b> <i>4:39:11 PM Mar 27, 2017</i>
     */
    private final byte[] dispBuffer;

    /**
     * Lock for access synchronization to gpio-related resources.
     * <p><b>Created on:</b> <i>10:36:40 PM Apr 3, 2017</i>
     */
    private final Lock lockGpio = new ReentrantLock();

    /**
     * Stored contrast value. Used to save previous contrast when dimming and un-dimming display.
     * <p><b>Created on:</b> <i>12:29:06 AM Apr 4, 2017</i>
     */
    private final AtomicInteger contrast = new AtomicInteger(0);


    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.OLEDDisplay.<p>
     * Without hardware reset pin, without external VCC connected.
     * <p><b>PRE-conditions:</b> non-null {@code dim}, non-null {@code hwConn}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:23:57 AM Mar 28, 2017</i>
     * 
     * @param dim
     *            display dimensions
     * @param hwConn
     *            display hardware connection
     */
    public Display(final Dimensions dim, final SSD1306Connection hwConn) {
        this(dim, hwConn, null, false);
    }

    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.OLEDDisplay.<p>
     * With hardware reset pin, without external VCC connected.
     * <p><b>PRE-conditions:</b> non-null {@code dim}, non-null {@code hwConn}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:49:00 AM Mar 29, 2017</i>
     * 
     * @param dim
     *            display dimensions
     * @param hwConn
     *            display hardware connection
     * @param rstPin
     *            provisioned hardware reset pin if present, {@code null} if no hardware reset pin present
     */
    public Display(final Dimensions dim, final SSD1306Connection hwConn, final @Nullable GpioPinDigitalOutput rstPin) {
        this(dim, hwConn, rstPin, false);
    }

    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.OLEDDisplay.<p>
     * Without hardware reset pin, with external VCC connected.
     * <p><b>PRE-conditions:</b> non-null {@code dim}, non-null {@code hwConn}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>1:00:58 PM Mar 29, 2017</i>
     * 
     * @param dim
     *            display dimensions
     * @param hwConn
     *            display hardware connection
     * @param hasExternalVCC
     *            true if "VCC" pin is connected to external VCC source, false if "VCC" pin is connected with internal
     *            "charge pump"
     */
    public Display(final Dimensions dim, final SSD1306Connection hwConn, final boolean hasExternalVCC) {
        this(dim, hwConn, null, hasExternalVCC);
    }

    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.OLEDDisplay.<p>
     * With hardware reset pin, with external VCC connected.
     * <p><b>PRE-conditions:</b> non-null {@code dim}, non-null {@code hwConn}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>1:02:54 PM Mar 29, 2017</i>
     * 
     * @param dim
     *            display dimensions
     * @param hwConn
     *            display hardware connection
     * @param rstPin
     *            provisioned hardware reset pin if present, {@code null} if no hardware reset pin present
     * @param hasExternalVCC
     *            true if "VCC" pin is connected to external VCC source, false if "VCC" pin is connected with internal
     *            "charge pump"
     */
    public Display(
        final Dimensions dim,
        final SSD1306Connection hwConn,
        final @Nullable GpioPinDigitalOutput rstPin,
        final boolean hasExternalVCC) {

        this.dimensions = dim;
        this.hwConn = hwConn;
        this.rstPin = rstPin;
        this.hasExternalVCC = hasExternalVCC;

        pages = dim.height() / 8;
        dispBuffer = new byte[pages * dim.width()];

        img = new BufferedImage(dim.width(), dim.height(), BufferedImage.TYPE_BYTE_BINARY);
        graphics = notNull(img.createGraphics());
    }

    /**
     * Turn on command mode and send command.
     * <p><b>PRE-conditions:</b> non-null {@code cmd}
     * <br><b>POST-conditions:</b> non-null result
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>1:26:55 PM Mar 29, 2017</i>
     * 
     * @param cmd
     *            command to send
     * @return this instance (for call chaining)
     */
    private final Display command(final Command cmd) {
        hwConn.command(cmd);
        return this;
    }

    /**
     * Turn on command mode and send single byte value.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>10:42:10 PM Mar 29, 2017</i>
     * 
     * @param value
     *            byte value to be sent
     * @return this instance (for call chaining)
     */
    private final Display command(final byte value) {
        hwConn.command(value);
        return this;
    }

    /**
     * Turns on data mode and sends data array
     * 
     * @param data
     *            Data array
     * @return this instance (for call chaining)
     */
    /**
     * Turn on data mode and send data buffer.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> non-null result
     * <br><b>Side-effects:</b> GPIO I/O call
     * <br><b>Created on:</b> <i>1:34:24 PM Mar 29, 2017</i>
     * 
     * @param buffer
     *            byte buffer to send
     * @return this instance (for call chaining)
     */
    private final Display data(final byte[] buffer) {
        hwConn.data(buffer);
        return this;
    }

    /**
     * Perform display initialization sequence (see SSD1306 datasheet for more information).
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>2:01:34 PM Mar 29, 2017</i>
     */
    @SuppressWarnings("nls")
    private final void initDisplay() {
        // these magic numbers come from SSD1306 datasheet and depend on screen configuration:
        // multiplex - using smaller values results in "black" (unused) region on the screen, we use full screen
        // compins - configure COM pins layout, affects mapping of display buffer bits to pxels on the screen
        // ratio - clock divison factor and internal clock oscillation frequency - two 4-bit values combined in one byte
        if (dimensions == W128_H64) {
            initDisplay(asByte(0x3F), asByte(0x12), asByte(0x80));
        } else if (dimensions == W128_H32) {
            initDisplay(asByte(0x1F), asByte(0x02), asByte(0x80));
        } else if (dimensions == W96_H16) {
            initDisplay(asByte(0x0F), asByte(0x02), asByte(0x60));
        } else {
            LOG.error("unsupported display dimensions : dim = [%s]", dimensions);
            throw LOG.throwing(new AssertionError());
        }
    }

    /**
     * Perform display initialization sequence (see SSD1306 datasheet for more information).
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>6:22:51 PM Mar 29, 2017</i>
     * 
     * @param multiplex
     *            multiplex ratio
     * @param compins
     *            COM pins layout
     * @param ratio
     *            division factor/clock oscillation freq
     */
    @SuppressWarnings("nls")
    private final void initDisplay(final byte multiplex, final byte compins, final byte ratio) {
        LOG.traceEntry("multiplex = [{}] ; compins = [{}] ; ratio = [{}]", hex(multiplex), hex(compins), hex(ratio));

        runSync(lockGpio, () -> {
            command(DISPLAY_OFF);
            command(SET_CLOCK_DIV);
            command(ratio);
            command(SET_MULTIPLEX);
            command(multiplex);
            command(SET_DISPLAY_OFFSET);
            command(asByte(0x0));
            command(SET_DISPLAY_START_LINE_0);
            command(SET_CHARGE_PUMP);
            command(hasExternalVCC ? CHARGE_PUMP_DISABLE : CHARGE_PUMP_ENABLE);

            command(SET_MEMORY_ADDRESSING_MODE);
            command(MEMORY_ADDRESSING_MODE_HORIZONTAL);
            command(SET_SEGMENT_REMAP_127);
            command(SET_COM_OUTPUT_SCAN_DIRECTION_REMAPPED);
            command(SET_COM_PINS_CONFIGURATION);
            command(compins);

            contrast.set(hasExternalVCC ? 0x9F : 0xCF); // these magic numbers were found in other implementations
            contrast(asByte(contrast.get()));

            command(SET_PRECHARGE_PERIOD);
            command(hasExternalVCC ? asByte(0x22) : asByte(0xF1)); // send pre-charge period value

            command(SET_VCOMH_DESELECT_LEVEL);
            command(asByte(0x40));
            command(DISPLAY_ON_RESUME);
            command(SET_NORMAL_DISPLAY);
        });

        LOG.traceExit();
    }

    /**
     * Init and turn on the display.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>2:04:24 PM Apr 3, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    public final Display begin() {
        LOG.traceEntry();

        runSync(lockGpio, () -> {
            hwReset();
            initDisplay();
            command(DISPLAY_ON);
            clear();
            sync();
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Reset display using "RESET" pin : pull reset pin high and low. Nothing is done if display has no "RESET" pin.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>2:10:18 PM Apr 3, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display hwReset() {
        LOG.traceEntry();

        if (rstPin != null) {
            final GpioPinDigitalOutput rstPinSafe = notNull(rstPin);
            runSync(lockGpio, () -> {
                try {
                    rstPinSafe.setState(true);
                    Thread.sleep(1);
                    rstPinSafe.setState(false);
                    Thread.sleep(10);
                    rstPinSafe.setState(true);
                } catch (final InterruptedException e) {
                    LOG.error("unexpected thread interruption", e);
                }
            });
        }

        return notNull(LOG.traceExit(this));
    }

    /**
     * Convert packed RBG value to monochrome {@link PixelState}.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>5:51:48 PM Apr 7, 2017</i>
     * 
     * @param rgb
     *            packed rgb color value
     * @return converted monochrome pixel state
     */
    private static final PixelState rgbToPixelState(final int rgb) {
        final Color color = new Color(rgb);
        final int rgbSum = color.getRed() + color.getGreen() + color.getBlue();
        return (rgbSum > MONOCHROME_THRESHOLD) ? PixelState.ON : PixelState.OFF;
    }

    /**
     * Sync current image state with display. Should be used when drawing on {@link #graphics()} is finished and those
     * modifications need to be displayed on screen.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>2:53:35 PM Apr 3, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    public final Display sync() {
        LOG.traceEntry();

        // convert image to display buffer pixel by pixel
        img.flush();
        for (int y = 0 ; y < height() ; y++) {
            for (int x = 0 ; x < width() ; x++) {
                final PixelState pixel = rgbToPixelState(img.getRGB(x, y));
                pixelToBuffer(x, y, pixel);
            }
        }

        runSync(lockGpio, () -> {
            command(SET_COLUMN_ADDR);
            command(asByte(0x00)); // column addr range start
            command(asByte(dimensions.width() - 1)); // column addr range end
            command(Command.SET_PAGE_ADDR);
            command(asByte(0x00)); // page addr range start
            command(asByte(pages - 1)); // page addr range end

            data(dispBuffer);
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Clear current image state. Note that display is <em>not</em> updated, use {@link #sync()} to update the display.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> state of the internal image is modified
     * <br><b>Created on:</b> <i>11:58:37 PM Apr 3, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    public final Display clear() {
        LOG.traceEntry();

        graphics().setColor(BLACK);
        graphics().fillRect(0, 0, dimensions.width(), dimensions.height());

        return notNull(LOG.traceExit(this));
    }

    /**
     * Set display contrast.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>12:06:18 AM Apr 4, 2017</i>
     * 
     * @param contrastFactor
     *            factor from {@code 0.0D} to {@code 1.0D}, where {@code 0.0D} is lowest contrast, and {@code 1.0D} is
     *            highest contrast
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "boxing", "nls" })
    public final Display contrast(final double contrastFactor) {
        LOG.traceEntry("contrastFactor = [{}]", contrastFactor);

        final int contrastInt = toContrastInt(contrastFactor);
        LOG.trace("setting contrast : contrastByte = [%s]", hex(asByte(contrastInt)));
        contrast.set(contrastInt);
        contrast(asByte(contrastInt));

        return notNull(LOG.traceExit(this));
    }

    /**
     * Convert double contrast factor to integer between {@code 0} and {@code 255}.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> 0 &lt;= result &lt;= 255
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>12:38:24 AM Apr 4, 2017</i>
     * 
     * @param contrastFactor
     *            factor from {@code 0.0D} to {@code 1.0D}, where {@code 0.0D} is lowest contrast, and {@code 1.0D} is
     *            highest contrast
     * @return contrast integer value between {@code 0} and {@code 255}
     */
    @SuppressWarnings({ "boxing", "nls" })
    private static final int toContrastInt(final double contrastFactor) {
        double contrastNorm = contrastFactor;
        if (Double.compare(contrastFactor, 0.0D) < 0) {
            LOG.error("contrast factor is less than 0.0D, normalizing : contrastFactor = [%f]", contrastFactor);
            contrastNorm = 0.0D;
        }
        if (Double.compare(contrastFactor, 1.0D) > 0) {
            LOG.error("contrast factor is more than 1.0D, normalizing : contrastFactor = [%f]", contrastFactor);
            contrastNorm = 1.0D;
        }
        return (int) (contrastNorm * 255);
    }

    /**
     * Set display contrast.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>12:40:28 AM Apr 4, 2017</i>
     * 
     * @param contrastByte
     *            contrast byte value
     */
    private final void contrast(final byte contrastByte) {
        runSync(lockGpio, () -> {
            command(SET_CONTRAST);
            command(contrastByte);
        });
    }

    /**
     * Dim or un-dim display.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>12:22:13 AM Apr 4, 2017</i>
     * 
     * @param dim
     *            true if display should be dimmed, false if it should be un-dimmed
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "boxing", "nls" })
    public final Display dim(final boolean dim) {
        LOG.traceEntry("dim = [{}]", dim);

        contrast(dim ? asByte(0x00) : asByte(contrast.get()));

        return notNull(LOG.traceExit(this));
    }

    /**
     * Set "inverted" display mode on/off.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>12:50:34 AM Apr 4, 2017</i>
     * 
     * @param inverse
     *            true if display should be inverted, false otherwise
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "boxing", "nls" })
    public final Display invert(final boolean inverse) {
        LOG.traceEntry("inverse = [{}]", inverse);

        runSync(lockGpio, () -> {
            command(inverse ? SET_INVERSE_DISPLAY : SET_NORMAL_DISPLAY);
        });

        return notNull(LOG.traceExit(this));
    }


    /**
     * Project : pi_java_ssd1306<p>
     * Horizontal scroll directions.
     * <p><b>Created on:</b> <i>5:45:40 PM Apr 4, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static enum HorizontalDirection {
            /**
             * Left horizontal scrolling direction.
             * <p><b>Created on:</b> <i>5:45:51 PM Apr 4, 2017</i>
             */
            LEFT,

            /**
             * Right horizontal scrolling direction.
             * <p><b>Created on:</b> <i>5:46:04 PM Apr 4, 2017</i>
             */
            RIGHT;
    }

    /**
     * Project : pi_java_ssd1306<p>
     * Vertical scroll direction.
     * <p><b>Created on:</b> <i>10:26:34 AM Apr 22, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static enum VerticalDirection {
            /**
             * Up vertical scrolling direction.
             * <p><b>Created on:</b> <i>10:07:43 AM Apr 22, 2017</i>
             */
            UP,

            /**
             * Down vertical scrolling direction.
             * <p><b>Created on:</b> <i>10:08:02 AM Apr 22, 2017</i>
             */
            DOWN;
    }

    /**
     * Project : pi_java_ssd1306<p>
     * Scrolling frequency in terms of frames. For instance, {@link ScrollFrequency#FRAMES_5} means that one scrolling
     * step is performed each 5 time clocking frames.
     * <p><b>Created on:</b> <i>2:08:15 AM Apr 5, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static enum ScrollFrequency {
            /**
             * 2 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:13:32 AM Apr 5, 2017</i>
             */
            FRAMES_2(asByte(0b111)),

            /**
             * 3 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:13:43 AM Apr 5, 2017</i>
             */
            FRAMES_3(asByte(0b100)),

            /**
             * 4 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:13:51 AM Apr 5, 2017</i>
             */
            FRAMES_4(asByte(0b101)),

            /**
             * 5 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:14:01 AM Apr 5, 2017</i>
             */
            FRAMES_5(asByte(0b000)),

            /**
             * 25 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:14:10 AM Apr 5, 2017</i>
             */
            FRAMES_25(asByte(0b110)),

            /**
             * 64 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:14:20 AM Apr 5, 2017</i>
             */
            FRAMES_64(asByte(0b001)),

            /**
             * 128 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:14:31 AM Apr 5, 2017</i>
             */
            FRAMES_128(asByte(0b010)),

            /**
             * 256 frames scrolling frequency.
             * <p><b>Created on:</b> <i>2:14:45 AM Apr 5, 2017</i>
             */
            FRAMES_256(asByte(0b011));

        /**
         * Byte code of the scrolling frame frequency.
         * <p><b>Created on:</b> <i>2:11:52 AM Apr 5, 2017</i>
         */
        private final byte code;


        /**
         * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.ScrollFrequency.<p>
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>2:12:13 AM Apr 5, 2017</i>
         * 
         * @param code
         *            byte code of the scrolling frame frequency
         */
        private ScrollFrequency(final byte code) {
            this.code = code;
        }

        /**
         * Get byte code of this scrolling frame frequency.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>2:12:33 AM Apr 5, 2017</i>
         * 
         * @return byte code of this scrolling frame frequency
         */
        public final byte code() {
            return code;
        }
    }


    /**
     * Scroll all screen area horizontally with default scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code direction}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>8:37:55 PM Apr 21, 2017</i>
     * 
     * @param direction
     *            horizontal scrolling direction
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollHorizontal(final HorizontalDirection direction) {
        LOG.traceEntry("direction = [{}]", direction);
        return notNull(LOG.traceExit(
            scrollHorizontal(direction, SCROLL_FREQ_DEFAULT, 0, pages - 1)));
    }

    /**
     * Scroll all screen area horizontally with specified scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code direction}, non-null {@code frequency}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>8:39:49 PM Apr 21, 2017</i>
     * 
     * @param direction
     *            horizontal scrolling direction
     * @param frequency
     *            scrolling frequency
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollHorizontal(final HorizontalDirection direction, final ScrollFrequency frequency) {
        LOG.traceEntry("direction = [{}] ; frequency = [{}]", direction, frequency);
        return notNull(LOG.traceExit(
            scrollHorizontal(direction, frequency, 0, pages - 1)));
    }

    /**
     * Start continuous horizontal scrolling of area between specified pages. One page is a 8-leds vertical column.
     * Therefore, display matrix has {@code HEIGHT/8} pages.
     * <p><b>PRE-conditions:</b> non-null {@code direction}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>5:51:23 PM Apr 4, 2017</i>
     * 
     * @param direction
     *            horizontal scrolling direction
     * @param frequency
     *            scrolling frequency in terms of frames
     * @param startPage
     *            start page (horizontal "coordinate") of the scrolling segment
     * @param endPage
     *            end page (horizontal "coordinate") of the scrolling segment
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "nls", "boxing" })
    public final Display scrollHorizontal(
        final HorizontalDirection direction,
        final ScrollFrequency frequency,
        final int startPage,
        final int endPage) {

        LOG.traceEntry("direction = [{}] ; frequency = [{}] ; startPage = [{}] ; endPage = [{}]",
            direction, frequency, startPage, endPage);
        isTrue(0 <= startPage && startPage < pages);
        isTrue(startPage <= endPage && endPage < pages);

        runSync(lockGpio, () -> {
            stopScroll(); // stop previous scrolling and restore display state according to the buffer
            command(direction == LEFT ? SCROLL_HORIZONTAL_LEFT : SCROLL_HORIZONTAL_RIGHT);
            command(asByte(0x00)); // dummy byte
            command(asByte(startPage)); // start page
            command(frequency.code()); // interval between scroll steps
            command(asByte(endPage)); // end page
            command(asByte(0x00)); // dummy byte
            command(asByte(0xFF)); // dummy byte
            command(SCROLL_ACTIVATE);
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Scroll all screen vertically with default scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code direction}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>10:56:56 AM Apr 22, 2017</i>
     * 
     * @param direction
     *            vertical scrolling direction
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollVertical(final VerticalDirection direction) {
        LOG.traceEntry("direction = [{}]", direction);
        scrollVertical(direction, SCROLL_FREQ_DEFAULT);
        return notNull(LOG.traceExit(this));
    }

    /**
     * Scroll all screen vertically with specified scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code direction}, non-null {@code frequency}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>7:07:13 PM Apr 23, 2017</i>
     * 
     * @param direction
     *            vertical scrolling direction
     * @param frequency
     *            scrolling frequency
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollVertical(final VerticalDirection direction, final ScrollFrequency frequency) {
        LOG.traceEntry("direction = [{}] ; frequency = [{}]", direction, frequency);
        final int verticalOffset = (direction == UP) ? 1 : height() - 1;
        scrollDiagonal(LEFT, frequency, 0, 0, verticalOffset, 0, height());
        return notNull(LOG.traceExit(this));
    }

    /**
     * Scroll all screen diagonally with default scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code horiz}, non-null {@code vert}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>10:18:34 AM Apr 22, 2017</i>
     * 
     * @param horiz
     *            horizontal scrolling direction
     * @param vert
     *            vertical scrolling direction
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollDiagonal(final HorizontalDirection horiz, final VerticalDirection vert) {
        LOG.traceEntry("horiz = [{}] ; vert = [{}]", horiz, vert);
        scrollDiagonal(horiz, vert, SCROLL_FREQ_DEFAULT);
        return notNull(LOG.traceExit(this));
    }

    /**
     * Scroll all screen diagonally with specified scrolling frequency.
     * <p><b>PRE-conditions:</b> non-null {@code horiz}, non-null {@code vert}, non-null {@code frequency}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>10:24:58 AM Apr 22, 2017</i>
     * 
     * @param horiz
     *            horizontal scrolling direction
     * @param vert
     *            vertical scrolling direction
     * @param frequency
     *            scrolling frequency
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display scrollDiagonal(
        final HorizontalDirection horiz,
        final VerticalDirection vert,
        final ScrollFrequency frequency) {
        LOG.traceEntry("horiz = [{}] ; vert = [{}] ; frequency = [{}]", horiz, vert, frequency);

        final int verticalOffset = (vert == UP) ? 1 : height() - 1;
        scrollDiagonal(horiz, frequency, 0, pages - 1, verticalOffset, 0, height());
        return notNull(LOG.traceExit(this));
    }

    /**
     * Start continuous diagonal (horizontal + vertical) scrolling. One page is a 8-leds vertical column. Therefore,
     * display matrix has {@code HEIGHT/8} pages.
     * <p><b>PRE-conditions:</b> non-null {@code direciton}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>1:58:31 AM Apr 5, 2017</i>
     * 
     * @param direction
     *            horizontal component scroll direction
     * @param frequency
     *            scrolling frequency in terms of frames
     * @param startPage
     *            start page (horizontal "coordinate") of the scrolling segment
     * @param endPage
     *            end page (horizontal "coordinate") of the scrolling segment
     * @param verticalOffset
     *            vertical scrolling offset in rows (from 0 to {@code HEIGHT - 1}
     * @param rowsFixed
     *            number of rows in top fixed area (0 for full screen scrolling)
     * @param rowsScroll
     *            number of rows in scroll area (height for full screen scrolling)
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "nls", "boxing" })
    public final Display scrollDiagonal(
        final HorizontalDirection direction,
        final ScrollFrequency frequency,
        final int startPage,
        final int endPage,
        final int verticalOffset,
        final int rowsFixed,
        final int rowsScroll) {

        LOG.traceEntry("direction = [{}] ; frequency = [{}] ; startPage = [{}] ; endPage = [{}] ; "
            + "verticaloffset = [{}] ; rowsFixed = [{}] ; rowsScroll = [{}]",
            direction, frequency, startPage, endPage, verticalOffset, rowsFixed, rowsScroll);
        isTrue(0 <= startPage && startPage < pages);
        isTrue(startPage <= endPage && endPage < pages);
        isTrue(0 <= verticalOffset && verticalOffset < width());

        runSync(lockGpio, () -> {
            stopScroll(); // stop previous scrolling and restore display state according to the buffer
            command(SET_VERTICAL_SCROLL_AREA);
            command(asByte(rowsFixed));
            command(asByte(rowsScroll));

            command(direction == LEFT ? SCROLL_DIAGONAL_LEFT : SCROLL_DIAGONAL_RIGHT);
            command(asByte(0x00)); // dummy byte
            command(asByte(startPage)); // start page
            command(frequency.code()); // scrolling frequency
            command(asByte(endPage));
            command(asByte(verticalOffset));
            command(SCROLL_ACTIVATE);
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Stop current scrolling and restore display state according to the current image state.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>11:05:36 AM Apr 5, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    public final Display stopScroll() {
        LOG.traceEntry();

        runSync(lockGpio, () -> {
            command(SCROLL_DEACTIVATE);
            sync();
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Get display width.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:08:58 AM Apr 5, 2017</i>
     * 
     * @return display width
     */
    public final int width() {
        return dimensions.width();
    }

    /**
     * Get display height.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:09:41 AM Apr 5, 2017</i>
     * 
     * @return display height
     */
    public final int height() {
        return dimensions.height();
    }

    /**
     * Get number of 8-bit rows this display has (8-led columns).
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>11:10:12 AM Apr 5, 2017</i>
     * 
     * @return number of pages this display has
     */
    public final int pages() {
        return pages;
    }


    /**
     * Project : pi_java_ssd1306<p>
     * Position in 2D space of the display image matrix. Axis directions are as in {@code java.awt} package.
     * <p>Negative coordinates positions are possible, it's client code responsibility to validate coordinates
     * constraints.
     * <p><b>Created on:</b> <i>1:56:35 AM Apr 7, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static final class Position {

        /**
         * X coordinate.
         * <p><b>Created on:</b> <i>1:57:27 AM Apr 7, 2017</i>
         */
        private final int x;

        /**
         * Y coordinate.
         * <p><b>Created on:</b> <i>1:58:02 AM Apr 7, 2017</i>
         */
        private final int y;


        /**
         * Factory method for producing {@link Position} instances.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>1:42:24 AM Apr 9, 2017</i>
         * 
         * @param x
         *            X coordinate
         * @param y
         *            Y coordinate
         * @return new {@link Position} instance
         */
        public static final Position of(final int x, final int y) {
            return new Position(x, y);
        }


        /**
         * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.Position.<p>
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>1:58:15 AM Apr 7, 2017</i>
         * 
         * @param x
         *            X coordinate
         * @param y
         *            Y coordinate
         */
        public Position(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Get X coordinate of this position.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>1:59:24 AM Apr 7, 2017</i>
         * 
         * @return X coordinate
         */
        public final int x() {
            return x;
        }

        /**
         * Get Y coordinate of this position.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> NONE
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>1:59:51 AM Apr 7, 2017</i>
         * 
         * @return Y coordinate
         */
        public final int y() {
            return y;
        }

        /**
         * Get string representation of this position.
         * <p><b>PRE-conditions:</b> NONE
         * <br><b>POST-conditions:</b> non-empty {@code result}
         * <br><b>Side-effects:</b> NONE
         * <br><b>Created on:</b> <i>1:31:24 PM Apr 26, 2017</i>
         * 
         * @see java.lang.Object#toString()
         * @return string representation of this position
         */
        @SuppressWarnings("nls")
        @Override
        public final String toString() {
            return notNull(new StringBuilder()
                .append("{x=[").append(x)
                .append("],y=[").append(y)
                .append("]}")
                .toString());
        }
    }

    /**
     * Project : pi_java_ssd1306<p>
     * Strategy for drawing image over another one.
     * <p><b>Created on:</b> <i>1:45:01 AM Apr 7, 2017</i>
     * 
     * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
     * @version 0.1
     */
    public static enum OverlayType {
            /**
             * Both {@link PixelState#ON} and {@link PixelState#OFF} pixels are drawn, effectively erasing all the
             * underlying image content.
             * <p><b>Created on:</b> <i>1:46:27 AM Apr 7, 2017</i>
             */
            FULL,

            /**
             * Only {@link PixelState#ON} pixels are drawn. {@link PixelState#OFF} pixels are untouched.
             * <p><b>Created on:</b> <i>1:47:41 AM Apr 7, 2017</i>
             */
            ON_PIXELS,

            /**
             * Only {@link PixelState#OFF} pixels are drawn. {@link PixelState#ON} pixels are untouched. This is useful
             * for erasing some complex shape.
             * <p><b>Created on:</b> <i>1:50:25 AM Apr 7, 2017</i>
             */
            OFF_PIXELS,

            /**
             * None pixels are drawn at all. This seems to be potentially useful for some scenarios in client code.
             * <p><b>Created on:</b> <i>1:52:23 AM Apr 7, 2017</i>
             */
            NONE;
    }


    /**
     * Set pixel state in display buffer. Should be used for converting raster image into display buffer representation
     * pixel by pixel.
     * <p><b>PRE-conditions:</b> non-null {@code pixel}, 0 &lt;= x &lt; width, 0 &lt;= y &lt; height
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object state is modified
     * <br><b>Created on:</b> <i>11:30:44 AM Apr 5, 2017</i>
     * 
     * @param x
     *            pixel x coordinate
     * @param y
     *            pixel y coordinate
     * @param pixel
     *            new pixel state
     * @return this instance (for call chaining)
     */
    private final Display pixelToBuffer(final int x, final int y, final PixelState pixel) {
        assert (isTrue(0 <= x && x < width()));
        assert (isTrue(0 <= y && y < height()));

        final int page = y / 8;
        final int index = (page * width()) + x;
        final int offsetInPage = y % 8;
        if (pixel == PixelState.ON) {
            dispBuffer[index] |= asByte(1 << offsetInPage);
        } else { // pixel OFF
            dispBuffer[index] &= ~asByte(1 << offsetInPage);
        }

        return this;
    }

    /**
     * Scale and place image on display. Given image overlays ({@link OverlayType#FULL}) current display image content
     * in rectangle between {@code leftTop} and {@code rightBottom}.
     * <p>NOTE: this method only changes internal state of this object, updated image is not transmitted to the display.
     * To sync internal state with display use {@link #sync()}.
     * <p><b>PRE-conditions:</b> non-null {@code img}, non-null {@code leftTop}, non-null {@code rightBottom}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object state is modified, graphics object is modified
     * <br><b>Created on:</b> <i>1:05:34 AM Apr 7, 2017</i>
     * 
     * @param img
     *            image to scale and draw
     * @param leftTop
     *            left top edge position of the scaled image borders rectangle on the display
     * @param rightBottom
     *            right bottom position of the scaled image borders rectangle on the display
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "hiding", "nls" })
    public final Display image(final BufferedImage img, final Position leftTop, final Position rightBottom) {
        LOG.traceEntry("img = [{}] ; leftTop = [{}] ; rightBottom = [{}]", img, leftTop, rightBottom);
        return notNull(LOG.traceExit(
            image(img, leftTop, rightBottom, FULL)));
    }

    /**
     * Scale and place image on display using provided overlay strategy.
     * <p>Given image may either overlay or mix with current image content, or even not drawn at all. Particularly, this
     * depends on {@code overlay} parameter:
     * <ul>
     * <li> {@link OverlayType#FULL} - both {@link PixelState#ON} and {@link PixelState#OFF} pixels of the {@code img}
     * are rendered, effectively erasing underlying image between {@code leftTop} and {@code rightBottom}
     * <li> {@link OverlayType#ON_PIXELS} - only {@link PixelState#ON} pixels of the {@code img} are rendered,
     * effectively mixing underlying image with provided one
     * <li> {@link OverlayType#OFF_PIXELS} - only {@link PixelState#OFF} pixels of the {@code img} are rendered,
     * effectively erasing {@code img} shape from current underlying image
     * <li> {@link OverlayType#NONE} - this method does absolutely nothing for this type of overlay
     * </ul>
     * <p>NOTE: this method only changes internal state of this object, updated image is not transmitted to the display.
     * To sync internal state with display use {@link #sync()}.
     * <p><b>PRE-conditions:</b> non-null {@code img}, non-null {@code leftTop}, non-null {@code rightBottom}, non-null
     * {@code overlay}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object is modified, graphics object is modified
     * <br><b>Created on:</b> <i>1:35:32 AM Apr 7, 2017</i>
     * 
     * @param img
     *            image to be scaled and drawn
     * @param leftTop
     *            target position of the left top edge of the {@code img}
     * @param rightBottom
     *            target position of the right bottom edge of the {@code img}
     * @param overlay
     *            overlay type
     * @return this instance (for call chaining)
     */
    @SuppressWarnings({ "hiding", "nls" })
    public final Display image(
        final BufferedImage img,
        final Position leftTop,
        final Position rightBottom,
        final OverlayType overlay) {

        LOG.traceEntry("img = [{}] ; leftTop = [{}] ; rightBottom = [{}] ; overlay = [{}]",
            img, leftTop, rightBottom, overlay);
        isTrue(leftTop.x() < rightBottom.x());
        isTrue(leftTop.y() < rightBottom.y());

        if (overlay != NONE) {
            final int width = rightBottom.x() - leftTop.x();
            final int height = rightBottom.y() - leftTop.y();
            final BufferedImage scaled = scale(img, width, height);
            // draw pixel by pixel depending on overlay strategy
            // this may be not the most efficient way to draw image, but flexibility of the overlay strategies is more
            // important
            for (int x = 0 ; x < width ; x++) {
                for (int y = 0 ; y < height ; y++) {
                    final int xAbs = x + leftTop.x();
                    final int yAbs = y + leftTop.y();
                    // draw only if pixel absolute position is inside the visible area (display pixel matrix)
                    if ((0 <= xAbs && xAbs < width()) && (0 <= yAbs && yAbs < height())) {
                        final PixelState pixel = rgbToPixelState(scaled.getRGB(x, y));
                        if (pixel == PixelState.ON && (overlay == FULL || overlay == ON_PIXELS)) {
                            graphics().setColor(WHITE);
                            graphics().drawLine(xAbs, yAbs, xAbs, yAbs); // draw WHITE point
                        } else if (pixel == PixelState.OFF && (overlay == FULL || overlay == OFF_PIXELS)) {
                            graphics().setColor(BLACK);
                            graphics().drawLine(xAbs, yAbs, xAbs, yAbs); // draw BLACK point
                        }
                    }
                }
            }
        }

        return notNull(LOG.traceExit(this));
    }

    /**
     * Scale image to fit in provided {@code width} and {@code height}.
     * <p><b>PRE-conditions:</b> non-null {@code img}, positive {@code width}, positive {@code height}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>2:55:17 AM Apr 7, 2017</i>
     * 
     * @param img
     *            image to scale
     * @param width
     *            target image width
     * @param height
     *            target image height
     * @return scaled image
     */
    private static final BufferedImage scale(final BufferedImage img, final int width, final int height) {
        final double widthOrig = img.getWidth();
        final double heightOrig = img.getHeight();
        final double sx = width / widthOrig;
        final double sy = height / heightOrig;
        final AffineTransform trns = AffineTransform.getScaleInstance(sx, sy);
        final AffineTransformOp scale = new AffineTransformOp(trns, TYPE_BILINEAR);
        return notNull(scale.filter(img, new BufferedImage(width, height, img.getType())));
    }

    /**
     * Returns Graphics object which is associated to current AWT image,
     * if it wasn't set using setImage() with false createGraphics parameter
     * 
     * @return Graphics2D object
     */
    public final Graphics2D graphics() {
        return this.graphics;
    }

    /**
     * Draw text on screen in the specified position, using default font and @link OverlayType#FULL} overlay type.
     * <p>NOTE: display is not updated until {@link #sync()} is called
     * <p><b>PRE-conditions:</b> non-null {@code text}, non-null {@code leftTop}, non-null {@code font}, non-null
     * {@code overlay}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object is modified, graphics object is modified
     * <br><b>Created on:</b> <i>2:07:47 AM Apr 9, 2017</i>
     * 
     * @param text
     *            text to draw
     * @param leftTop
     *            left top position of text bounds
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display text(final String text, final Position leftTop) {
        LOG.traceEntry("text = [{}] ; leftTop = [{}]", text, leftTop);
        return notNull(LOG.traceExit(
            text(text, leftTop, FONT_DEFAULT, FULL)));
    }

    /**
     * Draw text on screen in the specified position, using specified font and {@link OverlayType#FULL} overlay type.
     * <p>NOTE: display is not updated until {@link #sync()} is called
     * <p><b>PRE-conditions:</b> non-null {@code text}, non-null {@code leftTop}, non-null {@code font}, non-null
     * {@code overlay}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object is modified, graphics object is modified
     * <br><b>Created on:</b> <i>2:07:45 AM Apr 9, 2017</i>
     * 
     * @param text
     *            text to draw
     * @param leftTop
     *            left top position of text bounds
     * @param font
     *            font to be used for drawing text
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display text(final String text, final Position leftTop, final Font font) {
        LOG.traceEntry("text = [{}] ; leftTop = [{}] ; font = [{}]", text, leftTop, font);
        return notNull(LOG.traceExit(
            text(text, leftTop, font, FULL)));
    }

    /**
     * Draw text on screen in the specified position, using default font and specified overlay type.
     * <p>NOTE: display is not updated until {@link #sync()} is called
     * <p><b>PRE-conditions:</b> non-null {@code text}, non-null {@code leftTop}, non-null {@code font}, non-null
     * {@code overlay}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object is modified, graphics object is modified
     * <br><b>Created on:</b> <i>2:06:12 AM Apr 9, 2017</i>
     * 
     * @param text
     *            text to draw
     * @param leftTop
     *            left top position of text bounds
     * @param overlay
     *            overlay type to be used for drawing
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display text(final String text, final Position leftTop, final OverlayType overlay) {
        LOG.traceEntry("text = [{}] ; leftTop = [{}] ; overlay = [{}]", text, leftTop, overlay);
        return notNull(LOG.traceExit(
            text(text, leftTop, FONT_DEFAULT, overlay)));
    }

    /**
     * Draw text on screen in the specified position, using specified font and overlay type.
     * <p>NOTE: display is not updated until {@link #sync()} is called
     * <p><b>PRE-conditions:</b> non-null {@code text}, non-null {@code leftTop}, non-null {@code font}, non-null
     * {@code overlay}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> this object is modified, graphics object is modified
     * <br><b>Created on:</b> <i>2:00:57 AM Apr 9, 2017</i>
     * 
     * @param text
     *            text to draw
     * @param leftTop
     *            left top position of text bounds
     * @param font
     *            font to be used for drawing text
     * @param overlay
     *            overlay type to be used for drawing
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display text(
        final String text,
        final Position leftTop,
        final Font font,
        final OverlayType overlay) {

        LOG.traceEntry("text = [{}] ; leftTop = [{}] ; font = [{}] ; overlay = [{}]", text, leftTop, font, overlay);

        // To use overlay types we need to draw text pixel by pixel. This is achieved by rendering text to a separate
        // temporary buffered image. And then this image is rendered pixel by pixel on top of display image according to
        // the provided overlay type using routines for rendering images.

        // evaluate text metrics
        graphics().setFont(font);
        final Rectangle2D txtBounds = graphics().getFontMetrics().getStringBounds(text, graphics());
        final int txtWidth = (int) Math.ceil(txtBounds.getWidth());
        final int txtHeight = (int) Math.ceil(txtBounds.getHeight());

        // create temporary buffered image to draw text into it
        final BufferedImage txtImg = new BufferedImage(txtWidth, txtHeight, BufferedImage.TYPE_BYTE_BINARY);
        final Graphics2D txtGraphics = txtImg.createGraphics();
        txtGraphics.setFont(font);
        txtGraphics.drawString(text, 0, txtHeight - 1);
        txtImg.flush();

        // draw text on display image, re-use image drawing overlay-aware routine
        final Position rightBottom = Position.of(leftTop.x() + txtWidth, leftTop.y() + txtHeight);
        image(txtImg, leftTop, rightBottom, overlay);

        // clean up temporary image
        txtGraphics.dispose();

        return notNull(LOG.traceExit(this));
    }

    /**
     * Sleep on current thread for specified time duration. Just utility method for more fluent API.
     * <p><b>PRE-conditions:</b> non-null {@code duration}
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> current thread sleep, this method blocks
     * <br><b>Created on:</b> <i>10:56:05 PM Apr 18, 2017</i>
     * 
     * @param duration
     *            duration to sleep
     * @return this instance (for call chaining)
     */
    @SuppressWarnings("nls")
    public final Display sleep(final Duration duration) {
        LOG.traceEntry("duration = [{}]", duration);
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException e) {
            LOG.error("unexpected thread interruption, ignored", e);
        }

        return notNull(LOG.traceExit(this));
    }

    /**
     * Clear display contents and turn it OFF.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-null {@code result}
     * <br><b>Side-effects:</b> GPIO I/O calls
     * <br><b>Created on:</b> <i>11:26:48 PM Apr 18, 2017</i>
     * 
     * @return this instance (for call chaining)
     */
    public final Display stop() {
        LOG.traceEntry();

        runSync(lockGpio, () -> {
            stopScroll();
            invert(false);
            dim(false);
            clear();
            sync();
            command(DISPLAY_OFF);
        });

        return notNull(LOG.traceExit(this));
    }

    /**
     * Get string representation of this display.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-empty {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>1:39:55 PM Apr 26, 2017</i>
     * 
     * @see java.lang.Object#toString()
     * @return string representation of this display
     */
    @SuppressWarnings("nls")
    @Override
    public final String toString() {
        return notNull(new StringBuilder()
            .append("{dim=[").append(dimensions)
            .append("],hasRst=[").append(rstPin != null)
            .append("],hwConn=[").append(hwConn)
            .append("]}")
            .toString());
    }

}
