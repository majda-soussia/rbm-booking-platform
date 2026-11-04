package com.rbm.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails client() {
        return org.springframework.security.core.userdetails.User
                .withUsername("client@rbm.com").password("x").authorities("ROLE_CLIENT").build();
    }

    private MockHttpServletRequest requeteAvec(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    @DisplayName("Token valide : l'utilisateur est authentifié avec ses rôles")
    void tokenValide() throws Exception {
        String token = jwtService.generateToken("client@rbm.com", List.of("CLIENT"));
        when(userDetailsService.loadUserByUsername("client@rbm.com")).thenReturn(client());
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requeteAvec("Bearer " + token), new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("client@rbm.com");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CLIENT");
        assertThat(chain.getRequest()).isNotNull();   // la requête a continué son chemin
    }

    @Test
    @DisplayName("Sans en-tête Authorization : requête anonyme")
    void sansEntete() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requeteAvec(null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token invalide : requête anonyme, base non sollicitée")
    void tokenInvalide() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requeteAvec("Bearer n-importe-quoi"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("Token valide mais compte supprimé : requête anonyme")
    void compteSupprime() throws Exception {
        String token = jwtService.generateToken("fantome@rbm.com", List.of("CLIENT"));
        when(userDetailsService.loadUserByUsername("fantome@rbm.com"))
                .thenThrow(new UsernameNotFoundException("introuvable"));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(requeteAvec("Bearer " + token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}