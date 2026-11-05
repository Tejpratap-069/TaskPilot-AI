package com.taskpilot.repository;

import com.taskpilot.entity.Category;
import com.taskpilot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByUserAndNameIgnoreCase(User user, String name);
    List<Category> findByUserOrderByNameAsc(User user);
}
