package com.gradapptracker.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserPrincipal implements Spring Security's UserDetails and holds the
 * authenticated user's info.
 */
public class UserPrincipal implements UserDetails {
    private final Integer id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Integer id, String email, String password,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public Integer getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static UserPrincipal create(User user) {
        String roles = user.getRoles();
        List<SimpleGrantedAuthority> authorities;
        if (roles == null || roles.isBlank()) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), authorities);
    }

    // Overload to support the real entity class
    public static UserPrincipal create(com.gradapptracker.backend.user.entity.User userEntity) {
        String roles = userEntity.getRoles();
        List<SimpleGrantedAuthority> authorities;
        if (roles == null || roles.isBlank()) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return new UserPrincipal(userEntity.getUserId(), userEntity.getEmail(), userEntity.getPassword(), authorities);
    }
}

// Minimal package-private User class to allow compiling this file in absence of
// a separate entity.
class User {
    private Integer id;
    private String email;
    private String password;
    private String roles;

    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRoles() {
        return roles;
    }
}

