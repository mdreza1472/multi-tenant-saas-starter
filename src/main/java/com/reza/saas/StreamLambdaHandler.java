package com.reza.saas;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lambda entry point: translates API Gateway HTTP API (payload v2) events into
 * servlet requests for the Spring Boot app, and the responses back again.
 *
 * The Spring context is built in the static initializer below, i.e. during Lambda's
 * INIT phase. SnapStart (enabled on the "live" alias in the CDK stack) snapshots the
 * environment after that phase, so cold starts restore an already-initialised context
 * instead of rebuilding it. That only pays off while the context is built eagerly:
 * keep spring.main.lazy-initialization=false, or beans get created on the first
 * request, after the snapshot.
 *
 * Because one snapshot is restored into many execution environments, don't caches
 * anything unique or time-sensitive during initialisation (random seeds, timestamps,
 * open connections).
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER;

    static {
        try {
            HANDLER = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(SaasApplication.class);
        } catch (ContainerInitializationException e) {
            // Fail the cold start loudly - a half-initialised container is worse than none.
            throw new IllegalStateException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        HANDLER.proxyStream(input, output, context);
    }
}
