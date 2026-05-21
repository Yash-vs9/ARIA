package com.yash.nerve.repository;

import com.yash.nerve.models.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<ChatMessage,Long> {
}
