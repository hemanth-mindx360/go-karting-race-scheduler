package exceptions;
public class IllegalAdminActionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public IllegalAdminActionException(String message) { super(message); }
}
