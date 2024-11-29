package models;

import java.util.Date;

import java.util.Date;

public class Payment {
    private int id;
    private int bookingId;
    private double amount;
    private Date paymentDate;
    private String status;
    private Date refundDate;

    // Constructors
    public Payment() {}

    public Payment(int id, int bookingId, double amount, Date paymentDate, String status, Date refundDate) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.status = status;
        this.refundDate = refundDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getRefundDate() { return refundDate; }
    public void setRefundDate(Date refundDate) { this.refundDate = refundDate; }
}