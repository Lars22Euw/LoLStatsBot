public class Reason {
    String message;
    double value;
    static int longestReason = 0;

    public Reason(String message, double value) {
        this.message = message;
        this.value = value;
        if (message.length() > longestReason) longestReason = message.length()+2;
    }

    @Override
    public String toString() {
        return message;
    }
}
