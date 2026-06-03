package com.company.admin.appointment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, Long> {

    Page<AppointmentRecord> findByDeletedFalse(Pageable pageable);

    @EntityGraph(attributePaths = "familyMembers")
    Optional<AppointmentRecord> findByIdAndDeletedFalse(Long id);

    @Query("select coalesce(max(a.displaySequence), 0) from AppointmentRecord a where a.deleted = false")
    long maxDisplaySequence();

    List<AppointmentRecord> findByDeletedFalseOrderByDisplaySequenceAscIdAsc();
}
