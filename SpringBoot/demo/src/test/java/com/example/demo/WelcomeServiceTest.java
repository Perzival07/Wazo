package com.example.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;



public class WelcomeServiceTest {
    @Test 
    void welcomesUserByName(){
        UserRepository fake = new UserRepository() {
            @Override 
            public String findName(int id){return "TestUser"; }
        };

        WelcomeService service = new WelcomeService(fake);
        assertEquals("Welcome, TestUser", service.welcome(1));
    }
}
