package exceptions;
public class RaceEndException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RaceEndException(String message) { super(message); }
}
