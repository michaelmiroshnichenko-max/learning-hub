package dev.learninghub.shared;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/info")
public class InfoResource {

    public record Info(String name, String version) {
    }

    private final Info info;

    public InfoResource(
            @ConfigProperty(name = "quarkus.application.name") String name,
            @ConfigProperty(name = "quarkus.application.version") String version) {
        this.info = new Info(name, version);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Info get() {
        return info;
    }
}
