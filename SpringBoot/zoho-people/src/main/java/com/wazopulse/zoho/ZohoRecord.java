package com.wazopulse.zoho;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Local copy of one Zoho People record (any form), stored as the JSON Zoho returned. */
@Entity
@Table(name = "zoho_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "form", "zoho_record_id"}))
public class ZohoRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(nullable = false, length = 64) private String form;
    @Column(name = "zoho_record_id", nullable = false) private String zohoRecordId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false) private String data;

    @Column(nullable = false) private Instant syncedAt;

    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { tenantId = v; }
    public String getForm() { return form; }
    public void setForm(String v) { form = v; }
    public String getZohoRecordId() { return zohoRecordId; }
    public void setZohoRecordId(String v) { zohoRecordId = v; }
    public String getData() { return data; }
    public void setData(String v) { data = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { syncedAt = v; }
}


