public class InputError extends Error {

    String error;
    InputError(String message) {
        error = message;
        System.out.println(error);
    }
}
