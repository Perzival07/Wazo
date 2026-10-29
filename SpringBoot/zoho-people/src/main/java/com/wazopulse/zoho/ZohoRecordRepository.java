package com.wazopulse.zoho;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ZohoRecordRepository extends JpaRepository<ZohoRecord, Long> {
    List<ZohoRecord> findByTenantIdAndForm(String tenantId, String form);

    Optional<ZohoRecord> findByTenantIdAndFormAndZohoRecordId(String tenantId, String form, String zohoRecordId);

    boolean existsByTenantIdAndForm(String tenantId, String form);

    @Modifying
    @Transactional
    @Query("delete from ZohoRecord r where r.tenantId = ?1 and r.form = ?2 and r.syncedAt < ?3")
    int deleteStale(String tenantId, String form, Instant cutoff);
}
