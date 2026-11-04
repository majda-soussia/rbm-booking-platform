package com.rbm.service;

import com.rbm.dto.DashboardResponse;
import com.rbm.entity.ERole;
import com.rbm.entity.StatutReservation;
import com.rbm.repository.ReservationRepository;
import com.rbm.repository.SalleRepository;
import com.rbm.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock SalleRepository salleRepository;
    @Mock UserRepository userRepository;
    @Mock ReservationRepository reservationRepository;

    @InjectMocks DashboardService service;

    @Test
    @DisplayName("Les compteurs reflètent les données des repositories")
    void compteurs() {
        when(salleRepository.count()).thenReturn(5L);
        when(salleRepository.countByActiveTrue()).thenReturn(4L);
        when(userRepository.countByRoles_Libelle(ERole.CLIENT)).thenReturn(12L);
        when(reservationRepository.countByStatut(StatutReservation.EN_ATTENTE)).thenReturn(3L);
        when(reservationRepository.countByStatut(StatutReservation.CONFIRMEE)).thenReturn(7L);
        when(reservationRepository.countByStatut(StatutReservation.REFUSEE)).thenReturn(1L);
        when(reservationRepository.countByStatut(StatutReservation.ANNULEE)).thenReturn(2L);
        when(reservationRepository.countByStatutAndDateDebutAfter(eq(StatutReservation.CONFIRMEE), any()))
                .thenReturn(4L);
        when(reservationRepository.sumPrixTotalByStatut(StatutReservation.CONFIRMEE))
                .thenReturn(new BigDecimal("1840.00"));

        DashboardResponse res = service.compteurs();

        assertThat(res.totalSalles()).isEqualTo(5);
        assertThat(res.sallesActives()).isEqualTo(4);
        assertThat(res.totalClients()).isEqualTo(12);
        assertThat(res.reservationsEnAttente()).isEqualTo(3);
        assertThat(res.reservationsConfirmees()).isEqualTo(7);
        assertThat(res.reservationsRefusees()).isEqualTo(1);
        assertThat(res.reservationsAnnulees()).isEqualTo(2);
        assertThat(res.confirmeesAVenir()).isEqualTo(4);
        assertThat(res.chiffreAffaires()).isEqualByComparingTo("1840.00");
    }
}