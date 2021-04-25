package visual;

import bot.ClashTeam;
import bot.StalkRole;
import bot.WinData;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import discord4j.core.object.entity.MessageChannel;
import org.apache.commons.lang3.tuple.Pair;
import util.U;
import util.UPair;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ImageGenerator {

    public static final int BG_SCALE = 8;
    public static final BufferedImage background = readImage("background.png");
    public static final BufferedImage mastery = readImage("mastery.png");
    public static final BufferedImage recently = readImage("recently.png");
    public static final int BACKGROUND_HEIGHT = (background == null) ? 280 : background.getHeight();
    public static final int BACKGROUND_WIDTH = (background == null) ? 498 : background.getWidth();
    public static final int OUT_WIDTH = BACKGROUND_WIDTH * BG_SCALE;
    public static final int OUT_HEIGHT = BACKGROUND_HEIGHT * BG_SCALE;
    public static final int CHAMPION_SQUARE_SIZE = 120;
    public static final int LINE_WIDTH = OUT_WIDTH / 500;
    public static final Color GOLD = new Color(173, 136, 0);
    public static final Color GREEN = new Color(0, 60, 0);
    public static final Color RED = new Color(60, 0, 0);
    private static double currentY;

    static BufferedImage createScaledBufferedImage(BufferedImage background, int scale) {
        int w = background.getWidth() * scale;
        int h = background.getHeight() * scale;
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }


    /**
     * fills an arrow on the graphics object
     *
     * @param g  graphic object
     * @param x  double, relative position in image
     * @param y  double, relative position in image
     * @param dx double, relative offset in image
     * @param dy double, relative offset in image
     */
    static void fillArrow(Graphics2D g, double x, double y, double dx, double dy) {
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

    static void drawChampionWithReasons(MessageChannel channel, Graphics2D g, double x, double y,
                                        double championSquareScale, double masteryScale, double recentlyScale,
                                        String championName, float scoreRecently, float scoreMastery) {
        BufferedImage champion = readImage(channel, championName + ".png");
        AffineTransform atc = new AffineTransform();
        atc.translate(x, y);
        atc.scale(championSquareScale, championSquareScale);
        g.drawImage(champion, atc, null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreMastery));
        AffineTransform atm = new AffineTransform();
        atm.translate(x + CHAMPION_SQUARE_SIZE * 7.0 / 24 * championSquareScale - 80 * masteryScale / 2, y + (CHAMPION_SQUARE_SIZE + 5) * championSquareScale);
        atm.scale(masteryScale, masteryScale);
        g.drawImage(mastery, atm, null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreRecently));
        AffineTransform atr = new AffineTransform();
        atr.translate(x + CHAMPION_SQUARE_SIZE * 17.0 / 24 * championSquareScale - 100 * recentlyScale / 2, y + (CHAMPION_SQUARE_SIZE + 5) * championSquareScale);
        atr.scale(recentlyScale, recentlyScale);
        g.drawImage(recently, atr, null);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    static Function<String, Double> splitSelectAtSemicolon(int i) {
        return s -> Double.parseDouble(s.split(";")[i]);
    }


    // ToDo: prettify
    static Double getMinimum(String[] resp, Function<String, Double> extractValue) {
        return getExtremum(true, resp, extractValue);
    }

    static Double getMaximum(String[] resp, Function<String, Double> extractValue) {
        return getExtremum(false, resp, extractValue);
    }

    private static Double getExtremum(boolean isMinimum, String[] resp, Function<String, Double> extractValue) {
        var result = isMinimum ? Double.MAX_VALUE : Double.MIN_VALUE;
        for (var entry : resp) {
            var score = extractValue.apply(entry);
            if (isMinimum ? (score < result) : (score > result)) {
                result = score;
            }
        }
        return result;
    }

    static void makeTitle(Graphics2D g, String s) {
        g.setFont(new Font("Calibri", Font.PLAIN, 12));
        Font newFont = g.getFont().deriveFont(g.getFont().getSize() * (float) BG_SCALE * 2.4f);
        g.setFont(newFont);
        g.drawString(s, (int) (OUT_WIDTH * 0.05), (int) (OUT_HEIGHT * 0.08) + g.getFont().getSize() / 2);
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 0.6f));
    }

    private static BufferedImage readImage(String s) {
        return readImage(null, s);
    }

    private static BufferedImage readImage(MessageChannel channel, String s) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("res/" + s));
        } catch (IIOException e) {
            U.log(System.err, "Image " + s + " not found in res/");
            return null;
        } catch (IOException e) {
            if (channel != null)
                channel.createMessage("Error when loading icon at res/" + s).block();
            e.printStackTrace();
        }
        return image;
    }

    static void setBackground(Graphics2D g, BufferedImage image) {
        AffineTransform at = new AffineTransform();
        at.scale(BG_SCALE, BG_SCALE);
        g.drawImage(image, at, null);
    }

    static void doubleRect(Graphics2D g, double x, double y, double dx, double dy) {
        g.fillRect((int) (x * OUT_WIDTH), (int) (y * OUT_HEIGHT), (int) (dx * OUT_WIDTH), (int) (dy * OUT_HEIGHT));
    }

    static <T> void doublePie(Graphics2D g, double x, double y, double r, SortedSet<WinData<T>> winData) {
        var winDataList = new ArrayList<>(winData);
        var ratio = winDataList.stream().map(WinData::getRatio).collect(Collectors.toList());
        final var totalGames = (double) U.mapSum(winDataList, WinData::getGames);
        U.log(totalGames);
        var portion = winDataList.stream().map(WinData::getGames).map(games -> games / totalGames).collect(Collectors.toList());
        var labels = winDataList.stream().map(WinData::getLabel).collect(Collectors.toList());
        doublePie(g, x, y, r, portion, ratio, labels);
    }

    static void doubleBar(Graphics2D g, double x, double y, double width, double height, SortedSet<WinData<?>> winData) {
        var winDataList = new ArrayList<>(winData);
        var ratio = winDataList.stream().map(WinData::getRatio).collect(Collectors.toList());
        final var totalGames = (double) U.mapSum(winDataList, WinData::getGames);
        var portion = winDataList.stream().map(WinData::getGames).map(games -> games / totalGames).collect(Collectors.toList());
        var labels = winDataList.stream().map(WinData::getLabel).collect(Collectors.toList());
        var images = winDataList.stream().map(WinData::getImages).filter(Objects::nonNull).map(ImageGenerator::readImage).collect(Collectors.toList());
        if (images.isEmpty()) {
            doubleBar(g, x, y, width, height, portion, ratio, labels);
        } else {
            doubleBar(g, x, y, width, height, portion, ratio, images);
        }
    }

    /**
     * @param portions Portion of games played with the Key Property (e.g. 0.3 -> 30% der Spiele mit Braum)
     * @param ratio    Win-Ratio in the games played with the Key Property (e.g. 0.5 -> 50% der Spiele mit Braum gewonnen)
     */
    static void doubleBar(Graphics2D g, double x, double y, double width, double height, List<Double> portions, List<Double> ratio, List<?> labels) {
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
                makeSmallText(g, (String) label, x + 0.08 * width, currentY + portionHeight / 2 + 0.006);
            }
            currentY += portionHeight + 0.01;
        });
    }

    static void doublePie(Graphics2D g, double x, double y, double r, List<Double> portions, List<Double> ratio, List<String> labels) {
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

    static void doubleRectBorder(Graphics2D g, double x, double y, double dx, double dy) {
        g.drawRect((int) (x * OUT_WIDTH), (int) (y * OUT_HEIGHT), (int) (dx * OUT_WIDTH), (int) (dy * OUT_HEIGHT));
    }


    static void makeSmallText(Graphics2D g, String message, double x, double y) {
        g.setStroke(new BasicStroke(0));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() / 3f));
        g.drawString(message, (int) (OUT_WIDTH * x), (int) (OUT_HEIGHT * y));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 3f));
    }


    static void makeMediumText(Graphics2D g, String message, double x, double y) {
        g.setStroke(new BasicStroke(0));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() / 2f));
        g.drawString(message, (int) (OUT_WIDTH * x), (int) (OUT_HEIGHT * y) + g.getFont().getSize() / 2);
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 2f));
    }

    static void makeMessage(MessageChannel channel, BufferedImage img, String s) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", output);
        } catch (IOException e) {
            channel.createMessage("Error when creating the image response").block();
            e.printStackTrace();
        }
        channel.createMessage((messageCreateSpec) ->
                messageCreateSpec.addFile(s, new ByteArrayInputStream(output.toByteArray()))).block();
    }
}
