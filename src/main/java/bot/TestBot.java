package bot;

import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;

public class TestBot extends Bot {

    public static final String TEST_CALL = ".stalk Thomas -i -n 10";

    public static void main(String[] args) {
        var s = new TestBot(args[0], args[1]);
        s.getCommands(s.client);
        var guild = s.client.getGuildById(Snowflake.of(591616808835088404L)).block();
        var name = guild.getOwner().block().getDisplayName();
        System.out.println(name);
        new Thread(() -> {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testMethod(s);
        }).start();
        s.client.login().block();
    }

    TestBot(String riotAPI, String discordAPI) {
        super(riotAPI, discordAPI);
    }

    private static void testMethod(TestBot s) {
        var channel = s.client.getChannelById(Snowflake.of(598559453398171679L)).block();
        ((MessageChannel) channel).createMessage(TEST_CALL).block();
    }
}
