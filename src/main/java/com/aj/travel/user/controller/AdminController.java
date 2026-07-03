package com.aj.travel.user.controller;

import com.aj.travel.common.api.ApiResponse;
import com.aj.travel.user.dto.CreateUserRequest;
import com.aj.travel.user.dto.UserResponse;
import com.aj.travel.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> registerAdmin(
            @Valid @RequestBody CreateUserRequest request
    ) {
        System.out.println("hi");
        UserResponse savedAdmin = userService.createAdmin(request);

        return ApiResponse.success(
                "Admin registered successfully",
                savedAdmin
        );
    }
}
