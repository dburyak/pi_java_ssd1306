package dburyak.pi.ssd1306.example;


import static dburyak.pi.ssd1306.Display.Dimensions.W128_H64;
import static dburyak.pi.ssd1306.Display.HorizontalDirection.LEFT;
import static dburyak.pi.ssd1306.Display.HorizontalDirection.RIGHT;
import static dburyak.pi.ssd1306.Display.OverlayType.FULL;
import static dburyak.pi.ssd1306.Display.OverlayType.OFF_PIXELS;
import static dburyak.pi.ssd1306.Display.OverlayType.ON_PIXELS;
import static dburyak.pi.ssd1306.Display.VerticalDirection.DOWN;
import static dburyak.pi.ssd1306.Display.VerticalDirection.UP;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;

import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import dburyak.pi.ssd1306.Display;
import dburyak.pi.ssd1306.Display.Position;
import dburyak.pi.ssd1306.Display.ScrollFrequency;
import dburyak.pi.ssd1306.SSD1306_I2C;


/**
 * Project : pi_java_ssd1306<p>
 * Demonstration code to show how to work with all {@link Display} functions.
 * <p><b>Created on:</b> <i>10:51:09 PM Apr 18, 2017</i>
 * 
 * @author <i>Dmytro Buryak &lt;dmytro.buryak@gmail.com&gt;</i>
 * @version 0.1
 */
public final class Demo {

    /**
     * This demo entry point.
     * <p><b>PRE-conditions:</b> NONE
     * <br><b>POST-conditions:</b> NONE
     * <br><b>Side-effects:</b> GPIO I/O activity
     * <br><b>Created on:</b> <i>10:51:09 PM Apr 18, 2017</i>
     * 
     * @param args
     *            not used
     * @throws UnsupportedBusNumberException
     *             if illegal I2C is used
     * @throws IOException
     *             if I/O exception during I2C I/O
     */
    @SuppressWarnings({ "nls", "null" })
    public static final void main(final String[] args) throws IOException, UnsupportedBusNumberException {
        final SSD1306_I2C i2cConn = SSD1306_I2C.newInstance(1, 0x3C);
        final Display display = new Display(W128_H64, i2cConn);

        final BufferedImage imgMix = new BufferedImage(40, 40, TYPE_BYTE_BINARY);
        final Graphics2D grMix = imgMix.createGraphics();
        grMix.setColor(BLACK);
        grMix.fillRect(0, 0, 40, 40);
        grMix.setColor(WHITE);
        grMix.drawLine(0, 0, 40, 40);
        grMix.drawLine(40, 0, 0, 40);
        imgMix.flush();

        final BufferedImage imgOver = new BufferedImage(20, 20, TYPE_BYTE_BINARY);
        final Graphics2D grOver = imgOver.createGraphics();
        grOver.setColor(BLACK);
        grOver.fillRect(0, 0, 20, 20);
        grOver.setColor(WHITE);
        grOver.drawLine(10, 0, 10, 20);
        grOver.drawLine(0, 10, 20, 10);
        imgOver.flush();

        display.begin();
        display
            .text("Hello", Position.of(0, 0))
            .sync()
            .sleep(Duration.ofSeconds(3))

            .text("World", Position.of(22, 22))
            .sync()
            .sleep(Duration.ofSeconds(3))

            .text("Demo", Position.of(40, 40), new Font("Helvetica", Font.PLAIN, 16))
            .sync()
            .sleep(Duration.ofSeconds(3))

            .image(imgMix, Position.of(0, 0), Position.of(127, 63), ON_PIXELS)
            .sync()
            .sleep(Duration.ofSeconds(3))

            .image(imgOver, Position.of(0, 0), Position.of(127, 63), OFF_PIXELS)
            .sync()
            .sleep(Duration.ofSeconds(3))

            .image(imgMix, Position.of(40, 40), Position.of(127, 63), FULL)
            .sync()
            .sleep(Duration.ofSeconds(3))

            .clear()
            .text("LOGO", Position.of(53, 25))
            .sync()

            // scroll all display area
            .dim(true)
            .scrollHorizontal(RIGHT, ScrollFrequency.FRAMES_3, 0, display.pages() - 1)
            .sleep(Duration.ofSeconds(7))
            .stopScroll()
            .sleep(Duration.ofSeconds(3))

            .dim(false)
            .scrollVertical(UP)
            .sleep(Duration.ofSeconds(7))
            .stopScroll()
            .sleep(Duration.ofSeconds(3))

            .dim(true)
            .clear()
            .text("OLED PI", Position.of(53, 25))
            .sync()
            .scrollDiagonal(LEFT, DOWN)
            .sleep(Duration.ofSeconds(7))
            .stopScroll()

            .contrast(0.01)
            .clear()
            .text("Some Text", Position.of(53, 25))
            .sync()
            .scrollDiagonal(RIGHT, ScrollFrequency.FRAMES_4, 0, 0, 3, 0, display.height())
            .sleep(Duration.ofSeconds(7))
            .stopScroll()
            .contrast(1.0D)
            .sleep(Duration.ofSeconds(3));


        display.graphics().setColor(WHITE);
        display.graphics().drawLine(0, 0, 40, 40);
        display
            .sync()
            .sleep(Duration.ofSeconds(5));
        display.graphics().setColor(WHITE);
        display.graphics().drawLine(40, 0, 0, 40);
        display
            .sync()
            .sleep(Duration.ofSeconds(5));

        display.stop();
    }

}
