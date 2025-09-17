package com.example.timedeposit.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SecurityConfig のユニットテスト。
 *
 * 目的：
 * - extractAuthorities() が realm_access と resource_access の roles を重複なくマージし、
 *   "ROLE_" プレフィックス付きの GrantedAuthority に変換できること
 * - jwtAuthConverter() が上記コンバータを用いて権限を付与できること
 *
 * WebFilterChain 全体の統合テストは本テストでは行わず、純粋にロール抽出のロジックを検証する。
 */
class SecurityConfigTest {

    /**
     * extractAuthorities()：複数ソース（realm/resource）からロールを集約し、重複を排除。
     */
    @Test
    void extractAuthorities_shouldMergeRealmAndResourceRoles_withRolePrefix_andDedup() {
        // realm_access.roles = ["user", "admin"]
        Map<String, Object> realm = Map.of("roles", List.of("user", "admin"));

        // resource_access = { "app1": { roles:["viewer","user"] }, "app2": { roles:["auditor"] } }
        Map<String, Object> resource = Map.of(
                "app1", Map.of("roles", List.of("viewer", "user")),
                "app2", Map.of("roles", List.of("auditor"))
        );

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("realm_access", realm)
                .claim("resource_access", resource)
                .build();

        Collection<GrantedAuthority> authorities = SecurityConfig.extractAuthorities(jwt);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_user", "ROLE_admin", "ROLE_viewer", "ROLE_auditor"
                );
    }

    /**
     * jwtAuthConverter()：Jwt → Authentication 変換時に、extractAuthorities が使われること。
     * 返される Authentication の authorities に "ROLE_xxx" が含まれる。
     */
    @Test
    void jwtAuthConverter_shouldAttachAuthorities() {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationConverter converter = config.jwtAuthConverter();

        Map<String, Object> realm = Map.of("roles", List.of("user"));
        Map<String, Object> resource = Map.of(
                "myclient", Map.of("roles", List.of("editor", "user"))
        );

        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .claim("realm_access", realm)
                .claim("resource_access", resource)
                .build();

        var auth = converter.convert(jwt);
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_user", "ROLE_editor");
    }
}
