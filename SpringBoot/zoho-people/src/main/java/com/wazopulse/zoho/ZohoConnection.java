package com.wazopulse.zoho;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "zoho_connections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
public class ZohoConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false) private String tenantId;
    @Column(name = "user_id", nullable = false) private String userId;

    @Column(nullable = false, length = 2048) private String refreshTokenEnc;
    @Column(nullable = false) private String accountsServer;
    @Column(nullable = false) private String peopleDomain;
    @Column(nullable = false, length = 8) private String dataCenter;
    @Column(nullable = false) private String zohoEmail;
    private String zohoRecordId;
    @Column(length = 1024) private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private ConnectionStatus status = ConnectionStatus.CONNECTED;

    private Instant connectedAt;
    private Instant lastSyncAt;

    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant v) { lastSyncAt = v; }
    public Long getId() { return id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String v) { tenantId = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { userId = v; }
    public String getRefreshTokenEnc() { return refreshTokenEnc; }
    public void setRefreshTokenEnc(String v) { refreshTokenEnc = v; }
    public String getAccountsServer() { return accountsServer; }
    public void setAccountsServer(String v) { accountsServer = v; }
    public String getPeopleDomain() { return peopleDomain; }
    public void setPeopleDomain(String v) { peopleDomain = v; }
    public String getDataCenter() { return dataCenter; }
    public void setDataCenter(String v) { dataCenter = v; }
    public String getZohoEmail() { return zohoEmail; }
    public void setZohoEmail(String v) { zohoEmail = v; }
    public String getZohoRecordId() { return zohoRecordId; }
    public void setZohoRecordId(String v) { zohoRecordId = v; }
    public String getScopes() { return scopes; }
    public void setScopes(String v) { scopes = v; }
    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus v) { status = v; }
    public Instant getConnectedAt() { return connectedAt; }
    public void setConnectedAt(Instant v) { connectedAt = v; }
}
