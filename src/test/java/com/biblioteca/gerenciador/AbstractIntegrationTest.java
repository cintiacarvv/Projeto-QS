package com.biblioteca.gerenciador;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractIntegrationTest {

    // Define o container estático para iniciar uma única vez por execução de testes (JVM)
    static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0.4"));
        mongoDBContainer.start();
    }

    // Registra a URL de conexão mudando dinamicamente apenas uma vez
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}