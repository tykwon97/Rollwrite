package com.rollwrite.domain.notification.repository;

import com.rollwrite.domain.notification.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    Optional<Alarm> findAlarmByUser_IdAndFirebaseToken(Long userId, String firebaseToken);
    Optional<List<Alarm>> findAlarmsByUser(Long userId);
}
