import discord4j.core.object.entity.Message;

import java.util.function.Consumer;
import java.util.function.Function;

public class Command {
    String argument;
    Consumer<Message> func;

    public Command(String argument, Consumer<Message> func) {
        this.argument = argument;
        this.func = func;
    }

    public Command(String argument) {
        this.argument = argument;
    }
}
