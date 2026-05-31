package com.company.admin.file;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SysFileRepository extends JpaRepository<SysFile, Long> {

    boolean existsByIdAndBusinessTypeAndDeletedFalse(Long id, String businessType);
}
