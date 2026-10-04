package com.reza.saas.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    public record UserResponse(String userId, String email, String role) {
        static UserResponse from(TenantUser u) {
            return new UserResponse(u.getUserId(), u.getEmail(), u.getRole());
        }
    }

    public record InviteUserRequest(@NotBlank @Email String email,
            @NotNull Role role) {
    }

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Void> invite(@Valid @RequestBody InviteUserRequest request) {
        userService.invite(request.email(), request.role());
        return ResponseEntity.accepted().build();
    }
}
