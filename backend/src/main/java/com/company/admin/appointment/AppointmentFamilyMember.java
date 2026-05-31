package com.company.admin.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_family_member")
public class AppointmentFamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_record_id", nullable = false)
    private AppointmentRecord appointmentRecord;

    @Column(length = 64)
    private String relationship;

    @Column(length = 64)
    private String name;

    private Integer age;

    @Column(name = "political_status", length = 80)
    private String politicalStatus;

    @Column(name = "work_unit_and_position", length = 255)
    private String workUnitAndPosition;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public Long getId() {
        return id;
    }

    public AppointmentRecord getAppointmentRecord() {
        return appointmentRecord;
    }

    public void setAppointmentRecord(AppointmentRecord appointmentRecord) {
        this.appointmentRecord = appointmentRecord;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPoliticalStatus() {
        return politicalStatus;
    }

    public void setPoliticalStatus(String politicalStatus) {
        this.politicalStatus = politicalStatus;
    }

    public String getWorkUnitAndPosition() {
        return workUnitAndPosition;
    }

    public void setWorkUnitAndPosition(String workUnitAndPosition) {
        this.workUnitAndPosition = workUnitAndPosition;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
