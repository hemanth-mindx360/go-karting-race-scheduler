package exceptions;
public class InvalidGroupSizeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public InvalidGroupSizeException(String message) { super(message); }
}
