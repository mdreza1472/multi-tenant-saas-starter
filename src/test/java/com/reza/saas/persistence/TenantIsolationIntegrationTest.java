package com.reza.saas.persistence;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * THE test that matters most for a multi-tenant product, and the first one to
 * write for real: prove tenant A can never read, list, overwrite or delete tenant B's data.
 *
 * Needs a real DynamoDB (DynamoDB Local or Testcontainers/LocalStack) - the repository
 * builds keys, and the guarantee is about what actually comes back from the table.
 */
@Disabled("Needs DynamoDB Local / Testcontainers - implement next")
class TenantIsolationIntegrationTest {

    @Test
    void tenantA_cannotSeeTenantB_notes() {
        // TODO: bind tenant A -> save a note -> bind tenant B -> assert
        //       listAll() is empty and findById(noteIdFromA) is empty.
    }

    @Test
    void save_ignoresCallerSuppliedKeys() {
        // TODO: set pk = TENANT#<other tenant> on an item before save() -> assert the
        //       stored item's pk is the CURRENT tenant's.
    }

    @Test
    void tokenWithoutTenantClaim_isRejectedWith403() {
        // TODO: MockMvc + spring-security-test jwt() with no custom:tenant_id -> 403.
    }
}
