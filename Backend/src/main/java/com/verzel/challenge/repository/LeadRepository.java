package com.verzel.challenge.repository;

import com.verzel.challenge.entity.LeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<LeadEntity,Long> {
}
