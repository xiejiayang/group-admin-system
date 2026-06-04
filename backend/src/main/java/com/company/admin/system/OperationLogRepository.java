package com.company.admin.system;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findByOperatorDepartmentName(String departmentName);
}
