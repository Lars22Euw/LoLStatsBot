import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class RiotCommand extends Command {

    public RiotCommand(String argument, BiConsumer<Arguments, MessageChannel> query, Function<Message, Arguments> parse) {
        super(argument);
        func = funcGen(query, parse);
    }

    public RiotCommand(String argument, BiConsumer<Arguments, MessageChannel> query) {
        super(argument);
        func = funcGen(query, this::parseArguments);
    }

    private Consumer<Message> funcGen(BiConsumer<Arguments, MessageChannel> query, Function<Message, Arguments> parse){
        return m -> {
            System.out.println(m.getContent().orElseThrow());
            final var channel = m.getChannel().block();
            assert channel != null;
            Message wait = channel.createMessage("Hang on while I'm querying the Riot API.").block();
            assert wait != null;
            System.out.println(m.getContent().orElseThrow());
            Arguments arguments = parse.apply(m);
            System.out.println(m.getContent().orElseThrow() + arguments);
            if (arguments == null) {
                wait.delete("Outdated message as query terminated.").block();
                return;
            }
            System.out.println(m.getContent().orElseThrow());
            query.accept(arguments, channel);
            wait.delete("Outdated message as query terminated.").block();
        };
    }

    private Arguments parseArguments(Message message) {
        var msgText = message.getContent().orElseThrow();
        boolean image = false;
        DateTime startDate = MyMessage.getDateMinus(MyMessage.MONTHS_IN_THE_PAST, 1);
        List<Queue> queues = new ArrayList<>();
        List<Champion> champions = new ArrayList<>();
        List<Summoner> summoners = new ArrayList<>();
        int gamesTogether = 2;
        int games = 70;
        var tokens = msgText.split(" ");
        for (int index = 1; index < tokens.length; index++) {
            try {
                switch (tokens[index]) {
                    default: {
                        summoners.addAll(MyMessage.parseSummoners(tokens[index]));
                        break;
                    }
                    case "-t": { // with TIME
                        startDate = MyMessage.parseTime(tokens[++index]);
                        break;
                    }
                    case "-c": { // with CHAMPION
                        champions.addAll(MyMessage.parseChamps(tokens[++index]));
                        break;
                    }
                    case "-g": { // with GamesTogether
                        gamesTogether = Integer.parseInt(tokens[++index]);
                        break;
                    }
                    case "-n": { // with Games
                        games = Integer.parseInt(tokens[++index]);
                        break;
                    }
                    case "-q": { // with QUEUE
                        queues = MyMessage.parseQueues(tokens[++index]);
                        break;
                    }
                    case "-i": { // as image
                        image = true;
                        break;
                    }
                }
            } catch (InputError e) {
                System.out.println("End of caught error.");
                message.getChannel().block().createMessage(e.error).block();
                return null;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }
        }

        return new Arguments.Builder()
                .withQueues(queues)
                .withGamesTogether(gamesTogether)
                .withGames(games)
                .withSummoners(summoners)
                .withChampions(champions)
                .withStartTime(startDate)
                .isImage(image)
                .get();
    }

}
