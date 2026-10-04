package com.reza.saas.persistence;

import java.util.regex.Pattern;

/**
 * The ONE place key formats live. Every item's partition key starts with
 * TENANT#<tenantId>, so any query is scoped to a tenant by construction.
 * See docs/architecture.md for the full key-design table.
 */
public final class SingleTableKeys {

    public static final String TENANT_PREFIX = "TENANT#";
    public static final String TENANT_META_SK = "META";
    public static final String USER_PREFIX = "USER#";
    public static final String NOTE_PREFIX = "NOTE#";

    // Mirrors TenantContext's validation - duplicated deliberately rather than
    // imported, so this class defends its own output even if a caller (like
    // TenantRepository.create, which bypasses TenantContext during provisioning)
    // forgets to validate first.
    private static final Pattern VALID_TENANT_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private SingleTableKeys() {
    }

    public static String tenantPk(String tenantId) {
        if (tenantId == null || !VALID_TENANT_ID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("Invalid tenant id: cannot build a key from it");
        }
        return TENANT_PREFIX + tenantId;
    }
}