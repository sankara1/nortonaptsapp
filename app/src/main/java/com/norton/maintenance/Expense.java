package com.norton.maintenance;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Expense {
    public String type; // e.g., "Watchman", "TNEB"
    public double amount;
    public String paymentType;
    public String month; // Month name for easier filtering
    public Date date;
    public String comments;

    public Expense() {}

    public Expense(String type, double amount, String paymentType, String month, Date date, String comments) {
        this.type = type;
        this.amount = amount;
        this.paymentType = paymentType;
        this.month = month;
        this.date = date;
        this.comments = comments;
    }
}