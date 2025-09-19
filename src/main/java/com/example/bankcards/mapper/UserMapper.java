package com.example.bankcards.mapper;

import com.example.bankcards.dto.request.UserCreateRequest;
import com.example.bankcards.dto.response.UserAuthResponse;
import com.example.bankcards.dto.response.UserResponse;
import com.example.bankcards.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cards", ignore = true)
    User toUser(UserCreateRequest dto, String passwordHash);

    UserResponse toUserResponse(User user);

    UserAuthResponse toUserAuthResponse(User user, boolean enabled);
}
