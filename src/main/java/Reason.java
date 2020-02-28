public class Reason {
    String message;
    double value;

    public Reason(String message, double value) {
        this.message = message;
        this.value = value;
    }

    @Override
    public String toString() {
        return message;
    }
}
