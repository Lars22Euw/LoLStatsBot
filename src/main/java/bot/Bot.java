package bot;

import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import util.U;
import visual.ClashImageGenerator;
import visual.FarmImageGenerator;
import visual.ImageGenerator;
import visual.StalkImageGenerator;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Bot {

    private static final String PREFIX = ".";

    protected final DiscordClient client;
    private final Manager manager;

    private final List<Command> commands = List.of(
            new RiotCommand("m", this::matches),
            new RiotCommand("matches", this::matches),
            new RiotCommand("c", this::clash),
            new RiotCommand("clash", this::clash),
            new RiotCommand("s", this::stalk),
            new RiotCommand("stalk", this::stalk),
            new RiotCommand("f", this::farm),
            new RiotCommand("farm", this::farm),
            new Command("h", this::help),
            new Command("help", this::help));

    private void farm(Arguments arguments, MessageChannel channel) {
        try {
            var matchHistory = arguments.summoner.matchHistory()
                    .withQueues(arguments.queues)
                    .withChampions(arguments.champions)
                    .withEndIndex(arguments.games)
                    .get();
            List<ParticipantStats> mhStats = matchHistory.stream().map(m -> m.getParticipants().find(p -> p.getSummoner().getName()
                    .equalsIgnoreCase(arguments.summoner.getName())).getStats()).collect(Collectors.toList());
            StringBuilder sb = new StringBuilder();
            if (arguments.image) {
                FarmImageGenerator.farm(channel, U.zip(matchHistory, mhStats));
                return;
            }
            U.forEach(matchHistory, mhStats, (mh, st) -> {
                U.log(mh);
                final var queue = mh.getQueue();
                sb.append(mh.getCreationTime().toString(Util.dtf))
                        .append(" ")
                        .append(Util.asString(queue == null ? "" : queue.name(), 8))
                        .append(" ")
                        .append(Util.asString(mh.getParticipants().find(p -> p.getSummoner().getName()
                                .equalsIgnoreCase(arguments.summoner.getName())).getChampion().getName(), 14));

                final var creepScore = st.getCreepScore() + st.getNeutralMinionsKilled();
                final var cs = Util.asString(creepScore, 5);
                sb.append(" ")
                        .append(cs)
                        .append("\n");

            });
            System.out.println("Clash: " + sb.toString());
            channel.createMessage("```" + "\nCreep Scores:\n" + sb.toString() + "```").block();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stalk(Arguments arguments, MessageChannel c) {
        if (arguments.image) {
            var matches = findMatches(arguments);
            StalkImageGenerator.stalk(c, new StalkDataset(arguments.summoner, matches));
            return;
        }
        var resp = MyMessage.stalk(arguments);
        var title = Bot.buildTitle("Stalk for: ", List.of(arguments.summoner), arguments.queues, null, null);
        System.out.println(title + " ; " + resp);
        c.createMessage(title + "```" + resp + "```").block();
    }

    private List<Match> findMatches(Arguments arguments) {
        var historySize = Math.min(arguments.games, 200);

        if (arguments.queues.size() == 0) {
            return MatchHistory.forSummoner(arguments.summoner).withEndIndex(historySize).get();
        } else {
            return MatchHistory.forSummoner(arguments.summoner).withQueues(arguments.queues).withEndIndex(historySize).get();
        }
    }

    private void clash(Arguments arguments, MessageChannel channel) {
        String[] resp = new String[0];
        try {
            resp = new MyMessage(manager).clash(arguments);
        } catch (IllegalArgumentException e) {
            channel.createMessage(String.format("Summoner %s has no matches! Check the spelling.", e.getMessage())).block();
            return;
        }
        if (arguments.image) {
            ClashImageGenerator.clash(resp, channel);
        } else {
            StringBuilder sb = new StringBuilder();
            for (var line: resp) {
                sb.append(line).append("\n");
            }
            System.out.println("Clash: "+sb.toString());
            channel.createMessage("```" + "\nBans in order:\n" + sb.toString() + "```").block();
        }
    }

    private void matches(Arguments arguments, MessageChannel channel) throws NoSuchElementException {
        System.out.println("wtf. No games found");

        assert manager != null;
        SortedSet<Game> matches = manager.gamesWith(arguments);

        if (matches == null || matches.size() == 0) {
            System.out.println("wtf. No games found");
            channel.createMessage("No games found.\n").block();
            return;
        }

        System.out.println("wtf. No games found");
        var myMessage = new MyMessage(manager);
        myMessage.sb.append(MyMessage.stringOf(matches));
        var resp = myMessage.build();
        if (resp == null || resp.length == 0) {
            System.out.println("Empty msg");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (var line: resp) {
            sb.append(line).append("\n");
        }
        System.out.println("wtf. No games found");
        StringBuilder title = buildTitle("Matches for:", arguments);
        channel.createMessage(title+"```"+sb.toString()+"```").block();
    }

    static StringBuilder buildTitle(String start, Arguments arguments) {
        return buildTitle(start, arguments.summoners, arguments.queues, arguments.champions, arguments.startDate);
    }

    static StringBuilder buildTitle(String start, List<Summoner> summoners, List<Queue> queues, List<Champion> champions,
                                    DateTime startDate) {
        var title = new StringBuilder(start);

        if (summoners.size() > 0) {
            title.append("[summoners: ");
            title.append(summoners.get(0).getName());
            for (var s: summoners.subList(1, summoners.size())) {
                title.append(", ").append(s.getName());
            }
            title.append("]");
        }

        if (queues != null && queues.size() > 0) {
            title.append("[queues: ");
            for (var q: queues) title.append(", ").append(q.name());
            title.append("]");
        } else title.append("[all queues]");

        if (champions != null && champions.size() > 0) {
            title.append("[champs: ");
            for (var c: champions) title.append(", ").append(c.getName());
            title.append("]");
        } else title.append("[all champs]");

        if (startDate != null) {
            var form = DateTimeFormat.forPattern("dd.MM.yyyy");
            title.append("[start:").append(form.print(startDate)).append("]");
        }
        return title;
    }

    private void help(Message m) {
        var messageChannel = m.getChannel().block();
        if (m.getContent().get().toLowerCase().contains("matches")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUMS", "List of players or shorthands: Lars,FoxDrop"},
                        {"-c CHAMPS", "List of champions: lee,LeeSin,mundo,DrMundo"},
                        {"-q QUEUES", "List of queues: ARAM,clash,Custom,normal,Ranked"},
                        {"-t TIME", "months in the past until now: 2m"}};
                messageSpec.setEmbed(setEmbed(".m .matches",
                        "`.m SUMS -c CHAMPS -q QUEUES -t TIME`\n" +
                                "bot.Arguments are space separated, use `,` within args\n" +
                                "`.matches Lars -c Garen,Teemo`", fields));
            }).block();
        } else if (m.getContent().get().toLowerCase().contains("stalk")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUM", "bot.Player or shorthand: `Lars` or `FoxDrop`"},
                        {"-g MIN", "min games together : `2`"},
                        {"-q QUEUES", "Queues or shorthands: `SR,ranked,ARAM`"},
                        {"-n GAMES", "games looked up (max 200) : `70`"}
                };
                messageSpec.setEmbed(setEmbed(".s .stalk",
                        "`.s SUM [-g MIN] [-q QUEUES] [-n GAMES]`\n" +
                                "`.stalk Lars -g 2 -q CLASH -n 50`\n"+
                                "From last GAMES games with MIN games together in given queues\n" +
                                "`*` or `ALL` lists all queues\n" +
                                "`SR` = summoners rift: blind, clash, custom, normal, ranked", fields));
            }).block();
        } else if (m.getContent().get().toLowerCase().contains("clash")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUMS", "List of players or shorthands: Lars,FoxDrop"},
                        {"-i", "Add -i to receive and image based response"}};
                messageSpec.setEmbed(setEmbed(".c .clash",
                        "`.c SUMS`\n" +
                                "bot.Arguments are space separated, use `,` within args\n" +
                                "`.clash Lars,Thomas,TeemoMain`\n"+
                                "Based on recent games and champion mastery", fields));
            }).block();
        } else if (m.getContent().get().toLowerCase().contains("farm")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUMS", "List of players or shorthands: Lars,FoxDrop"}};
                messageSpec.setEmbed(setEmbed(".c .clash",
                        "`.f `\n" +
                                "bot.Arguments are space separated, use `,` within args\n" +
                                "`.clash Lars,Thomas,TeemoMain`\n"+
                                "Based on recent games and champion mastery", fields));
            }).block();
        } else {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {".clash SUMS [-i]", "Draft prediction for list of players."},
                        {".help COMM", "Help for specific command."},
                        {".matches SUMS [-c CHAMPS] [-q QUEUES]", "graphs matches per day with various filters."},
                        {".stalk SUM [-g MIN] [-q QUEUES] [-n GAMES]", "recent Summoners you played with."}
                };
                messageSpec.setEmbed(setEmbed("LoL Stats Commands",
                        "You can dm this bot.\n" +
                                "bot.Arguments are space separated, use `,` within args\n" +
                                "`.matches Lars -c Garen,Teemo`", fields));
            }).block();
        }
    }

    private Consumer<EmbedCreateSpec> setEmbed(String title, String description, String[][] fields) {
        return embedSpec -> {
            embedSpec.setTitle(title).setDescription(description)
                     .setUrl("https://github.com/Lars22Euw/LoLStatsBot");
            for (var f: fields) {
                embedSpec.addField(f[0], f[1], false);
            }
        };
    }

    public Bot(String riotAPI, String discordAPI) {
        manager = new Manager(riotAPI);
        client = new DiscordClientBuilder(discordAPI).build();
    }

    public static void main(String[] args) {
        var s = new Bot(args[0], args[1]);
        s.getCommands(s.client);

        /*s.client.getGuilds().blockFirst().createEmoji(spec -> {
            File fi = new File("Aatrox.png");
            try {
                byte[] fileContent = Files.readAllBytes(fi.toPath());
                spec.setImage(Image.ofRaw(fileContent, Image.Format.PNG));
            } catch (IOException e) {
                e.printStackTrace();
            }
            spec.setName("myaatrox");

        }).block();
        s.client.getGuilds().blockFirst().getEmojis().filter(ge -> ge.getName().equals("myaatrox")).blockFirst().delete().block();
        System.out.println("emoji");*/
        var guild = s.client.getGuildById(Snowflake.of(591616808835088404L)).block();
        var name = guild.getOwner().block().getDisplayName();
        System.out.println(name);
        s.client.login().block();
    }

    protected void getCommands(DiscordClient client) {
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    var msgText = event.getMessage().getContent().orElse(null);
                    if (msgText == null) return;
                    var parts = msgText.split(" ");
                    for (var c: commands) {
                        //System.out.println(parts[0]);
                        if ((PREFIX+c.argument).equalsIgnoreCase(parts[0])) {
                            c.func.accept(event.getMessage());
                            return;
                        }
                    }
                    if (parts[0].startsWith(PREFIX)) {
                        event.getMessage().getChannel().block().createMessage("Unknown command!").block();
                        for (var c : commands) {
                            if (c.argument.equals("help")) {
                                c.func.accept(event.getMessage());
                                break;
                            }
                        }
                    }
                });

/*         // ########
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith(PREFIX+entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();*/
    }
}
