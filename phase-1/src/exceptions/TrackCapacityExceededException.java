package exceptions;
public class TrackCapacityExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public TrackCapacityExceededException(String message) { super(message); }
}
