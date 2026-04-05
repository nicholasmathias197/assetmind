package com.assetmind.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AssetmindUserDetails implements UserDetails {

    private final String id;
    private final String username;
    private final String password;
    private final String role;
    private final Set<String> featureAccess;
    private final boolean enabled;

    public AssetmindUserDetails(UserEntity user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.featureAccess = new LinkedHashSet<>();
        this.enabled = user.isEnabled();
    }

    public AssetmindUserDetails(UserEntity user, Collection<String> featureAccess) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.featureAccess = new LinkedHashSet<>(featureAccess);
        this.enabled = user.isEnabled();
    }

    public String getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        for (String feature : featureAccess) {
            authorities.add(new SimpleGrantedAuthority("FEATURE_" + feature.toUpperCase()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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
        return enabled;
    }
}

