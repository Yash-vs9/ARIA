package com.yash.nerve.repository;

import com.yash.nerve.models.Chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat,Long> {
}
