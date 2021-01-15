import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import discord4j.core.object.entity.MessageChannel;
import util.UPair;

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

public class ImageResponseGenerator {

    public static final Color TEXT_COLOR = new Color(173, 136, 0);
    public static final int BACKGROUND_PNG_ZOOM = 8;
    public static final int BACKGROUND_WIDTH = 498;
    public static final int BACKGROUND_HEIGHT = 280;
    public static final int BACKGROUND_ZOOMED_WIDTH = BACKGROUND_WIDTH * BACKGROUND_PNG_ZOOM;
    public static final int BACKGROUND_ZOOMED_HEIGHT = BACKGROUND_HEIGHT * BACKGROUND_PNG_ZOOM;
    public static final int CHAMPION_SQUARE_SIZE = 120;
    public static BufferedImage mastery = readImage("mastery.png");
    public static BufferedImage recently = readImage("recently.png");

    public static void clash(String[] resp, MessageChannel channel) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BACKGROUND_PNG_ZOOM, BACKGROUND_HEIGHT * BACKGROUND_PNG_ZOOM, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(channel, g);
        g.setColor(TEXT_COLOR);
        makeTitle(g, "Clashbans:");

        var numberOfPlayers = resp.length / ClashTeam.ENTRIES_PER_PLAYER;
        var y = BACKGROUND_ZOOMED_HEIGHT * 0.19;
        var championSquareScale = 0.24 * BACKGROUND_PNG_ZOOM;
        var masteryScale = championSquareScale * 1.1;
        var recentlyScale = championSquareScale * 0.42;
        final var championListWidth = BACKGROUND_ZOOMED_WIDTH * 0.6 - CHAMPION_SQUARE_SIZE * championSquareScale;
        final var championListHeight = BACKGROUND_ZOOMED_HEIGHT * 0.8 - (CHAMPION_SQUARE_SIZE + 55) * championSquareScale;

        var maxRecentlyScore = getMaximum(resp, splitSelectAtSemicolon(2));
        var maxMasteryScore = getMaximum(resp, splitSelectAtSemicolon(3));
        var minScoreLog = Math.log(getMinimum(resp, splitSelectAtSemicolon(1)) + 1);
        var maxScoreLog = Math.log(getMaximum(resp, splitSelectAtSemicolon(1)) + 1);
        makeArrow(g, BACKGROUND_ZOOMED_WIDTH * 0.95, BACKGROUND_ZOOMED_HEIGHT * 0.08, -championListWidth, 0);

        for (int p = 0; p < numberOfPlayers; p++) {
            var x = BACKGROUND_ZOOMED_WIDTH * 0.05;
            var summonerName = resp[p * ClashTeam.ENTRIES_PER_PLAYER].split(":")[0];
            g.drawString(summonerName, (int) x, (int) (y + g.getFont().getSize() / 2 +  110 * championSquareScale / 2));
            for (int i = 0; i < ClashTeam.ENTRIES_PER_PLAYER; i++) {
                var championName = resp[p * ClashTeam.ENTRIES_PER_PLAYER + i].split(":")[1];
                var championScore = Math.log(splitSelectAtSemicolon(1).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) + 1);
                var scoreRatio = 1 - ((championScore - minScoreLog) / (maxScoreLog - minScoreLog));
                float scoreRecently = (float) (splitSelectAtSemicolon(2).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxRecentlyScore);
                float scoreMastery = (float) (splitSelectAtSemicolon(3).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxMasteryScore);

                scoreRecently *= scoreRecently * scoreRecently;
                var trueX = BACKGROUND_ZOOMED_WIDTH * 0.4 + scoreRatio * championListWidth;
                x = Math.max(trueX, x);
                if (x + (CHAMPION_SQUARE_SIZE + 10) * championSquareScale > BACKGROUND_ZOOMED_WIDTH) {
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

    private static void makeArrow(Graphics2D g, double x, double y, double dx, double dy) {
        g.setStroke(new BasicStroke(3));
        final var xStart = (int) x;
        final var yStart = (int) y;
        final var xEnd = (int) (x + dx);
        final var yEnd = (int) (y + dy);
        g.drawLine(xStart, yStart, xEnd, yEnd);
        var firstSideX = dy - dx;
        var firstSideY = -dx - dy;
        var secondSideX = -dy - dx;
        var secondSideY = dx - dy;
        final var offset = 2 * BACKGROUND_PNG_ZOOM;
        final var vecLength = vecLength(firstSideX, firstSideY);
        firstSideX *= offset / vecLength;
        firstSideY *= offset / vecLength;
        secondSideX *= offset / vecLength;
        secondSideY *= offset / vecLength;
        g.fillPolygon(new int[] {xEnd, (int) (xEnd + firstSideX), (int) (xEnd + secondSideX)},
                new int[] {yEnd, (int) (yEnd + firstSideY), (int) (yEnd + secondSideY)}, 3);
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
        Font newFont = g.getFont().deriveFont(g.getFont().getSize() * (float) BACKGROUND_PNG_ZOOM * 2.4f);
        g.setFont(newFont);
        g.drawString(s, (int) (BACKGROUND_ZOOMED_WIDTH * 0.05), (int) (BACKGROUND_ZOOMED_HEIGHT * 0.08) + g.getFont().getSize() / 2);
        g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 0.6f));
    }

    private static void setBackground(MessageChannel channel, Graphics2D g) {
        BufferedImage background = readImage(channel, "background.png");
        AffineTransform at = new AffineTransform();
        at.scale(BACKGROUND_PNG_ZOOM, BACKGROUND_PNG_ZOOM);
        g.drawImage(background, at, null);
    }

    private static BufferedImage readImage(String s) {
       return readImage(null, s);
    }

    private static BufferedImage readImage(MessageChannel channel, String s) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("res/" + s));
        } catch (IOException e) {
            if (channel != null)
                channel.createMessage("Error when loading icon at res/" + s).block();
            e.printStackTrace();
        }
        return image;
    }


    public static void farm(MessageChannel channel, List<UPair<Match, ParticipantStats>> data) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BACKGROUND_PNG_ZOOM, BACKGROUND_HEIGHT * BACKGROUND_PNG_ZOOM, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(channel, g);
        g.setColor(TEXT_COLOR);
        makeTitle(g, "CreepScore:");

        makeArrow(g,
                BACKGROUND_ZOOMED_WIDTH * 0.05,
                BACKGROUND_ZOOMED_HEIGHT * 0.9,
                BACKGROUND_ZOOMED_WIDTH * 0.9,
                0);
        makeArrow(g,
                BACKGROUND_ZOOMED_WIDTH * 0.05,
                BACKGROUND_ZOOMED_HEIGHT * 0.9,
                0,
                -BACKGROUND_ZOOMED_HEIGHT * 0.6);
        makeSmallText(g, "cs/min", BACKGROUND_ZOOMED_WIDTH * 0.03, BACKGROUND_ZOOMED_HEIGHT * 0.35);
        makeSmallText(g, "time", BACKGROUND_ZOOMED_WIDTH * 0.94, BACKGROUND_ZOOMED_HEIGHT * 0.97);
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
