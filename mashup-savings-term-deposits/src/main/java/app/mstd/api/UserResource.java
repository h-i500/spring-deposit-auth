
package app.mstd.api;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Map;

@Path("/api")
public class UserResource {
  @Inject SecurityIdentity identity;

  @GET @Path("/me") @Authenticated
  public Map<String, Object> me() {
    return Map.of(
      "principal", identity.getPrincipal().getName(),
      "roles", identity.getRoles()
    );
  }
}
