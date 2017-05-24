package dburyak.pi.ssd1306;


/**
 * Project : pi_java_ssd1306<p>
 * SSD1306 device that can be written to.
 * <p><b>Created on:</b> <i>7:09:45 PM Mar 27, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
public interface SSD1306Connection {

    /**
     * Send command to device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> UNKNOWN
     * <br><b>Created on:</b> <i>7:21:54 PM Mar 27, 2017</i>
     * 
     * @param cmd
     *            command to be sent
     */
    default public void command(final Command cmd) {
        command(cmd.code());
    }

    /**
     * Send single byte in command mode. Usually is used to send parameter for previously sent command.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> UNKNOWN
     * <br><b>Created on:</b> <i>10:24:48 PM Mar 29, 2017</i>
     * 
     * @param value
     *            byte value to be sent
     */
    public void command(final byte value);

    /**
     * Write single byte of data to device.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> UNKNOWN
     * <br><b>Created on:</b> <i>7:22:28 PM Mar 27, 2017</i>
     * 
     * @param singleByte
     *            single byte to be written
     */
    public void data(final byte singleByte);

    /**
     * Write buffer of data to device.
     * <p><b>PRE-conditions:</b> non-null {@code buffer}
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> UNKNOWN
     * <br><b>Created on:</b> <i>7:24:02 PM Mar 27, 2017</i>
     * 
     * @param buffer
     *            data buffer to be written
     */
    public void data(final byte[] buffer);

}
