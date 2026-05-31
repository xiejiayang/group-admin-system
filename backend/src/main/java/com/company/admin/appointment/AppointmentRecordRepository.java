package com.company.admin.appointment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, Long> {

    Page<AppointmentRecord> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = "familyMembers")
    Optional<AppointmentRecord> findByIdAndDeletedFalse(Long id);
}
