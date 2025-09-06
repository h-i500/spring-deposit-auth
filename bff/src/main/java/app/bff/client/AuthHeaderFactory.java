package app.bff.client;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class AuthHeaderFactory implements ClientHeadersFactory {
  @Inject SecurityIdentity identity;

  @Override
  public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incoming,
                                               MultivaluedMap<String, String> outgoing) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

    AccessTokenCredential cred = identity.getCredential(AccessTokenCredential.class);
    if (cred != null && cred.getToken() != null && !cred.getToken().isBlank()) {
      headers.add("Authorization", "Bearer " + cred.getToken());
    }
    

    return headers;
  }
}
