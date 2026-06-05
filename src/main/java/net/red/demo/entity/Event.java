package net.red.demo.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import lombok.Getter;
import lombok.Setter;

import net.red.demo.remote.dto.enums.EventState;

@Getter
@Setter
@Entity
@SequenceGenerator(name = "eventGen", sequenceName = "event_id_seq", allocationSize = 1)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "eventGen")
    private Long id;
    @Column(nullable = false, unique = true)
    private String externalId;
    @Column
    private Long providerEventId;
    @Enumerated(EnumType.STRING)
    private EventState state;
    @Column(nullable = false)
    private String sport;
    @Column(nullable = false)
    private String league;
    // provider returns startDate values like "2020-11-09T:22:30:00" in UTC
    @Column(nullable = false)
    private OffsetDateTime startDate;
    @Column(nullable = false)
    private String matchName;
    // make sure that streamNames always sorted in the same way to prevent redundant Entity update
    @Type(StringArrayType.class)
    private String[] streamNames;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.changedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.changedAt = OffsetDateTime.now();
    }
}
