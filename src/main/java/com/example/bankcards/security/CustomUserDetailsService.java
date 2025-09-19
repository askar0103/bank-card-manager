package com.example.bankcards.security;

import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.service.application.UserApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserApplicationService userApplicationService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthResponse userAuthResponse = userApplicationService.getUserAuthByUsername(username);
        return CustomUserDetails.from(userAuthResponse);
    }
}
