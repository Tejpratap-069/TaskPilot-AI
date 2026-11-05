package com.taskpilot.util;

import com.taskpilot.entity.User;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    private final UserRepository users;

    public SecurityUtil(UserRepository users) {
        this.users = users;
    }

    /** Identity always comes from the JWT security context, never from the request body. */
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("No authenticated user");
        }
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
