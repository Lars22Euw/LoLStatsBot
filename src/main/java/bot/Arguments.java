package bot;

import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;
import util.U;

import java.util.List;

public class Arguments {
    List<Queue> queues;
    List<Champion> champions;
    List<Summoner> summoners;
    Summoner summoner;
    int gamesTogether;
    boolean image;
    DateTime startDate;
    int games;

    @Override
    public String toString() {
        return "bot.Arguments{" +
                "queues=" + U.mapAdd(queues, Enum::name) +
                ", champions=" + U.mapAdd(champions, Champion::getName) +
                ", summoners=" + U.mapAdd(summoners, Summoner::getName) +
                ", summoner=" + summoner.getName() +
                ", gamesTogether=" + gamesTogether +
                ", image=" + image +
                ", startDate=" + startDate.toString(Util.dtf) +
                ", games=" + games +
                '}';
    }

    public static class Builder {
        List<Queue> queues;
        List<Champion> champions;
        List<Summoner> summoners;
        Summoner summoner;
        int gamesTogether;
        boolean image;
        DateTime startDate;
        int games;

        public Builder() {}

        Builder withQueues(List<Queue> queues) {
            this.queues = queues;
            return this;
        }

        Builder withGamesTogether(int gamesTogether) {
            this.gamesTogether = gamesTogether;
            return this;
        }

        Builder withSummoner(Summoner summoner) {
            this.summoner = summoner;
            return this;
        }

        Builder withSummoners(List<Summoner> summoners) {
            this.summoners = summoners;
            this.summoner = summoners.isEmpty() ? null : summoners.get(0);
            return this;
        }

        public Arguments get() {
            Arguments arguments = new Arguments();
            arguments.summoner = summoner;
            arguments.summoners = summoners;
            arguments.queues = queues;
            arguments.gamesTogether = gamesTogether;
            arguments.image = image;
            arguments.startDate = startDate;
            arguments.champions = champions;
            arguments.games = games;
            return arguments;
        }

        public Builder isImage(boolean image) {
            this.image = image;
            return this;
        }

        public Builder withStartTime(DateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withChampions(List<Champion> champions) {
            this.champions = champions;
            return this;
        }

        public Builder withGames(int games) {
            this.games = games;
            return this;
        }
    }
}
