import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.List;

public class Arguments {
    List<Queue> queues;
    Summoner summoner;
    List<Summoner> summoners;
    int gamesTogether;

    public static class Builder {
        List<Queue> queues;
        Summoner summoner;
        List<Summoner> summoners;
        int gamesTogether;

        public Builder() {

        }

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
            return this;
        }

        public Arguments get() {
            Arguments arguments = new Arguments();
            arguments.summoner = summoner;
            arguments.summoners = summoners;
            arguments.queues = queues;
            arguments.gamesTogether = gamesTogether;
            return arguments;
        }
    }
}
