package com.reza.saas.persistence;

import com.reza.saas.config.DynamoDbClientHolder;
import com.reza.saas.tenant.TenantContext;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for every repository that stores tenant-owned data.
 *
 * THE isolation guarantee of this whole starter lives here: the partition key is
 * ALWAYS built from {@link TenantContext} (populated from the verified JWT), never
 * from a method parameter or request body. A caller therefore cannot ask this
 * class for another tenant's data, even by mistake - there is no argument to get
 * wrong. If you add a new tenant-owned entity, extend this class rather than
 * touching the DynamoDbTable directly.
 *
 * The DynamoDbTable is resolved fresh on every call via DynamoDbClientHolder rather
 * than cached at construction time - caching it would mean this repository keeps
 * using a client DynamoDbClientHolder has already closed after a SnapStart restore.
 * See DynamoDbClientHolder's javadoc.
 *
 * TODO: pagination (listAll currently reads every item under the prefix), and
 * optimistic locking via a version attribute on updates.
 */
public abstract class TenantScopedRepository<T extends BaseItem> {

    private final DynamoDbClientHolder clientHolder;
    private final String tableName;
    private final Class<T> itemType;
    private final String skPrefix;

    protected TenantScopedRepository(DynamoDbClientHolder clientHolder,
                                     String tableName,
                                     Class<T> itemType,
                                     String skPrefix) {
        this.clientHolder = clientHolder;
        this.tableName = tableName;
        this.itemType = itemType;
        this.skPrefix = skPrefix;
    }

    /** The entity's own id, appended to the sort-key prefix (e.g. NOTE# + noteId). */
    protected abstract String idOf(T item);

    public Optional<T> findById(String id) {
        return Optional.ofNullable(table().getItem(keyFor(id)));
    }

    public List<T> listAll() {
        QueryConditional conditional = QueryConditional.sortBeginsWith(
                Key.builder().partitionValue(currentTenantPk()).sortValue(skPrefix).build());
        List<T> results = new ArrayList<>();
        table().query(conditional).items().forEach(results::add);
        return results;
    }

    public T save(T item) {
        // Keys are overwritten unconditionally: whatever the caller put in pk/sk is ignored.
        item.setPk(currentTenantPk());
        item.setSk(skPrefix + idOf(item));
        table().putItem(item);
        return item;
    }

    public void delete(String id) {
        table().deleteItem(keyFor(id));
    }

    private DynamoDbTable<T> table() {
        return clientHolder.enhancedClient().table(tableName, TableSchema.fromBean(itemType));
    }

    private Key keyFor(String id) {
        return Key.builder()
                .partitionValue(currentTenantPk())
                .sortValue(skPrefix + id)
                .build();
    }

    private String currentTenantPk() {
        return SingleTableKeys.tenantPk(TenantContext.requireTenantId());
    }
}