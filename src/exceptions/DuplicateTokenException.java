package exceptions;
public class DuplicateTokenException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public DuplicateTokenException(String message) { super(message); }
}
