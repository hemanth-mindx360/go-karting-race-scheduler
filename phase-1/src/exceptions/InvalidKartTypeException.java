package exceptions;
public class InvalidKartTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidKartTypeException(String message) { super(message); }
}
