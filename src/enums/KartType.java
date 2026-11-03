package enums;
 
public enum KartType {
    SINGLE,
    TWIN,
    KIDS;
 
    public double priceForLaps(int laps) {
        switch (this) {
            case SINGLE:
                if (laps == 5) return 499;
                if (laps == 12) return 999;
                if (laps == 20) return 1499;
                break;
            case TWIN:
                if (laps == 5) return 699;
                break;
            case KIDS:
                if (laps == 5) return 399;
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupported lap count " + laps + " for " + this);
    }
}
 
 