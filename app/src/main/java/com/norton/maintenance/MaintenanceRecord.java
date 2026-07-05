package com.norton.maintenance;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

@IgnoreExtraProperties
public class MaintenanceRecord {
    public String flatNo;
    public String month;
    public int year;
    public String paymentType;
    public int amount;
    public String phoneNumber;
    @ServerTimestamp public Date date;
    public String comments;

    // 1. Required No-argument constructor for Firebase
    public MaintenanceRecord() {}

    // 2. The constructor your MainActivity.java is using
    public MaintenanceRecord(String flatNo, String month, int year, String paymentType, int amount, String phoneNumber, String comments) {
        this.flatNo = flatNo;
        this.month = month;
        this.year = year;
        this.paymentType = paymentType;
        this.amount = amount;
        this.phoneNumber = phoneNumber;
        this.comments = comments;
    }
}