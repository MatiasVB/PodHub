package org.podhub.podhub.security;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.dto.*;
import org.podhub.podhub.exception.InvalidRefreshTokenException;
import org.podhub.podhub.model.Role;
import org.podhub.podhub.model.User;
import org.podhub.podhub.model.RefreshToken;
import org.podhub.podhub.model.enums.UserRole;
import org.podhub.podhub.model.enums.UserStatus;
import org.podhub.podhub.repository.RefreshTokenRepository;
import org.podhub.podhub.repository.RoleRepository;
import org.podhub.podhub.repository.UserRepository;
import org.podhub.podhub.security.jwt.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    // =====================================================
    // LOGIN
    // =====================================================
    public AuthResponse login(AuthRequest request) {

        // 1. Validar credenciales usando AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.identifier(),
                        request.password()
                )
        );

        // 2. Cargar usuario
        User user = userRepository.findByEmail(request.identifier())
                .or(() -> userRepository.findByUsername(request.identifier()))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 3. Generar roles como strings
        List<String> roles =
                user.getRoleIds()
                        .stream()
                        .map(roleRepository::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(role -> "ROLE_" + role.getName().name())
                        .toList();

        // 4. Generar Access Token
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                Map.of("roles", roles)
        );

        // 5. Generar Refresh Token opaco
        RefreshToken refresh = createRefreshToken(user.getId());

        // 6. Mapear User → UserResponse
        UserResponse userResponse = UserResponse.from(user, roles);

        // 7. Crear AuthResponse
        return AuthResponse.of(
                accessToken,
                refresh.getToken(),
                900,                // exp. access
                7 * 24 * 3600,      // exp. refresh
                Instant.now(),
                refresh.getExpiresAt(),
                userResponse,
                roles
        );
    }

    // =====================================================
    // REGISTER
    // =====================================================
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email()))
            throw new RuntimeException("Ya existe un usuario con ese email.");

        if (userRepository.existsByUsername(request.username()))
            throw new RuntimeException("Ya existe un usuario con ese username.");

        // Buscar rol base USER
        Role userRole = roleRepository.findByName(UserRole.USER)
                .orElseThrow(() -> new RuntimeException("El rol USER no existe en la BD"));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .avatarUrl(request.avatarUrl())
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .roleIds(Set.of(userRole.getId()))
                .build();

        userRepository.save(user);

        List<String> roles = List.of("ROLE_USER");

        return UserResponse.from(user, roles);
    }

    // =====================================================
    // REFRESH TOKEN
    // =====================================================
    public AuthResponse refresh(RefreshRequest request) throws InvalidRefreshTokenException {

            RefreshToken oldToken = refreshTokenRepository.findByToken(request.refreshToken())
                    .orElseThrow(() -> new InvalidRefreshTokenException(request.refreshToken()));

            if (oldToken.isRevoked() || oldToken.getExpiresAt().isBefore(Instant.now())) {
                throw new InvalidRefreshTokenException(request.refreshToken());
            }

            // Cargar usuario asociado
            User user = userRepository.findById(oldToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

            List<String> roles =
                    user.getRoleIds()
                            .stream()
                            .map(roleRepository::findById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(role -> "ROLE_" + role.getName().name())
                            .toList();

            // Rotación de refresh token (revocar y generar uno nuevo)
            oldToken.setRevoked(true);
            refreshTokenRepository.save(oldToken);

            RefreshToken newRefresh = createRefreshToken(user.getId());

            // Generar access token nuevo
            String newAccessToken = jwtService.generateAccessToken(
                    user.getEmail(),
                    Map.of("roles", roles)
            );

            return AuthResponse.of(
                    newAccessToken,
                    newRefresh.getToken(),
                    900,
                    7 * 24 * 3600,
                    Instant.now(),
                    newRefresh.getExpiresAt(),
                    UserResponse.from(user, roles),
                    roles
            );
        }

        // =====================================================
        // HELPERS
        // =====================================================
        private RefreshToken createRefreshToken (String userId){
            RefreshToken token = new RefreshToken();

            token.setId(UUID.randomUUID().toString());
            token.setUserId(userId);
            token.setToken(UUID.randomUUID().toString().replace("-", "")); // opaco
            token.setCreatedAt(Instant.now());
            token.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600)); // 7 días
            token.setRevoked(false);

            return refreshTokenRepository.save(token);
        }
    }
