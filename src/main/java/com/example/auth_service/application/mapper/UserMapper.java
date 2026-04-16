package com.example.auth_service.application.mapper;

import com.example.auth_service.application.dto.RegisterRequest;
import com.example.auth_service.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

@Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "accountStatus", constant = "ACTIVE")
    @Mapping(target = "failedLoginAttempts", constant = "0")
    User toEntity(RegisterRequest request);
}
