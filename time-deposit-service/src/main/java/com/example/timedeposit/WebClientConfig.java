package com.example.timedeposit;

import org.springframework.context.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.reactive.function.client.ClientRequest;


@Configuration
public class WebClientConfig {
  @Bean
  WebClient webClient(WebClient.Builder builder) {
    return builder.filter((request, next) -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth instanceof JwtAuthenticationToken jwt) {
        ClientRequest newReq = ClientRequest.from(request)
            .headers(h -> h.setBearerAuth(jwt.getToken().getTokenValue()))
            .build();
        return next.exchange(newReq);
      }
      return next.exchange(request);
    }).build();
  }
}
