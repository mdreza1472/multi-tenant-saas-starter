package com.reza.saas.tenant;

import com.reza.saas.billing.Plan;
import com.reza.saas.persistence.BaseItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

/** Tenant metadata item: pk = TENANT#<id>, sk = META. */
@DynamoDbBean
public class Tenant extends BaseItem {

    private String tenantId;
    private String name;
    private Plan plan;
    private String status;
    private Instant createdAt;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
