package models;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
public class Customer {
    private final String username;
    private final String password;
    private final String name;
 
    private final List<BookingToken> bookings = new ArrayList<>();
 
    public Customer(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
 
    public String getUsername() {
         return username;
    }
 
    public String getPassword() {
        return password;
    }
    public String getName() {
        return name;
    }
 
    public List<BookingToken> getBookings() {
         return Collections.unmodifiableList(bookings);
        }
   
    public void addBooking(BookingToken booking) {
         bookings.add(booking);
        }
 
}