package dburyak.pi.ssd1306;


import static dburyak.pi.ssd1306.Util.asByte;
import static dburyak.pi.ssd1306.Util.hex;
import static dburyak.pi.ssd1306.Util.notNull;


/**
 * Project : pi_java_ssd1306<p>
 * SSD1306 commands.
 * <p><b>Created on:</b> <i>7:39:56 PM Mar 27, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
public enum Command {
        /**
         * Set display to sleep mode.
         * <p><b>Created on:</b> <i>10:12:58 PM Mar 29, 2017</i>
         */
        DISPLAY_OFF(asByte(0xAE)),

        /**
         * Set clock divison factor and internal clock oscillation frequency.
         * <p><b>Created on:</b> <i>10:22:41 PM Mar 29, 2017</i>
         */
        SET_CLOCK_DIV(asByte(0xD5)),

        /**
         * Set multiplex ratio. Must be followed by ratio value ranging from 16 to 63 sent in command mode.
         * <p><b>Created on:</b> <i>4:37:42 PM Mar 30, 2017</i>
         */
        SET_MULTIPLEX(asByte(0xA8)),

        /**
         * Set mapping of the display start line to one of the COM pins. Must be followed by offset value byte sent in
         * command mode (see SSD1306 datasheet for more information on possible offset values).
         * <p><b>Created on:</b> <i>4:44:04 PM Mar 30, 2017</i>
         */
        SET_DISPLAY_OFFSET(asByte(0xD3)),

        /**
         * Set mapping of display row to RAM register 0, which effectively turns OFF picture shifting.
         * <p><b>Created on:</b> <i>5:01:57 PM Mar 30, 2017</i>
         */
        SET_DISPLAY_START_LINE_0(asByte(0x40)),

        /**
         * Set memory addressing mode. Must be followed by either {@link #MEMORY_ADDRESSING_MODE_HORIZONTAL},
         * {@link #MEMORY_ADDRESSING_MODE_VERTICAL} or {@link #MEMORY_ADDRESSING_MODE_PAGE}.
         * <p><b>Created on:</b> <i>5:17:11 PM Mar 30, 2017</i>
         */
        SET_MEMORY_ADDRESSING_MODE(asByte(0x20)),

        /**
         * "Page" memory addressing mode.
         * <p><b>Created on:</b> <i>1:44:20 AM Mar 31, 2017</i>
         */
        MEMORY_ADDRESSING_MODE_PAGE(asByte(0x02)),

        /**
         * "Horizontal" memory addressing mode.
         * <p><b>Created on:</b> <i>1:45:27 AM Mar 31, 2017</i>
         */
        MEMORY_ADDRESSING_MODE_HORIZONTAL(asByte(0x00)),

        /**
         * "Vertical" addressing mode.
         * <p><b>Created on:</b> <i>1:45:41 AM Mar 31, 2017</i>
         */
        MEMORY_ADDRESSING_MODE_VERTICAL(asByte(0x01)),

        /**
         * Set mapping between the display data column address and the segment driver: column address 127 is mapped to
         * SEG0 (see SSD1306 datasheet for more info).
         * <p><b>Created on:</b> <i>1:47:13 AM Mar 31, 2017</i>
         */
        SET_SEGMENT_REMAP_127(asByte(0xA1)),

        /**
         * Configure "charge pump". This command <em>must</em> be followed by either {@link #CHARGE_PUMP_ENABLE} or
         * {@link #CHARGE_PUMP_DISABLE}.
         * <p><b>Created on:</b> <i>10:16:25 PM Mar 29, 2017</i>
         */
        SET_CHARGE_PUMP(asByte(0x8D)),

        /**
         * Enable "charge pump" during display on.
         * <p><b>Created on:</b> <i>6:38:04 AM Mar 26, 2017</i>
         */
        CHARGE_PUMP_ENABLE(asByte(0x14)),

        /**
         * Disable "charge pump".
         * <p><b>Created on:</b> <i>6:38:19 AM Mar 26, 2017</i>
         */
        CHARGE_PUMP_DISABLE(asByte(0x10)),

        /**
         * Set scan direction of the COM output to "remap" (see SSD1306 datasheet). Effectively, this command rotates
         * picture by 180 degrees.
         * <p><b>Created on:</b> <i>2:03:16 AM Mar 31, 2017</i>
         */
        SET_COM_OUTPUT_SCAN_DIRECTION_REMAPPED(asByte(0xC8)),

        /**
         * Set "COM" pins configuration. Configuration option is sent in next byte in command mode (see SDD1306
         * datasheet for more information).
         * <p><b>Created on:</b> <i>2:11:18 AM Mar 31, 2017</i>
         */
        SET_COM_PINS_CONFIGURATION(asByte(0xDA)),

        /**
         * Set contrast of the display. Contrast value ranging from 0 to 255 must be sent in next byte in command mode.
         * <p><b>Created on:</b> <i>2:15:16 AM Mar 31, 2017</i>
         */
        SET_CONTRAST(asByte(0x81)),

        /**
         * Set duration of the pre-charge period. Interval is measured in umber of DCLK.
         * <p><b>Created on:</b> <i>4:37:44 PM Mar 31, 2017</i>
         */
        SET_PRECHARGE_PERIOD(asByte(0xD9)),

        /**
         * Adjust V-comh regulator output.
         * <p><b>Created on:</b> <i>4:41:31 PM Mar 31, 2017</i>
         */
        SET_VCOMH_DESELECT_LEVEL(asByte(0xDB)),

        /**
         * Resume to RAM content display.
         * <p><b>Created on:</b> <i>4:44:24 PM Mar 31, 2017</i>
         */
        DISPLAY_ON_RESUME(asByte(0xA4)),

        /**
         * Set normal display mode : {@value 0} in display buffer indicates OFF pixel, {@value 1} - indicates ON one.
         * <p><b>Created on:</b> <i>4:46:53 PM Mar 31, 2017</i>
         */
        SET_NORMAL_DISPLAY(asByte(0xA6)),

        /**
         * Set inverse display mode : {@value 0} in display buffer indicates ON pixel, {@value 1} - indicates OFF one.
         * <p><b>Created on:</b> <i>4:47:34 PM Mar 31, 2017</i>
         */
        SET_INVERSE_DISPLAY(asByte(0xA7)),

        /**
         * Turn OLED panel display ON.
         * <p><b>Created on:</b> <i>4:50:14 PM Mar 31, 2017</i>
         */
        DISPLAY_ON(asByte(0xAF)),

        /**
         * Set column address range. Must be followed by two bytes in command mode : column start address and column end
         * address.
         * <p><b>Created on:</b> <i>6:07:53 AM Apr 1, 2017</i>
         */
        SET_COLUMN_ADDR(asByte(0x21)),

        /**
         * Set page address range. Must be followed by two bytes in command mode : page start address and page end
         * address.
         * <p><b>Created on:</b> <i>6:08:51 AM Apr 1, 2017</i>
         */
        SET_PAGE_ADDR(asByte(0x22)),

        /**
         * Configure horizontal left scrolling.
         * <p><b>Created on:</b> <i>3:56:37 AM Apr 4, 2017</i>
         */
        SCROLL_HORIZONTAL_LEFT(asByte(0x27)),

        /**
         * Configure horizontal right scrolling.
         * <p><b>Created on:</b> <i>4:01:31 AM Apr 4, 2017</i>
         */
        SCROLL_HORIZONTAL_RIGHT(asByte(0x26)),

        /**
         * Configure vertical and horizontal right scrolling (diagonal right).
         * <p><b>Created on:</b> <i>1:49:04 AM Apr 5, 2017</i>
         */
        SCROLL_DIAGONAL_RIGHT(asByte(0x29)),

        /**
         * Configure vertical and horizontal left scrolling (diagonal left).
         * <p><b>Created on:</b> <i>1:49:35 AM Apr 5, 2017</i>
         */
        SCROLL_DIAGONAL_LEFT(asByte(0x2A)),

        /**
         * Activate scrolling. Scrolling parameters should be configured before calling this command.
         * <p><b>Created on:</b> <i>4:04:17 AM Apr 4, 2017</i>
         */
        SCROLL_ACTIVATE(asByte(0x2F)),

        /**
         * Deactivate scrolling.
         * <p><b>Created on:</b> <i>4:04:19 AM Apr 4, 2017</i>
         */
        SCROLL_DEACTIVATE(asByte(0x2E)),

        /**
         * Set number of rows in top fixed area and rows in scroll area. See details in SSD1306 datasheet.
         * <p><b>Created on:</b> <i>10:05:10 AM Apr 5, 2017</i>
         */
        SET_VERTICAL_SCROLL_AREA(asByte(0xA3)),
        ;

    /**
     * Command code.
     * <p><b>Created on:</b> <i>8:01:01 PM Mar 27, 2017</i>
     */
    private final byte code;


    /**
     * Constructor for class : [pi_java_ssd1306] dburyak.pi.ssd1306.Command.<p>
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>8:01:07 PM Mar 27, 2017</i>
     * 
     * @param code
     *            command code
     */
    private Command(final byte code) {
        this.code = code;
    }

    /**
     * Get code of this command.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>8:01:20 PM Mar 27, 2017</i>
     * 
     * @return code of this command
     */
    public final byte code() {
        return this.code;
    }

    /**
     * Get string representation of the command.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> non-empty {@code result}
     * <br><b>Side-effects:</b> NONE
     * <br><b>Created on:</b> <i>9:54:16 PM Mar 29, 2017</i>
     * 
     * @see java.lang.Enum#toString()
     * @return string representation of this command
     */
    @SuppressWarnings({ "nls" })
    @Override
    public final String toString() {
        return notNull(new StringBuilder()
            .append("{").append(name())
            .append(",code=[").append(hex(code))
            .append("]}")
            .toString());
    }

}
