package com.reza.saas.config;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Owns the DynamoDB clients' lifecycle across a SnapStart checkpoint/restore cycle.
 *
 * CRaC requires every open socket/connection to be closed before a checkpoint is taken,
 * or the checkpoint fails outright - so beforeCheckpoint() closes the client. afterRestore()
 * then builds a fresh one, which re-resolves credentials rather than reusing whatever was
 * valid at snapshot time (which may be expired by the time a new environment restores).
 *
 * Repositories go through {@link #enhancedClient()} on every call rather than caching the
 * DynamoDbEnhancedClient instance themselves - a cached reference would still point at the
 * client this class closed in beforeCheckpoint, even after afterRestore builds a new one.
 */
public class DynamoDbClientHolder implements Resource {

    private volatile DynamoDbClient client;
    private volatile DynamoDbEnhancedClient enhancedClient;

    public DynamoDbClientHolder() {
        rebuild();
        Core.getGlobalContext().register(this);
    }

    public DynamoDbEnhancedClient enhancedClient() {
        return enhancedClient;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        client.close();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        rebuild();
    }

    private void rebuild() {
        this.client = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        this.enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }
}