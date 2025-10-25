package com.verzel.challenge.entity;

import com.verzel.challenge.type.Sender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_tb")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Sender sender;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    public MessageEntity(Sender sender, String content, ChatSessionEntity session) {
        this.sender = sender;
        this.content = content;
        this.session = session;
    }

    @CreatedDate
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ChatSessionEntity session;
}
