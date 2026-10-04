package com.reza.saas.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleTableKeysTest {

    @Test
    void tenantPk_isPrefixedSoEveryItemIsTenantScoped() {
        assertEquals("TENANT#tenant-12345", SingleTableKeys.tenantPk("tenant-12345"));
    }
}
