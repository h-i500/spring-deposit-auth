// package com.example.savings.security;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
// import org.springframework.security.web.SecurityFilterChain;

// import java.util.*;
// import java.util.stream.Collectors;

// @Configuration
// @EnableMethodSecurity
// public class SecurityConfig {

//     @Bean
//     SecurityFilterChain security(HttpSecurity http) throws Exception {
//         http.csrf(csrf -> csrf.disable());
//         http.authorizeHttpRequests(auth -> auth
//             .requestMatchers("/actuator/**").permitAll()
//             .anyRequest().authenticated()
//         );
//         http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
//             .jwtAuthenticationConverter(jwtAuthConverter())   // ← ここだけ呼ぶ
//         ));
//         return http.build();
//     }

//     // ★ Converter を「ラムダ @Bean」ではなく、明示クラスで提供する
//     @Bean
//     JwtAuthenticationConverter jwtAuthConverter() {
//         JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
//         conv.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
//         return conv;
//     }

//     // Keycloak の realm ロール → GrantedAuthority へ変換
//     private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
//         Map<String, Object> realmAccess = jwt.getClaim("realm_access");
//         Collection<String> roleNames = Optional.ofNullable(realmAccess)
//             .map(m -> m.get("roles"))
//             .filter(Collection.class::isInstance)
//             .map(c -> (Collection<?>) c)
//             .orElseGet(List::of)
//             .stream()
//             .map(Object::toString)
//             .collect(Collectors.toUnmodifiableList());

//         return roleNames.stream()
//             .map(r -> "ROLE_" + r)
//             .map(SimpleGrantedAuthority::new)
//             .collect(Collectors.toUnmodifiableSet());
//     }
// }
