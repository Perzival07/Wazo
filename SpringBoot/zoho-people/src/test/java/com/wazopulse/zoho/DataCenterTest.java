package com.wazopulse.zoho;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DataCenterTest {
    @Test
    void mapsIndia() {
        DataCenter dc = DataCenter.fromLocation("in");
        assertEquals("https://accounts.zoho.in", dc.accountsServer());
        assertEquals("https://people.zoho.in", dc.peopleBase());
    }

    @Test
    void caseInsensitiveAndDefaultsToUs() {
        assertEquals(DataCenter.EU, DataCenter.fromLocation("EU"));
        assertEquals(DataCenter.US, DataCenter.fromLocation(null));
        assertEquals(DataCenter.US, DataCenter.fromLocation(" "));
    }

    @Test
    void unknownLocationRejected() {
        assertThrows(ZohoExceptions.OAuthCallback.class, () -> DataCenter.fromLocation("evil.example"));
    }
}
