package com.example.demo;
import org.springframework.stereotype.Repository;



@Repository

public class UserRepository {
    public String findName(int id){
        return "Asha";
    }
}
