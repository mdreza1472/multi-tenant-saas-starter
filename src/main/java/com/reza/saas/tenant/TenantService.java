package com.reza.saas.tenant;

import com.reza.saas.common.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenants;

    public TenantService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public Tenant getCurrent() {
        return tenants.findCurrent().orElseThrow(() -> new NotFoundException("Tenant not found"));
    }

    /**
     * Self-serve onboarding. NOT IMPLEMENTED - planned steps:
     *  1. Generate a tenantId (UUID, satisfies TenantContext's id pattern).
     *  2. TenantRepository.create(...) with plan FREE (conditional put).
     *  3. Cognito AdminCreateUser for adminEmail with custom:tenant_id = tenantId
     *     (immutable attribute, settable at creation only), then AdminAddUserToGroup
     *     TENANT_ADMIN. Needs the cognitoidentityprovider SDK dependency and
     *     cognito-idp:Admin* permissions on the Lambda role (see CDK stack TODO).
     *  4. Create the billing customer via BillingService.
     *  5. Compensate on partial failure (delete the tenant item / Cognito user) so a
     *     half-provisioned tenant never exists.
     */
    public Tenant provision(String tenantName, String adminEmail) {
        throw new UnsupportedOperationException("Tenant provisioning is not implemented yet");
    }
}
