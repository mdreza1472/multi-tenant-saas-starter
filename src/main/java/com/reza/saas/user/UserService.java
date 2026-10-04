package com.reza.saas.user;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public List<TenantUser> list() {
        return users.listAll();
    }

    /**
     * NOT IMPLEMENTED - planned steps:
     *  1. Check the tenant's plan limit (Plan.maxUsers) against the current user count
     *     (TENANT_ADMIN and MEMBER both count toward the same cap - no separate admin-seat limit).
     *  2. Pre-check gsi1pk = EMAIL#<email> (a GSI query) to detect the email is already used
     *     by another tenant, before calling Cognito - avoids making an external API call
     *     that Cognito would reject anyway (Cognito enforces email uniqueness pool-wide),
     *     and gives a cleaner, faster rejection.
     *  3. Cognito AdminCreateUser with custom:tenant_id = the CALLER's tenant (from
     *     TenantContext, never from the request), plus the invited group/role.
     *  4. Save the TenantUser profile item, with gsi1pk = EMAIL#<email> set, so future
     *     invites can find it via step 2's check.
     */
    public void invite(String email, Role role) {
        throw new UnsupportedOperationException("User invitation is not implemented yet");
    }
}
