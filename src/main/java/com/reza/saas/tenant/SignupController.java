package com.reza.saas.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Size;

/**
 * Public (unauthenticated) tenant onboarding. Currently answers 501 - see
 * {@link TenantService#provision}. Open question: this route will need abuse
 * protection (rate limiting / captcha / email verification) before it goes live.
 */
@RestController
public class SignupController {

	public record SignupRequest(@NotBlank @Size(max = 200) String tenantName,
            @NotBlank @Email @Size(max = 254) String adminEmail) {
	}

    private final TenantService tenantService;

    public SignupController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/signup")
    public ResponseEntity<TenantController.TenantResponse> signup(@Valid @RequestBody SignupRequest request) {
        Tenant tenant = tenantService.provision(request.tenantName(), request.adminEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantController.TenantResponse.from(tenant));
    }
}
