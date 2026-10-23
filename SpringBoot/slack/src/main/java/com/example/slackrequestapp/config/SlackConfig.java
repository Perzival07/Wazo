package com.example.slackrequestapp.config;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {

    @Bean
    public MethodsClient slackClient(@Value("${slack.bot-token}") String token) {
        return Slack.getInstance().methods(token);
    }
}