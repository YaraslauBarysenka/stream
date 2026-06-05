package net.red.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;

import net.red.demo.kafka.dto.StreamChange;

@Data
@Entity
@SequenceGenerator(name = "changeGen", sequenceName = "change_id_seq", allocationSize = 1)
public class Change {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "changeGen")
    private Long id;
    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private StreamChange content;
}