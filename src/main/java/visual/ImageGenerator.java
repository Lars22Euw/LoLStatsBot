package visual;

import com.merakianalytics.orianna.types.core.summoner.Summoner;
import discord4j.core.object.entity.MessageChannel;
import bot.WinData;
import util.*;

import java.io.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ImageGenerator {

    public static final int BG_SCALE = 8;
    public static final BufferedImage background = readImage("background.png", false);
    public static final BufferedImage mastery = readImage("mastery.png", false);
    public static final BufferedImage recently = readImage("recently.png", false);
    public static final int BACKGROUND_HEIGHT = (background == null) ? 280 : background.getHeight();
    public static final int BACKGROUND_WIDTH = (background == null) ? 498 : background.getWidth();

    public static final int CHAMPION_SQUARE_SIZE = 120;
    public static final double CHAMPION_SQUARE_SCALE = 0.24 * BG_SCALE;
    public static final int OUT_WIDTH = BACKGROUND_WIDTH * BG_SCALE;
    public static final int OUT_HEIGHT = BACKGROUND_HEIGHT * BG_SCALE;
    public static final int LINE_WIDTH = OUT_WIDTH / 500;

    private static double currentY;
    public static final Color GOLD = new Color(173, 136, 0);
    public static final Color GREEN = new Color(0, 60, 0);
    public static final Color RED = new Color(60, 0, 0);
    final Graphics2D g;
    final BufferedImage img;

    public ImageGenerator(String title) {
        int w = background.getWidth() * BG_SCALE;
        int h = background.getHeight() * BG_SCALE;
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g = img.createGraphics();
        draw(ImageGenerator.background, 0, 0, BG_SCALE);
        g.setColor(GOLD);
        g.setFont(new Font("Calibri", Font.PLAIN, (int) (12 * BG_SCALE * 2.4f)));
        g.drawString(title, (int) (OUT_WIDTH * 0.05), (int) (OUT_HEIGHT * 0.08) + g.getFont().getSize() / 2);
        g.setFont(new Font("Calibri", Font.PLAIN, 12 * BG_SCALE));
    }

    /**
     * fills an arrow on the graphics object
     *  @param x  double, relative position in image
     * @param y  double, relative position in image
     * @param dx double, relative offset in image
     * @param dy double, relative offset in image
     */
    void fillArrow(double x, double y, double dx, double dy) {
        g.setStroke(new BasicStroke(LINE_WIDTH));
        final var xStart = (int) (x * OUT_WIDTH);
        final var yStart = (int) (y * OUT_HEIGHT);
        final var xEnd = (int) ((x + dx) * OUT_WIDTH);
        final var yEnd = (int) ((y + dy) * OUT_HEIGHT);
        g.drawLine(xStart, yStart, xEnd, yEnd);

        // norm direction
        var len = vecLength(dx, dy);
        var dxN = dx / len;
        var dyN = dy / len;

        final var arrowHeadScaling = LINE_WIDTH * 3;
        // front
        int xf = xEnd + (int) (dxN * arrowHeadScaling);
        int yf = yEnd + (int) (dyN * arrowHeadScaling);

        // left
        int xl = xEnd + (int) (dyN * arrowHeadScaling);
        int yl = yEnd - (int) (dxN * arrowHeadScaling);

        // right
        int xr = xEnd - (int) (dyN * arrowHeadScaling);
        int yr = yEnd + (int) (dxN * arrowHeadScaling);

        g.fillPolygon(new int[]{xf, xl, xr},
                new int[]{yf, yl, yr}, 3);
    }

    private static double vecLength(double x, double y) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public boolean draw(BufferedImage img, double x, double y, double scale) {
        var at = new AffineTransform();
        at.translate(x, y);
        at.scale(scale, scale);
        return g.drawImage(img, at, null);
    }


    private static BufferedImage readImage(String s, boolean drawFallback) {
        final BufferedImage img;
        try {
            return ImageIO.read(new File("res/" + s));
        } catch (IIOException e) {
            U.log(System.err, "Image " + s + " not found in res/");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (drawFallback) {
                img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                var g = img.getGraphics();
                g.setColor(Color.white);
                g.drawString(s, 128, 128);
            } else {
                img = null;
            }
        }
        return img;
    }

    void doubleRect(double x, double y, double dx, double dy) {
        g.fillRect((int) (x * OUT_WIDTH), (int) (y * OUT_HEIGHT), (int) (dx * OUT_WIDTH), (int) (dy * OUT_HEIGHT));
    }

    <T> void doublePie(double x, double y, double r, SortedSet<WinData<T>> winData) {
        var winDataList = new ArrayList<>(winData);
        var ratio = winDataList.stream().map(WinData::getRatio).collect(Collectors.toList());
        final var totalGames = (double) U.mapSum(winDataList, WinData::getGames);
        U.log(totalGames);
        var portion = winDataList.stream().map(WinData::getGames).map(games -> games / totalGames).collect(Collectors.toList());
        var labels = winDataList.stream().map(WinData::getLabel).collect(Collectors.toList());
        doublePie(x, y, r, portion, ratio, labels);
    }

    void doubleBar(double x, double y, double width, double height, SortedSet<WinData<?>> winData) {
        var winDataList = new ArrayList<>(winData);
        var ratio = winDataList.stream().map(WinData::getRatio).collect(Collectors.toList());
        final var totalGames = (double) U.mapSum(winDataList, WinData::getGames);
        var portion = winDataList.stream().map(WinData::getGames).map(games -> games / totalGames).collect(Collectors.toList());
        var labels = winDataList.stream().map(WinData::getLabel).collect(Collectors.toList());
        var images = winDataList.stream().map(WinData::getImages).filter(Objects::nonNull).map(s -> readImage(s, true)).collect(Collectors.toList());
        if (images.isEmpty()) {
            doubleBar(x, y, width, height, portion, ratio, labels);
        } else {
            doubleBar(x, y, width, height, portion, ratio, images);
        }
    }

    /**
     * @param portions Portion of games played with the Key Property (e.g. 0.3 -> 30% der Spiele mit Braum)
     * @param ratio    Win-Ratio in the games played with the Key Property (e.g. 0.5 -> 50% der Spiele mit Braum gewonnen)
     */
    void doubleBar(double x, double y, double width, double height, List<Double> portions, List<Double> ratio, List<?> labels) {
        currentY = y;
        final var rectsStart = (int) (x * OUT_WIDTH);
        final double portionFactor = 1 - (portions.size() - 1) * 0.01;

        // portion refers to the portion of games played with regard to all games recorded: e.g. 30 Games with Braum, 100 total games -> 0.3
        U.forEach(portions, ratio, labels, (portion, ra, label) -> {
            final var portionHeight = portion * height * portionFactor;
            g.setColor(GREEN);
            final var greenWidth = (int) (width * ra * OUT_WIDTH);
            final var rectHeight = (int) (currentY * OUT_HEIGHT);
            g.fillRect(rectsStart, (int) (currentY * OUT_HEIGHT), greenWidth, (int) (portionHeight * OUT_HEIGHT));
            g.setColor(RED);
            g.fillRect(rectsStart + greenWidth, (int) (currentY * OUT_HEIGHT),
                    (int) (width * (1 - ra) * OUT_WIDTH), (int) (portionHeight * OUT_HEIGHT));
            g.setColor(GOLD);
            if (label instanceof BufferedImage) {
                AffineTransform atc = new AffineTransform();
                atc.translate(rectsStart, rectHeight + (portionHeight / 2) * OUT_HEIGHT - ((BufferedImage) label).getHeight() * BG_SCALE * 0.15);
                atc.scale(BG_SCALE * 0.3, BG_SCALE * 0.3);
                g.drawImage((BufferedImage) label, atc, null);
            } else {
                makeSmallText((String) label, x + 0.08 * width, currentY + portionHeight / 2 + 0.006);
            }
            currentY += portionHeight + 0.01;
        });
    }

    void doublePie(double x, double y, double r, List<Double> portions, List<Double> ratio, List<String> labels) {
        var startEnd = new UPair<>(270, 0);
        g.setColor(new Color(0, 100, 0));
        g.fillArc((int) ((x - r / 2) * OUT_WIDTH), (int) ((y - r / 2) * OUT_WIDTH), (int) (r * OUT_WIDTH), (int) (r * OUT_WIDTH),
                startEnd.first, 360);
        g.setStroke(new BasicStroke(20));
        U.log(labels);
        U.forEach(portions, ratio, (po, ra) -> {
            U.log(po, ra);
            g.setColor(new Color(100, 0, 0));
            var innerR = r * (Math.sqrt(ra) + ra) / 2.0;
            if (innerR > 0) {
                g.fillArc((int) ((x - innerR / 2) * OUT_WIDTH), (int) ((y - innerR / 2) * OUT_WIDTH), (int) (innerR * OUT_WIDTH), (int) (innerR * OUT_WIDTH),
                        startEnd.first, (int) (po * 360));
                g.setColor(Color.BLACK);
                g.drawArc((int) ((x - innerR / 2) * OUT_WIDTH), (int) ((y - innerR / 2) * OUT_WIDTH), (int) (innerR * OUT_WIDTH), (int) (innerR * OUT_WIDTH),
                        startEnd.first, (int) (po * 360));
            }
            startEnd.first = (startEnd.first + (int) (po * 360)) % 360;
        });
        startEnd.first = 0;
        g.setColor(Color.BLACK);
        g.drawOval((int) ((x - r / 2) * OUT_WIDTH), (int) ((y - r / 2) * OUT_WIDTH), (int) (r * OUT_WIDTH), (int) (r * OUT_WIDTH));
        U.forEach(portions, ratio, (po, ra) -> {
            double angle = Math.toRadians((720 - startEnd.first + 90) % 360);
            int pX = (int) ((Math.cos(angle) * r * OUT_WIDTH / 2) + x * OUT_WIDTH);
            int pY = (int) ((Math.sin(angle) * r * OUT_WIDTH / 2) + y * OUT_WIDTH);
            g.drawLine((int) (x * OUT_WIDTH), (int) (y * OUT_WIDTH), pX, pY);
            startEnd.first = startEnd.first + (int) (po * 360);
        });
    }

    void doubleRectBorder(double x, double y, double dx, double dy) {
        g.drawRect((int) (x * OUT_WIDTH), (int) (y * OUT_HEIGHT), (int) (dx * OUT_WIDTH), (int) (dy * OUT_HEIGHT));
    }

    void drawSummoner(String sum, double x, double y) {
        var summoner = Summoner.named(sum).get();
        var summIcon = summoner.getProfileIcon().getImage().get();
        draw(summIcon, x - 256, y + 32, CHAMPION_SQUARE_SCALE * 0.36 * 300/summIcon.getWidth());
        g.drawString(sum, (int) x, (int) (y + g.getFont().getSize() / 2 +  110 * CHAMPION_SQUARE_SCALE / 2));
    }


    void makeSmallText(String message, double x, double y) {
        g.setStroke(new BasicStroke(0));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() / 3f));
        g.drawString(message, (int) (OUT_WIDTH * x), (int) (OUT_HEIGHT * y));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 3f));
    }

    void makeMediumText(String message, double x, double y) {
        g.setStroke(new BasicStroke(0));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() / 2f));
        g.drawString(message, (int) (OUT_WIDTH * x), (int) (OUT_HEIGHT * y) + g.getFont().getSize() / 2);
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 2f));
    }

    void makeMessage(MessageChannel channel, String filename) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", output);
        } catch (IOException e) {
            channel.createMessage("Error when creating the image response").block();
            e.printStackTrace();
        }
        channel.createMessage((messageCreateSpec) ->
                messageCreateSpec.addFile(filename, new ByteArrayInputStream(output.toByteArray()))).block();
    }
}
