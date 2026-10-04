package com.reza.saas.tenant;

import com.reza.saas.config.AppProperties;
import com.reza.saas.config.DynamoDbClientHolder;
import com.reza.saas.persistence.SingleTableKeys;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.Optional;

/**
 * The tenant META item has a fixed sort key (not a prefix), so it doesn't fit
 * {@code TenantScopedRepository}. Reads are still scoped through TenantContext.
 *
 * The DynamoDbTable is resolved fresh on every call via DynamoDbClientHolder rather
 * than cached at construction time - see DynamoDbClientHolder's javadoc and the same
 * fix in TenantScopedRepository.
 */
@Repository
public class TenantRepository {

    private final DynamoDbClientHolder clientHolder;
    private final String tableName;

    public TenantRepository(DynamoDbClientHolder clientHolder, AppProperties props) {
        this.clientHolder = clientHolder;
        this.tableName = props.dynamodb().tableName();
    }

    public Optional<Tenant> findCurrent() {
        Key key = Key.builder()
                .partitionValue(SingleTableKeys.tenantPk(TenantContext.requireTenantId()))
                .sortValue(SingleTableKeys.TENANT_META_SK)
                .build();
        return Optional.ofNullable(table().getItem(key));
    }

    /**
     * INTENTIONAL bypass of TenantContext (one of exactly two - the other is the Stripe
     * webhook): provisioning runs before any tenant exists, so the id comes from the
     * argument. The conditional put refuses to overwrite an existing tenant.
     */
    public void create(Tenant tenant) {
        tenant.setPk(SingleTableKeys.tenantPk(tenant.getTenantId()));
        tenant.setSk(SingleTableKeys.TENANT_META_SK);
        table().putItem(PutItemEnhancedRequest.builder(Tenant.class)
                .item(tenant)
                .conditionExpression(Expression.builder().expression("attribute_not_exists(pk)").build())
                .build());
    }

    private DynamoDbTable<Tenant> table() {
        return clientHolder.enhancedClient().table(tableName, TableSchema.fromBean(Tenant.class));
    }
}