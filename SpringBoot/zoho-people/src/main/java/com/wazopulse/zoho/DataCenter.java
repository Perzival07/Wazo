package com.wazopulse.zoho;

/** Zoho data centers. The OAuth "location" value in the callback decides which one a user is on. */
public enum DataCenter {
    US("us", "https://accounts.zoho.com", "https://people.zoho.com"),
    EU("eu", "https://accounts.zoho.eu", "https://people.zoho.eu"),
    IN("in", "https://accounts.zoho.in", "https://people.zoho.in"),
    AU("au", "https://accounts.zoho.com.au", "https://people.zoho.com.au"),
    JP("jp", "https://accounts.zoho.jp", "https://people.zoho.jp"),
    CA("ca", "https://accounts.zohocloud.ca", "https://people.zohocloud.ca"),
    CN("cn", "https://accounts.zoho.com.cn", "https://people.zoho.com.cn");

    private final String code;
    private final String accountsServer;
    private final String peopleBase;

    DataCenter(String code, String accountsServer, String peopleBase) {
        this.code = code;
        this.accountsServer = accountsServer;
        this.peopleBase = peopleBase;
    }

    public String code() { return code; }
    public String accountsServer() { return accountsServer; }
    public String peopleBase() { return peopleBase; }

    /** Missing location means the default (US) data center. Unknown values are rejected, never trusted. */
    public static DataCenter fromLocation(String location) {
        if (location == null || location.isBlank()) return US;
        for (DataCenter d : values()) {
            if (d.code.equalsIgnoreCase(location.trim())) return d;
        }
        throw new ZohoExceptions.OAuthCallback("unsupported_data_center");
    }
}
