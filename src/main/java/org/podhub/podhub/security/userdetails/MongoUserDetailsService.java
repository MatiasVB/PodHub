package org.podhub.podhub.security.userdetails;

import lombok.RequiredArgsConstructor;
import org.podhub.podhub.model.User;
import org.podhub.podhub.repository.RoleRepository;
import org.podhub.podhub.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * UserDetailsService que carga el usuario desde MongoDB.
 */
@Service
@RequiredArgsConstructor
public class MongoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Permitir login con email o username
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + identifier));

        // Convertir roles (almacenados por IDs) a GrantedAuthorities
        List<GrantedAuthority> authorities = user.getRoleIds()
                .stream()
                .map(roleId -> roleRepository.findById(roleId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(role -> {

                    // authority by role
                    Stream<GrantedAuthority> roleAuthority =
                            Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));

                    // permissions
                    Stream<GrantedAuthority> permissionAuthorities =
                            role.getPermissionNames()
                                    .stream()
                                    .map(SimpleGrantedAuthority::new);

                    return Stream.concat(roleAuthority, permissionAuthorities);
                })
                .toList();


        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // username interno para security
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}
