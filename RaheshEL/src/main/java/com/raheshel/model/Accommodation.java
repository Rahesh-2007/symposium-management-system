package com.raheshel.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class Accommodation {

    public static final int RATE_PER_DAY = 350; // same as original

    @Column(name = "accommodation_days")
    private int days;

    @Column(name = "accommodation_total")
    private int totalAmount;

    public Accommodation() {}

    public Accommodation(int days) {
        this.days = Math.max(0, days);
        this.totalAmount = this.days * RATE_PER_DAY;
    }

    // Same updateDays logic from original
    public void updateDays(int days) {
        this.days = Math.max(0, days);
        this.totalAmount = this.days * RATE_PER_DAY;
    }

    // Getters & Setters
    public int getDays()                 { return days; }
    public void setDays(int days)        { this.days = days; }

    public int getTotalAmount()          { return totalAmount; }
    public void setTotalAmount(int amt)  { this.totalAmount = amt; }
}
