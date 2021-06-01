package visual;

import bot.StalkDataset;
import bot.WinData;
import discord4j.core.object.entity.MessageChannel;
import util.U;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

public class StalkImageGenerator extends ImageGenerator {

    public StalkImageGenerator(MessageChannel channel, StalkDataset data) {
        super(data.summoner.getName());
        final var tableY = 0.26;
        final var tableHeight = 0.62;


        final var subHeadlineY = 0.2;
        final var firstColumnX = 0.02;
        final var tableWidth = 0.3;
        final var tableGap = 0.03;
        List<Object> datasets = new ArrayList<>(List.of(data.rolesData, data.championsData, data.gamemodesData));
        U.enumerateForEach(
                List.of("Winrate by Role", "Winrate by Champion", "Winrate by Gamemode"), datasets,
                (i, headline, dataset) -> {
            makeMediumText(headline, firstColumnX + i * (tableWidth + tableGap), subHeadlineY);
            doubleBar(firstColumnX + i * (tableWidth + tableGap), tableY, tableWidth, tableHeight, (SortedSet<WinData<?>>) dataset);
        });

        makeMessage(channel, "farm.png");
    }
}
