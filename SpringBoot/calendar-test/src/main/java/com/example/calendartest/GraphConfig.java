package com.example.calendartest;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    @Bean
    public GraphServiceClient graphServiceClient(
            @Value("${graph.client-id}") String clientId,
            @Value("${graph.tenant-id}") String tenantId) {

        DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
                .clientId(clientId)
                .tenantId(tenantId)
                .challengeConsumer(challenge -> System.out.println(challenge.getMessage()))
                .build();

        return new GraphServiceClient(credential, "Calendars.ReadWrite");
    }
}