package com.example.demo;

import org.springframework.stereotype.Service;

@Service
public class WelcomeService {
    
    
    private final UserRepository repo;

    public WelcomeService(UserRepository repo){
        this.repo = repo;
    }

    public String welcome(int id){
        return "Welcome, " + repo.findName(id);
    }


}
