package com.rbm.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";

    private final JwtService service = new JwtService(SECRET, 60_000);

    @Test
    @DisplayName("Un token généré est valide et contient l'email et les rôles")
    void genereEtLit() {
        String token = service.generateToken("admin@rbm.com", List.of("ADMIN"));

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractEmail(token)).isEqualTo("admin@rbm.com");
        assertThat(service.extractRoles(token)).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("Un token dont le contenu est modifié (CLIENT -> ADMIN) est refusé")
    void tokenFalsifie() {
        String token = service.generateToken("client@rbm.com", List.of("CLIENT"));
        String autre = service.generateToken("client@rbm.com", List.of("ADMIN"));

        String[] a = token.split("\\.");   // header . payload . signature
        String[] b = autre.split("\\.");

        // payload ADMIN avec la signature du token CLIENT
        String falsifie = a[0] + "." + b[1] + "." + a[2];

        assertThat(service.isValid(falsifie)).isFalse();
    }

    @Test
    @DisplayName("Un token signé avec un autre secret est refusé")
    void autreSecret() {
        JwtService autre = new JwtService("another-secret-key-with-at-least-32-chars-xx", 60_000);
        String token = autre.generateToken("admin@rbm.com", List.of("ADMIN"));
        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Un token expiré est refusé")
    void tokenExpire() {
        JwtService court = new JwtService(SECRET, -1000);
        String token = court.generateToken("admin@rbm.com", List.of("ADMIN"));
        assertThat(court.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Un secret trop court est refusé au démarrage")
    void secretTropCourt() {
        assertThatThrownBy(() -> new JwtService("court", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Une chaîne quelconque n'est pas un token valide")
    void chaineInvalide() {
        assertThat(service.isValid("n'importe quoi")).isFalse();
    }
}