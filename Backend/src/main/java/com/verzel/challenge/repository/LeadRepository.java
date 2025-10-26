package com.verzel.challenge.repository;

import com.verzel.challenge.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<LeadEntity,Long> {
    Optional<LeadEntity> findByEmailIgnoreCase(String email);
}
