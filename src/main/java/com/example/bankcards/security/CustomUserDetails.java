package com.example.bankcards.security;

import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.enums.Role;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomUserDetails implements UserDetails, CredentialsContainer {

    private final Long id;

    private final String username;

    // mutable — so eraseCredentials() can null it
    private String passwordHash;

    private final boolean enabled;

    private final Role role;

    private final List<GrantedAuthority> authorities;


    public static CustomUserDetails from(UserAuthResponse userAuthResponse) {
        return new CustomUserDetails(
                userAuthResponse.id(),
                userAuthResponse.username(),
                userAuthResponse.passwordHash(),
                userAuthResponse.enabled(),
                userAuthResponse.role(),
                List.of(new SimpleGrantedAuthority(
                        userAuthResponse.role().getAuthority()
                ))
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }
}
