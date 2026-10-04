package com.reza.saas.user;

import com.reza.saas.config.AppProperties;
import com.reza.saas.config.DynamoDbClientHolder;
import com.reza.saas.persistence.SingleTableKeys;
import com.reza.saas.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends TenantScopedRepository<TenantUser> {

    public UserRepository(DynamoDbClientHolder clientHolder, AppProperties props) {
        super(clientHolder, props.dynamodb().tableName(), TenantUser.class, SingleTableKeys.USER_PREFIX);
    }

    @Override
    protected String idOf(TenantUser user) {
        return user.getUserId();
    }
}
