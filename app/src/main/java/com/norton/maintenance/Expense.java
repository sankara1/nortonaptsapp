package com.norton.maintenance;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Expense {
    public String type; // e.g., "Watchman", "TNEB"
    public int amount;
    public String paymentType;
    public String month; // Month name for easier filtering
    public int year;
    public Date date;
    public String comments;

    public Expense() {}

    public Expense(String type, int amount, String paymentType, String month, int year, Date date, String comments) {
        this.type = type;
        this.amount = amount;
        this.paymentType = paymentType;
        this.month = month;
        this.year = year;
        this.date = date;
        this.comments = comments;
    }
}