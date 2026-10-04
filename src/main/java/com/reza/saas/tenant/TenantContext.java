package com.reza.saas.tenant;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Holds the tenant id for the request currently being handled.
 *
 * The id is set ONLY by {@link TenantContextFilter}, from the verified JWT's
 * custom:tenant_id claim - never from a header, path or body the client controls.
 *
 * Ids are validated against a strict pattern because they are concatenated into
 * DynamoDB keys (TENANT#<id>): allowing '#' would let a crafted id collide with
 * another key shape.
 */
public final class TenantContext {

    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        if (tenantId == null || !VALID_ID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("Invalid tenant id");
        }
        CURRENT.set(tenantId);
    }

    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** Fails loudly (rather than defaulting) if no tenant is bound - default-deny. */
    public static String requireTenantId() {
        String tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant bound to this request");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
