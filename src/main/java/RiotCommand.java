import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import discord4j.core.object.entity.Message;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;


public class RiotCommand extends Command {

    public RiotCommand(String argument, Function<Arguments, String> query, Function<Message, Arguments> parse) {
        super(argument);
        func = funcGen(query, parse);
    }

    public RiotCommand(String argument, Function<Arguments, String> query) {
        super(argument);
        func = funcGen(query, this::parseArguments);
    }

    private Function<Message, Integer> funcGen(Function<Arguments, String> query, Function<Message, Arguments> parse){
        return m -> {
            Arguments arguments = parse.apply(m);
            Message wait = m.getChannel().block().createMessage("Hang on while I'm querying the Riot API.").block();

            String result = query.apply(arguments);
            var title = Bot.buildTitle("Stalk for: ", List.of(arguments.summoner), arguments.queues, null, null);

            System.out.println(title + " ; " + result);
            m.getChannel().block().createMessage(title + "```" + result + "```").block();
            wait.delete("Outdated message as query terminated.").block();
            return 0;
        };
    }

    private Arguments parseArguments(Message message) {
        var msgText = message.getContent().orElseThrow();
        var args = msgText.split(" ");
        if (args.length < 2) {
            message.getChannel().block().createMessage("```Expected at least a Summoner```").block();
            throw new IllegalArgumentException();
        }
        final Summoner sum;
        try {
            sum = MyMessage.parseSummoners(args[1]).get(0);
        } catch (InputError e) {
            message.getChannel().block().createMessage("```"+e.error+"```").block();
            throw new IllegalArgumentException();
        }
        final int gamesTogether;
        try {
            gamesTogether = (args.length < 3) ? 2 : Integer.parseInt(args[2]);
        } catch (Exception e) {
            message.getChannel().block().createMessage("```Tried to parse number but found: "+args[2]+"```").block();
            throw new IllegalArgumentException();
        }
        List<Queue> queues;
        try {
            if (args.length < 4) throw new InputError("Expected queues argument");
            queues = MyMessage.parseQueues(args[3]);
        } catch (InputError e) {
            message.getChannel().block().createMessage("```"+e.error+"```").block();
            throw new IllegalArgumentException();
        }
        return new Arguments.Builder()
                .withQueues(queues)
                .withGamesTogether(gamesTogether)
                .withSummoner(sum).get();
    }

}
