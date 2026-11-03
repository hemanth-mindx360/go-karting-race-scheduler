package exceptions;
public class InvalidTokenStateException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidTokenStateException(String message) { super(message); }
}
