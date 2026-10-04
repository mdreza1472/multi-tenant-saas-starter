package com.reza.saas.user;

import com.reza.saas.persistence.BaseItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

/**
 * A user profile inside a tenant: pk = TENANT#<id>, sk = USER#<cognito sub>.
 * Cognito owns credentials; this item holds app-level data (role, display info).
 * Planned: gsi1pk = EMAIL#<email> to detect an address already used by another tenant.
 */
@DynamoDbBean
public class TenantUser extends BaseItem {

    private String userId;
    private String email;
    private String role;
    private Instant createdAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
