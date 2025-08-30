package app.bff.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@RegisterRestClient(configKey = "savings")
@RegisterClientHeaders(AuthHeaderFactory.class)  // ← これで Bearer を自動付与
@Path("/accounts")
public interface SavingsClient {
  @GET @Path("/{id}")
  String get(@PathParam("id") String id);
}
