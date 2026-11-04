package exceptions;
public class RaceStartException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RaceStartException(String message) { super(message); }
}
