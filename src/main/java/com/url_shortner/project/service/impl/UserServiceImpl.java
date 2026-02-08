package com.url_shortner.project.service.impl;

import com.url_shortner.project.dto.UserRequestDto;
import com.url_shortner.project.dto.UserResponseDto;
import com.url_shortner.project.entity.UserEntity;
import com.url_shortner.project.repository.UserRepository;
import com.url_shortner.project.service.JWTService;
import com.url_shortner.project.service.UserService;
import lombok.RequiredArgsConstructor;
import com.url_shortner.project.entity.Role;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JWTService jwtService;

    @Override
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .name(request.getName())
                .apiKey(UUID.randomUUID().toString())
                .build();

        Set<String> rolesToProcess = new HashSet<>();
        if (request.getRole() != null) {
            rolesToProcess.add(request.getRole());
        }

        System.out.println("rolesToProcess: " + rolesToProcess + " isEmpty: " + rolesToProcess.isEmpty());
        if (!rolesToProcess.isEmpty()) {
            Set<Role> roles = rolesToProcess.stream()
                    .map(roleName -> {
                        try {
                            return Role.valueOf(roleName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleName);
                        }
                    })
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        } else {
            user.getRoles().add(Role.HOBBY);
        }

        UserEntity savedUser = userRepository.save(user);
        System.out.println("User created: " + savedUser);

        String token = jwtService.generateToken(savedUser);

        return UserResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .apiKey(savedUser.getApiKey())
                .createdAt(savedUser.getCreatedAt())
                .token(token)
                .build();
    }
}
