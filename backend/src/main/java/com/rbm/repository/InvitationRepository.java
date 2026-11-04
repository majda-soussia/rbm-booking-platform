package com.rbm.repository;

import com.rbm.entity.Invitation;
import com.rbm.entity.StatutEnvoi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByReservationId(Long reservationId);

    void deleteByReservationId(Long reservationId);

    long countByReservationIdAndStatutEnvoi(Long reservationId, StatutEnvoi statutEnvoi);

    boolean existsByReservationIdAndEmailInviteIgnoreCaseAndStatutEnvoi(
            Long reservationId, String emailInvite, StatutEnvoi statutEnvoi);
}