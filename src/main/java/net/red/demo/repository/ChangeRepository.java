package net.red.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import net.red.demo.entity.Change;

@Repository
public interface ChangeRepository extends JpaRepository<Change, Long> {
}