package com.verzel.challenge.repository;

import com.verzel.challenge.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity,Long> {
}
