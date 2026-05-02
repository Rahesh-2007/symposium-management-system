package com.raheshel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "college")
public class College {

    @Id
    @Column(name = "college_id")
    private int collegeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    public College() {}

    public College(String name, int collegeId, String address) {
        this.name = name;
        this.collegeId = collegeId;
        this.address = address;
    }

    // Getters & Setters
    public int getCollegeId()             { return collegeId; }
    public void setCollegeId(int id)      { this.collegeId = id; }

    public String getName()               { return name; }
    public void setName(String name)      { this.name = name; }

    public String getAddress()            { return address; }
    public void setAddress(String addr)   { this.address = addr; }
}
