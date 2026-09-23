package com.bank.bankingsystem.config;

import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.UserStatus;
import com.bank.bankingsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class SuperAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);
    private static final String PLACEHOLDER = "PENDING_SEED_REPLACE_AT_STARTUP";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.username:MikeyD}")
    private String superAdminUsername;

    @Value("${app.super-admin.email:mikeyD@bank.com}")
    private String superAdminEmail;

    @Value("${app.super-admin.password:}")
    private String superAdminPassword;

    public SuperAdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public ApplicationRunner seedSuperAdmin() {
        return (ApplicationArguments args) -> {
            var existing = userRepository.findByUsername(superAdminUsername);

            if (existing.isEmpty()) {
                // V6 should have inserted the placeholder row already; if not,
                // create it now to keep the app usable.
                if (!StringUtils.hasText(superAdminPassword)) {
                    log.error("SUPER_ADMIN_PASSWORD is empty. Set it in .env then restart.");
                    return;
                }
                User u = new User();
                u.setUsername(superAdminUsername);
                u.setEmail(superAdminEmail);
                u.setPasswordHash(passwordEncoder.encode(superAdminPassword));
                u.setRole(Role.SUPER_ADMIN);
                u.setStatus(UserStatus.ACTIVE);
                userRepository.insert(u);
                log.info("Seeded SUPER_ADMIN '{}' from environment.", superAdminUsername);
                return;
            }

            User superAdmin = existing.get();

            if (PLACEHOLDER.equals(superAdmin.getPasswordHash())) {
                if (!StringUtils.hasText(superAdminPassword)) {
                    log.error("SUPER_ADMIN_PASSWORD is empty. Cannot replace placeholder. " +
                              "Set it in .env then restart.");
                    return;
                }
                String hash = passwordEncoder.encode(superAdminPassword);
                userRepository.updatePasswordHash(superAdmin.getId(), hash);
                log.info("Replaced SUPER_ADMIN placeholder with BCrypt hash from env.");
            } else {
                log.debug("SUPER_ADMIN '{}' already seeded. Skipping.", superAdminUsername);
            }

            // Ensure email + role are correct even if V6 was edited.
            if (!superAdminEmail.equalsIgnoreCase(superAdmin.getEmail())
                    || superAdmin.getRole() != Role.SUPER_ADMIN) {
                // no-op in normal flow; kept minimal — use SQL if you need to force-align
                log.debug("SUPER_ADMIN drift detected (email/role). Manual review recommended.");
            }
        };
    }
}