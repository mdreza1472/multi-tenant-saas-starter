package com.reza.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for local runs (start this class from your IDE with AWS_REGION,
 * TABLE_NAME and COGNITO_USER_POOL_ID set). In AWS, Lambda enters through
 * {@link StreamLambdaHandler} instead, which boots this same application class.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SaasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasApplication.class, args);
    }
}
