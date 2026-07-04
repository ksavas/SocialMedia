package com.socialmedia.post_service.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.socialmedia.post_service.transaction.RetryingMongoTransactionRunner;
import com.socialmedia.post_service.transaction.TransactionRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.time.Duration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    @Value("${spring.data.mongodb.database:post_db}")
    private String database;

    @Value("${spring.data.mongodb.username:social}")
    private String username;

    @Value("${spring.data.mongodb.password:social123}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authDatabase;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    protected boolean autoIndexCreation() {
        return true;
    }

    @Override
    public MongoClient mongoClient() {
        String uri = String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s&directConnection=true",
                username, password, host, port, database, authDatabase);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        return MongoClients.create(settings);
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }

    @Bean
    public TransactionRunner transactionRunner(MongoTransactionManager transactionManager) {
        return new RetryingMongoTransactionRunner(transactionManager, 5, Duration.ofMillis(20));
    }
}
