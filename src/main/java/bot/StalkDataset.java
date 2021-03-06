package bot;

import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StalkDataset {


    public Summoner summoner;
    // Need sorted Sorted List
    public SortedSet<WinData<Champion>> championsData = new TreeSet<>(
        Comparator.comparing(data -> - (data.getWins()*1.0 /data.getGames())));
    public SortedSet<WinData<Queue>> gamemodesData = new TreeSet<>(
            Comparator.comparing(data -> - (data.getWins()*1.0 /data.getGames())));
    public SortedSet<WinData<StalkRole>> rolesData = new TreeSet<>(
            Comparator.comparing(data -> - (data.getWins()*1.0 /data.getGames())));
    public SortedSet<WinData<String>> playersData = new TreeSet<>(
            Comparator.comparing(WinData::getKey));


    public static <T> Function<Map.Entry<T, List<Match>>, WinData<T>> winDataFunction(Summoner summoner) {
        return entry -> {
            T t = entry.getKey();
            int wins = 0;
            for (var match : entry.getValue()) {
                if (match.getBlueTeam().contains(summoner) ^ !match.getBlueTeam().isWinner()) {
                    wins++;
                }
            }
            int games = entry.getValue().size();
            return new WinData<>(t, wins, games);
        };
    }

    public StalkDataset(Summoner summoner, List<Match> matches) {
        this.summoner = summoner;
        gamemodesData.addAll(
                matches.stream().collect(Collectors.groupingBy(Match::getQueue)).entrySet()
                .stream().map(winDataFunction(summoner)).collect(Collectors.toList()));

        final var entries = matches.stream().collect(
                Collectors.groupingBy(m -> m.getParticipants()
                        .find(p -> p.getSummoner().getName().equals(summoner.getName())).getChampion())
        );
        final var entries1 = entries.entrySet();

        List<WinData<Champion>> list = new ArrayList<>();
        Function<Map.Entry<Champion, List<Match>>, WinData<Champion>> mapper = winDataFunction(summoner);
        for (Map.Entry<Champion, List<Match>> championListEntry : entries1) {
            WinData<Champion> championWinData = mapper.apply(championListEntry);
            list.add(championWinData);
        }
        championsData.addAll(list);
        // Currently games with Lane.None are removed from the dataset
        rolesData.addAll(
                matches.stream().collect(Collectors.groupingBy(m -> StalkRole.findRole(m, summoner))).entrySet()
                        .stream().filter(e -> !e.getKey().equals(StalkRole.NONE)).map(winDataFunction(summoner)).collect(Collectors.toList()));
    }
}
