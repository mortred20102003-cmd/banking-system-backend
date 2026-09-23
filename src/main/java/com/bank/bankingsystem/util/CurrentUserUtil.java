package com.bank.bankingsystem.util;

import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    public static AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser au)) {
            throw new UnauthorizedAccountAccessException("No authenticated user in context");
        }
        return au;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(currentUser().getRole());
    }

    public static boolean isAdminOrAbove() {
        String role = currentUser().getRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
    }
}