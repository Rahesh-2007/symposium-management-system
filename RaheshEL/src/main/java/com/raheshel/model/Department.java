package com.raheshel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private int departmentId;

    @Column(name = "department_name", nullable = false, unique = true)
    private String departmentName;

    @Column(name = "head_name")
    private String headName;

    @Column(name = "head_phone")
    private String headPhone;

    @Column(name = "head_email")
    private String headEmail;

    public Department() {}

    public Department(String departmentName, String headName, String headPhone, String headEmail) {
        this.departmentName = departmentName;
        this.headName = headName;
        this.headPhone = headPhone;
        this.headEmail = headEmail;
    }

    // Getters & Setters
    public int getDepartmentId()                    { return departmentId; }
    public void setDepartmentId(int id)             { this.departmentId = id; }

    public String getDepartmentName()               { return departmentName; }
    public void setDepartmentName(String name)      { this.departmentName = name; }

    public String getHeadName()                     { return headName; }
    public void setHeadName(String headName)        { this.headName = headName; }

    public String getHeadPhone()                    { return headPhone; }
    public void setHeadPhone(String headPhone)      { this.headPhone = headPhone; }

    public String getHeadEmail()                    { return headEmail; }
    public void setHeadEmail(String headEmail)      { this.headEmail = headEmail; }
}
