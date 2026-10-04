package com.reza.saas.tenant;

import com.reza.saas.billing.Plan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants")
public class TenantController {

    public record TenantResponse(String tenantId, String name, Plan plan, String status) {
        static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getTenantId(), t.getName(), t.getPlan(), t.getStatus());
        }
    }

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** "current" = the tenant from the caller's token; there is deliberately no /tenants/{id}. */
    @GetMapping("/current")
    public TenantResponse current() {
        return TenantResponse.from(tenantService.getCurrent());
    }
}
