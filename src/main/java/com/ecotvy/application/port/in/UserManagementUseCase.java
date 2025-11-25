package com.ecotvy.application.port.in;

import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;

public interface UserManagementUseCase {
    UserDto registerUser(UserDto userDto);
    UserDto login(String email, String password);
    UserDto getUserById(Long userId);
    UserDto updateProfile(Long userId, UserProfileUpdateRequest request);
    void updateLocation(Long userId, Double latitude, Double longitude);
}

