package com.rbm.service;

import com.rbm.dto.DashboardResponse;
import com.rbm.entity.ERole;
import com.rbm.entity.StatutReservation;
import com.rbm.repository.ReservationRepository;
import com.rbm.repository.SalleRepository;
import com.rbm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final SalleRepository salleRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    public DashboardResponse compteurs() {
        return new DashboardResponse(
                salleRepository.count(),
                salleRepository.countByActiveTrue(),
                userRepository.countByRoles_Libelle(ERole.CLIENT),
                reservationRepository.countByStatut(StatutReservation.EN_ATTENTE),
                reservationRepository.countByStatut(StatutReservation.CONFIRMEE),
                reservationRepository.countByStatut(StatutReservation.REFUSEE),
                reservationRepository.countByStatut(StatutReservation.ANNULEE),
                reservationRepository.countByStatutAndDateDebutAfter(StatutReservation.CONFIRMEE, LocalDateTime.now()),
                reservationRepository.sumPrixTotalByStatut(StatutReservation.CONFIRMEE));
    }
}