package com.url_shortner.project.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url_shortner.project.dto.UrlRequestDto;
import com.url_shortner.project.dto.UserRequestDto;
import com.url_shortner.project.dto.UserResponseDto;
import com.url_shortner.project.entity.UserEntity;
import com.url_shortner.project.repository.UserRepository;
import com.url_shortner.project.service.JWTService;
import com.url_shortner.project.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    @Test
    void testShortenUrl_WithoutToken_Forbidden() throws Exception {
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://google.com");

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testShortenUrl_WithValidToken_Created() throws Exception {
        // Create a user to get a valid token
        UserRequestDto userRequest = new UserRequestDto();
        userRequest.setEmail("security-test@example.com");
        userRequest.setName("Security Tester");

        UserResponseDto createdUser = userService.createUser(userRequest);
        String token = createdUser.getToken();

        // Perform shorten request with token
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://google.com");

        mockMvc.perform(post("/shorten")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
