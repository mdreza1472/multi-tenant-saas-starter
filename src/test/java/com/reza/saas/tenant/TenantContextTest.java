package com.reza.saas.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    void requireTenantId_withNothingBound_throws() {
        assertThrows(IllegalStateException.class, TenantContext::requireTenantId);
    }

    @Test
    void set_thenRequire_returnsTheTenant() {
        TenantContext.set("tenant-12345");
        assertEquals("tenant-12345", TenantContext.requireTenantId());
    }

    @Test
    void clear_removesTheBinding() {
        TenantContext.set("tenant-12345");
        TenantContext.clear();
        assertTrue(TenantContext.current().isEmpty());
    }

    @Test
    void set_rejectsIdsThatCouldInjectKeySeparators() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set("abc#USER#1"));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set(null));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set("short"));
    }
}
