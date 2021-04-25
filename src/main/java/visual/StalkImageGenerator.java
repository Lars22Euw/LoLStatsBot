package visual;

import bot.StalkDataset;
import bot.StalkRole;
import bot.WinData;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import discord4j.core.object.entity.MessageChannel;
import util.U;
import util.UPair;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.SortedSet;

public class StalkImageGenerator extends ImageGenerator {

    public static void stalk(MessageChannel channel, StalkDataset data) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BG_SCALE, BACKGROUND_HEIGHT * BG_SCALE, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(g, background);
        g.setColor(GOLD);
        final var tableY = 0.26;
        final var tableHeight = 0.62;

        makeTitle(g, data.summoner.getName());

        final var subHeadlineY = 0.2;
        final var firstColumnX = 0.02;
        final var tableWidth = 0.3;
        final var tableGap = 0.03;
        List<Object> datasets = new ArrayList<>(List.of(data.rolesData, data.championsData, data.gamemodesData));
        U.enumerateForEach(
                List.of("Winrate by Role", "Winrate by Champion", "Winrate by Gamemode"), datasets,
                (i, headline, dataset) -> {
            makeMediumText(g, headline, firstColumnX + i * (tableWidth + tableGap), subHeadlineY);
            doubleBar(g, firstColumnX + i * (tableWidth + tableGap), tableY, tableWidth, tableHeight, (SortedSet<WinData<?>>) dataset);
        });

        makeMessage(channel, img, "farm.png");
    }
}
