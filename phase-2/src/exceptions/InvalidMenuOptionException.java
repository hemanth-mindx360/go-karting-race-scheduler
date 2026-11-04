package exceptions;
public class InvalidMenuOptionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidMenuOptionException(String message) { super(message); }
}
