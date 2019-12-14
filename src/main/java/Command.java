import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.function.BinaryOperator;
import java.util.function.Function;

public class Command {
    String argument;
    Function<Message, Integer> func;


    public Command(String argument, Function<Message, Integer> func) {
        this.argument = argument;
        this.func = func;
    }
}
