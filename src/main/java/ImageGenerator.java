import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import discord4j.core.object.entity.MessageChannel;
import util.U;
import util.UPair;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.util.List;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class ImageGenerator {

    public static final Color TEXT_COLOR = new Color(173, 136, 0);
    public static final int BG_SCALE = 4;
    public static final BufferedImage background = readImage("background.png");
    public static final BufferedImage mastery = readImage("mastery.png");
    public static final BufferedImage recently = readImage("recently.png");
    public static final int BACKGROUND_HEIGHT = (background == null) ? 280 : background.getHeight();
    public static final int BACKGROUND_WIDTH = (background == null) ? 498 : background.getWidth();
    public static final int OUT_WIDTH = BACKGROUND_WIDTH * BG_SCALE;
    public static final int OUT_HEIGHT = BACKGROUND_HEIGHT * BG_SCALE;
    public static final int CHAMPION_SQUARE_SIZE = 120;

    static BufferedImage createScaledBufferedImage(BufferedImage background, int scale) {
        int w = background.getWidth() * scale;
        int h = background.getHeight() * scale;
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    public static void clash(String[] resp, MessageChannel channel) {
        var img = createScaledBufferedImage(mastery, BG_SCALE);
        var g = img.createGraphics();

        setBackground(g, background);
        g.setColor(TEXT_COLOR);
        makeTitle(g, "Clashbans:");

        var numberOfPlayers = resp.length / ClashTeam.ENTRIES_PER_PLAYER;
        var y = OUT_HEIGHT * 0.19;
        var championSquareScale = 0.24 * BG_SCALE;
        var masteryScale = championSquareScale * 1.1;
        var recentlyScale = championSquareScale * 0.42;
        final var championListWidth = OUT_WIDTH * 0.6 - CHAMPION_SQUARE_SIZE * championSquareScale;
        final var championListHeight = OUT_HEIGHT * 0.8 - (CHAMPION_SQUARE_SIZE + 55) * championSquareScale;

        var maxRecentlyScore = getMaximum(resp, splitSelectAtSemicolon(2));
        var maxMasteryScore = getMaximum(resp, splitSelectAtSemicolon(3));
        var minScoreLog = Math.log(getMinimum(resp, splitSelectAtSemicolon(1)) + 1);
        var maxScoreLog = Math.log(getMaximum(resp, splitSelectAtSemicolon(1)) + 1);
        fillArrow(g, 0.95, 0.08, -championListWidth, 0); // TODO: adapt champ list

        for (int p = 0; p < numberOfPlayers; p++) {
            var x = OUT_WIDTH * 0.05;
            var summonerName = resp[p * ClashTeam.ENTRIES_PER_PLAYER].split(":")[0];
            g.drawString(summonerName, (int) x, (int) (y + g.getFont().getSize() / 2 +  110 * championSquareScale / 2));
            for (int i = 0; i < ClashTeam.ENTRIES_PER_PLAYER; i++) {
                var championName = resp[p * ClashTeam.ENTRIES_PER_PLAYER + i].split(":")[1];
                var championScore = Math.log(splitSelectAtSemicolon(1).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) + 1);
                var scoreRatio = 1 - ((championScore - minScoreLog) / (maxScoreLog - minScoreLog));
                float scoreRecently = (float) (splitSelectAtSemicolon(2).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxRecentlyScore);
                float scoreMastery = (float) (splitSelectAtSemicolon(3).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxMasteryScore);

                scoreRecently *= scoreRecently * scoreRecently;
                var trueX = OUT_WIDTH * 0.4 + scoreRatio * championListWidth;
                x = Math.max(trueX, x);
                if (x + (CHAMPION_SQUARE_SIZE + 10) * championSquareScale > OUT_WIDTH) {
                    break;
                }
                drawChampionWithReasons(channel, g, x, y,
                        championSquareScale, masteryScale, recentlyScale,
                        championName, scoreRecently, scoreMastery);
                x += CHAMPION_SQUARE_SIZE * championSquareScale;
            }
            y += championListHeight / 4.0;
        }

        makeMessage(channel, img, "clash.png");

    }

    /**
     * fills an arrow on the graphics object
     * @param g graphic object
     * @param x double, relative position in image
     * @param y double, relative position in image
     * @param dx double, relative offset in image
     * @param dy double, relative offset in image
     */
    private static void fillArrow(Graphics2D g, double x, double y, double dx, double dy) {
        U.log("Line relative: ("+x+","+y+") to ("+(x+dx)+","+(y+dy)+")");

        final int width = 5; // TODO: make width global relative to img
        g.setStroke(new BasicStroke(width));
        final var xStart = (int) (x * OUT_WIDTH);
        final var yStart = (int) (y * OUT_HEIGHT);
        final var xEnd = (int) ((x + dx) * OUT_WIDTH);
        final var yEnd = (int) ((y + dy) * OUT_HEIGHT);
        U.log("Line: ("+xStart+","+yStart+") to ("+xEnd+","+yEnd+")");
        g.drawLine(xStart, yStart, xEnd, yEnd);

        //norm direction
        var len = vecLength(dx, dy);
        var dxN = dx/len;
        var dyN = dy/len;

        //front
        int xf = xEnd + (int) (dxN * width * 2);
        int yf = yEnd + (int) (dyN * width * 2);

        //left
        int xl = xEnd + (int) (dyN * width * 2);
        int yl = yEnd - (int) (dxN * width * 2);

        //right
        int xr = xEnd - (int) (dyN * width * 2);
        int yr = yEnd + (int) (dxN * width * 2);

        U.log("Triangle: ("+xf+","+yf+") to ("+xl+","+yl+") to ("+xr+","+yr+")");

        g.fillPolygon(new int[] {xf, xl, xr},
                      new int[] {yf, yl, yr}, 3);
    }

    private static double vecLength(double x, double y) {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    private static void drawChampionWithReasons(MessageChannel channel, Graphics2D g, double x, double y,
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

    private static Function<String, Double> splitSelectAtSemicolon(int i) {
        return s -> Double.parseDouble(s.split(";")[i]);
    }

    private static Double getMinimum(String[] resp, Function<String, Double> extractValue) {
        return getExtremum(true, resp, extractValue);
    }

    private static Double getMaximum(String[] resp, Function<String, Double> extractValue) {
        return getExtremum(false, resp, extractValue);
    }

    private static Double getExtremum(boolean isMinimum, String[] resp, Function<String, Double> extractValue) {
        var result = isMinimum ? Double.MAX_VALUE : Double.MIN_VALUE;
        for (var entry : resp) {
            var score = extractValue.apply(entry);
            if (isMinimum ? (score < result) : (score > result) ) {
                result = score;
            }
        }
        return result;
    }

    private static void makeTitle(Graphics2D g, String s) {
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
            U.log(System.err, "Image "+s+" not found in res/");
            return null;
        } catch (IOException e) {
            if (channel != null)
                channel.createMessage("Error when loading icon at res/" + s).block();
            e.printStackTrace();
        }
        return image;
    }

    private static void setBackground(Graphics2D g, BufferedImage image) {
        AffineTransform at = new AffineTransform();
        at.scale(BG_SCALE, BG_SCALE);
        g.drawImage(image, at, null);
    }

    public static void farm(MessageChannel channel, List<UPair<Match, ParticipantStats>> data) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BG_SCALE, BACKGROUND_HEIGHT * BG_SCALE, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(g, background);
        g.setColor(TEXT_COLOR);
        makeTitle(g, "CreepScore:");

        fillArrow(g, 0.05, 0.9, 0.9, 0); // >
        fillArrow(g, 0.05, 0.9, 0, -0.6); // ^
        makeSmallText(g, "cs/min", OUT_WIDTH * 0.03, OUT_HEIGHT * 0.35);
        makeSmallText(g, "time", OUT_WIDTH * 0.94, OUT_HEIGHT * 0.97);
        makeMessage(channel, img, "farm.png");

    }

    private static void makeSmallText(Graphics2D g, String message, double x, double y) {
        g.setStroke(new BasicStroke(0));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() / 3f));
        g.drawString(message, (int) (x), (int) (y));
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 3f));
    }

    private static void makeMessage(MessageChannel channel, BufferedImage img, String s) {
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
