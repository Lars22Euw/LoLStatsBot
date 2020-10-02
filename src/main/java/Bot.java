import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import discord4j.core.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.function.Consumer;

public class Bot {

    private static final String PREFIX = ".";

    private final DiscordClient client;
    private final Manager manager;

    private final List<Command> commands = List.of(
            new Command("m", this::matches),
            new Command("matches", this::matches),
            new Command("c", this::clash),
            new Command("clash", this::clash),
            new Command("s", this::stalk),
            new Command("stalk", this::stalk),
            new Command("h", this::help),
            new Command("help", this::help));

    private Integer stalk(Message message) {
        var msgText = message.getContent().get();
        var args = msgText.split(" ");
        if (args.length < 2) {
            message.getChannel().block().createMessage("```Expected at least a Summoner```").block();
            return -1;
        }
        final Summoner sum;
        try {
            sum = MyMessage.parseSummoners(args[1]).get(0);
        } catch (InputError e) {
            message.getChannel().block().createMessage("```"+e.error+"```").block();
            return -1;
        }
        final int gamesTogether;
        try {
            gamesTogether = (args.length < 3) ? 2 : Integer.parseInt(args[2]);
        } catch (Exception e) {
            message.getChannel().block().createMessage("```Tried to parse number but found: "+args[2]+"```").block();
            return -1;
        }
        List<Queue> queues;
        try {
            if (args.length < 4) throw new InputError("Expected queues argument");
            queues = MyMessage.parseQueues(args[3]);
        } catch (InputError e) {
            message.getChannel().block().createMessage("```"+e.error+"```").block();
            return -1;
        }
        var resp = MyMessage.stalk(sum, gamesTogether, queues);
        StringBuilder sb = new StringBuilder();
        sb.append(resp);
        System.out.println("Stalk: "+sb.toString());
        message.getChannel().block().createMessage("```" + sb.toString() + "```").block();
        return 0;
    }


    private Integer clash(Message message) {
        var msgText = message.getContent().get();
        var resp = messageClash(msgText);
        StringBuilder sb = new StringBuilder();
        for (var line: resp) {
            //System.out.println("some " + line);
            sb.append(line).append("\n");
        }
        System.out.println("Clash: "+sb.toString());
        message.getChannel().block().createMessage("```" + "\nBans in order:\n" + sb.toString() + "```").block();
        return 0;
    }

    private String[] messageClash(String input) {
        if (manager == null) System.out.println("manager is null");
        return new MyMessage(manager).clash(input);
    }

    private Integer matches(Message message) {
        var resp = message(message);
        if (resp == null || resp.length == 0) {
            System.out.println("Empty msg");
        }
        StringBuilder sb = new StringBuilder();
        for (var line: resp) {
            if (sb.length() > 1900) {
                message.getChannel().block().createMessage("LoL matches per day:" +"```"+sb.toString()+"```").block();
                sb = new StringBuilder();
            }
            sb.append(line).append("\n");
        }
        message.getChannel().block().createMessage("```"+sb.toString()+"```").block();
        return 0;
    }

    private Integer help(Message m) {
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
                                "Arguments are space separated, use `,` within args\n" +
                                "`.matches Lars -c Garen,Teemo`", fields));
            }).block();
        } else if (m.getContent().get().toLowerCase().contains("stalk")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUM", "Player or shorthand: `Lars` or `FoxDrop`"},
                        {"MIN", "min games together : `2`"},
                        {"QUEUES", "Queues or shorthands: `SR,ranked,ARAM`"}
                };
                messageSpec.setEmbed(setEmbed(".s .stalk",
                        "`.s SUM MIN QUEUES`\n" +
                                "`.sum Lars 2 CLASH`\n"+
                                "From last 70 games with MIN games together in given queues\n" +
                                "SR = summoners rift: blind, clash, custom, normal, ranked", fields));
            }).block();
        } else if (m.getContent().get().toLowerCase().contains("clash")) {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {"SUMS", "List of players or shorthands: Lars,FoxDrop"}};
                messageSpec.setEmbed(setEmbed(".c .clash",
                        "`.c SUMS`\n" +
                                "Arguments are space separated, use `,` within args\n" +
                                "`.clash Lars,Thomas,TeemoMain`\n"+
                                "Based on recent games and champion mastery", fields));
            }).block();
        } else {
            messageChannel.createMessage(messageSpec -> {
                final String[][] fields = {
                        {".clash SUMS", "Draft prediction for list of players."},
                        {".help COMM", "Help for specific command."},
                        {".matches SUMS -c CHAMPS -q QUEUES", "graphs matches per day with various filters."},
                        {".stalk SUM MIN QUEUES", "recent Summoners you played with."}
                };
                messageSpec.setEmbed(setEmbed("LoL Stats Commands",
                        "You can dm this bot.\n" +
                                "Arguments are space separated, use `,` within args\n" +
                                "`.matches Lars -c Garen,Teemo`", fields));
            }).block();
        }


        return 0;
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

    Bot(String riotAPI, String discordAPI) {
        manager = new Manager(riotAPI);
        client = new DiscordClientBuilder(discordAPI).build();
    }

    private String[] message(Message message) {
        if (manager == null) System.out.println("manager is null");
        var msgText = message.getContent().orElseThrow();
        return new MyMessage(msgText, manager).build();
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
        var guild = s.client.getGuildById(Snowflake.of(591616808835088404l)).block();
        var name = guild.getOwner().block().getDisplayName();
        System.out.println(name);
        s.client.login().block();
    }

    private void getCommands(DiscordClient client) {
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    var msgText = event.getMessage().getContent().orElse(null);
                    if (msgText == null) return;
                    var parts = msgText.split(" ");
                    for (var c: commands) {
                        //System.out.println(parts[0]);
                        if ((PREFIX+c.argument).equalsIgnoreCase(parts[0])) {
                            c.func.apply(event.getMessage());
                            break;
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
