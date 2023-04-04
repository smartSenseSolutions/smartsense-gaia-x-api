package com.smartsense.gaiax.utils;

/**
 * @author Nitin
 * @version 1.0
 */
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App{
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Load config from YAML file
        ConfigStoreOptions file = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", "config.yaml"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(file);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        retriever.getConfig().onSuccess(config -> {
            SDSigner SDSigner = new SDSigner(
                    vertx,
                    config.getString("verification_method"),
                    config.getString("x5u_url"),
                    config.getString("api_version"),
                    config.getString("base_url"),
                    config.getString("private_key")
            );
            JsonObject sd = vertx.fileSystem().readFileBlocking("self-description.json").toJsonObject();
            SDSigner.start(sd).onComplete(ar -> {
                LOGGER.info("Process Finished");
                System.exit(0);
            });
        });
    }
}