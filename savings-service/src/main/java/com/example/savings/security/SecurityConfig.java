package com.example.savings.security;

import java.util.*;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize を有効化（Controllerの hasRole('user'|'read') が効く）
public class SecurityConfig {

  // Keycloak の clientId（resource_access.<clientId>.roles を読むときに使用）
  private static final String RESOURCE_CLIENT_ID = "savings-service";

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .exceptionHandling(ex -> ex
        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) // 未認証→401
        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())            // 権限不足→403
      )
      .authorizeHttpRequests(auth -> auth
        // 公開してよいものがあればここで permitAll
        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()

        // Controller無注釈の検索APIも read ロール必須に
        // .requestMatchers(HttpMethod.GET, "/api/accounts/search").hasRole("read")
        // まずは認証のみ
        .requestMatchers(HttpMethod.GET, "/accounts").authenticated() // まずは認証のみ

        // それ以外は認証必須（/accounts は @PreAuthorize が二重で守る）
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
      );

    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    return converter;
  }

  @SuppressWarnings("unchecked")
  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    // realm_access.roles
    List<String> realmRoles = Optional.ofNullable((Map<String, Object>) jwt.getClaims().get("realm_access"))
        .map(m -> (List<String>) m.get("roles"))
        .orElseGet(List::of);

    // resource_access.<clientId>.roles
    List<String> resourceRoles = Optional.ofNullable((Map<String, Object>) jwt.getClaims().get("resource_access"))
        .map(m -> (Map<String, Object>) m.get(RESOURCE_CLIENT_ID))
        .map(m -> (List<String>) m.get("roles"))
        .orElseGet(List::of);

    // 任意: scope/scope(scp) を SCOPE_ で付与したい場合
    List<String> scopes = Optional.ofNullable((Object) jwt.getClaims().get("scope"))
        .map(Object::toString)
        .map(s -> Arrays.asList(s.split(" ")))
        .orElseGet(() -> (List<String>) Optional.ofNullable((List<String>) jwt.getClaims().get("scp")).orElseGet(List::of));

    Stream<GrantedAuthority> roleAuthorities =
        Stream.concat(realmRoles.stream(), resourceRoles.stream())
              .map(r -> new SimpleGrantedAuthority("ROLE_" + r)); // hasRole('user') に対応

    Stream<GrantedAuthority> scopeAuthorities =
        scopes.stream().map(s -> new SimpleGrantedAuthority("SCOPE_" + s));

    List<GrantedAuthority> authorities = new ArrayList<>();
    roleAuthorities.forEach(authorities::add);
    scopeAuthorities.forEach(authorities::add);
    return authorities;
  }
}
