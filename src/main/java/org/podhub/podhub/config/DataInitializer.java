package org.podhub.podhub.config;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.model.Role;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.RoleRepository;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // CREAR ROLES SI NO EXISTEN

        createRoleIfNotExists(UserRole.USER, Set.of(
                "EPISODE_READ",
                "PODCAST_READ"
        ));

        createRoleIfNotExists(UserRole.CREATOR, Set.of(
                "EPISODE_READ",
                "EPISODE_WRITE",
                "PODCAST_READ",
                "PODCAST_WRITE"
        ));

        createRoleIfNotExists(UserRole.ADMIN, Set.of(
                "ADMIN_PANEL",
                "EPISODE_READ",
                "EPISODE_WRITE",
                "PODCAST_READ",
                "PODCAST_WRITE",
                "USER_MANAGE"
        ));

        // CREAR USUARIO ADMIN POR DEFECTO SI NO EXISTE

        if (!userRepository.existsByEmail("admin@podhub.com")) {

            Role adminRole = roleRepository.findByName(UserRole.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .id(UUID.randomUUID().toString())
                    .username("admin")
                    .email("admin@podhub.com")
                    .passwordHash(passwordEncoder.encode("admin123")) // ⚠️ cambiar en producción
                    .displayName("Administrator")
                    .avatarUrl(null)
                    .roleIds(Set.of(adminRole.getId()))
                    .status(UserStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            userRepository.save(admin);
            System.out.println("Admin user created: admin@podhub.com / admin123");
        }
    }

    private void createRoleIfNotExists(UserRole roleName, Set<String> permissions) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .id(UUID.randomUUID().toString())
                    .name(roleName)
                    .permissionNames(permissions)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            roleRepository.save(role);
            System.out.println("Role created: " + roleName);
        }
    }
}
