package app.bff.debug;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * Vert.x Mutiny WebClient を CDI で注入できるようにする Producer。
 */
@ApplicationScoped
public class WebClientProducer {

    @Inject
    Vertx vertx; // Mutiny 版 Vertx

    @Produces
    @ApplicationScoped
    public WebClient webClient() {
        return WebClient.create(vertx); // Mutiny 版を生成
    }
}
