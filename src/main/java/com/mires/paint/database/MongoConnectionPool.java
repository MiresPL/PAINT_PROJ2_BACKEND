package com.mires.paint.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConnectionPool {
    @Bean
    public MongoClient mongoClient() {
        final ConnectionString string = new ConnectionString("mongodb://Mires:Mateusz2003%40%24@localhost:27017/paint?authSource=admin");

        final ConnectionPoolSettings poolSettings = ConnectionPoolSettings.builder()
                .maxSize(50)
                .minSize(10)
                .maxWaitTime(1000, TimeUnit.MILLISECONDS)
                .build();

        final MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(string)
                .applyToConnectionPoolSettings(builder -> builder.applySettings(poolSettings))
                .build();


        return MongoClients.create(clientSettings);
    }
}
