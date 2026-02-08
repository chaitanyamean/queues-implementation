package com.url_shortner.project.service;

import com.url_shortner.project.dto.UserRequestDto;
import com.url_shortner.project.dto.UserResponseDto;

public interface UserService {
    UserResponseDto createUser(UserRequestDto request);
}
