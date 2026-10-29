package com.wazopulse.zoho;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZohoConnectionRepository extends JpaRepository<ZohoConnection, Long> {
    Optional<ZohoConnection> findByTenantIdAndUserId(String tenantId, String userId);
}
