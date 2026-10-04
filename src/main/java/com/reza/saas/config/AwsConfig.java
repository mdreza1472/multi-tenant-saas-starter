package com.reza.saas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS client wiring. Region and credentials come from the default provider
 * chains (AWS_REGION and the Lambda execution role in AWS; your local profile
 * when run from an IDE).
 *
 * The actual DynamoDB clients live in DynamoDbClientHolder, not here, because they
 * must be closed before a SnapStart checkpoint and rebuilt after restore - see that
 * class's javadoc. This class just registers the holder as a bean.
 */
@Configuration
public class AwsConfig {

    @Bean
    DynamoDbClientHolder dynamoDbClientHolder() {
        return new DynamoDbClientHolder();
    }
}