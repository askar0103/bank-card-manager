package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.LoginResponse;
import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.enums.ErrorCode;
import com.example.bankcards.enums.Role;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.util.provider.InvalidUsernamePasswordProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.bankcards.util.TestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    private LoginRequest loginRequest;
    private LoginResponse loginResponse;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(
                TEST_USERNAME,
                TEST_USER_PASSWORD
        );
        loginResponse = new LoginResponse(
                TEST_USERNAME,
                Role.USER,
                TEST_TOKEN
        );
        userDetails = CustomUserDetails.from(new UserAuthResponse(
                TEST_USER_ID,
                TEST_USERNAME,
                TEST_PASSWORD_HASH,
                true,
                Role.USER
        ));
    }

    @Nested
    class LoginTests {

        // --- POSITIVE CASE ---

        @Test
        void login_WithValidCredentials_ReturnsLoginResponse() throws Exception {
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtService.generateToken(userDetails)).thenReturn(TEST_TOKEN);

            LoginResponse expectedResponse = loginResponse;

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(expectedResponse.username()))
                    .andExpect(jsonPath("$.role").value(expectedResponse.role().name()))
                    .andExpect(jsonPath("$.token").value(expectedResponse.token()));

            verify(authenticationManager).authenticate(any());
            verify(authentication).getPrincipal();
            verify(jwtService).generateToken(userDetails);
        }

        // --- NEGATIVE CASES ---

        @Test
        void login_WithInvalidCredentials_ReturnsUnauthorized() throws Exception {
            String expectedMessage = "Invalid credentials";

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException(expectedMessage));

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(expectedMessage))
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.UNAUTHORIZED.name()));

            verify(jwtService, never()).generateToken(any());
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidUsernamePasswordProvider.class)
        void login_WithInvalidInput_ReturnsBadRequest(String username, String password) throws Exception {
            LoginRequest invalidInput = new LoginRequest(username, password);

            mockMvc.perform(post("/api/v1/auth/login")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidInput)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(ErrorCode.BAD_REQUEST.name()));

            verify(authenticationManager, never()).authenticate(any());
            verify(jwtService, never()).generateToken(any());
        }
    }
}
