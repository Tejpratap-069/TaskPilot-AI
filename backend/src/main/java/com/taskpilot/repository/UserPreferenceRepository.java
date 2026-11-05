package com.taskpilot.repository;

import com.taskpilot.entity.UserPreference;
import com.taskpilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUser(User user);
}
