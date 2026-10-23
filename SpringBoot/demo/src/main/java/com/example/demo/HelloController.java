package com.example.demo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class HelloController {

    private final WelcomeService service;

    public HelloController(WelcomeService service){
        this.service = service;
    }
    

    @GetMapping("/hello")
    public String hello(){
        return "Hello, Spring Boot!";
    }

    @GetMapping("/welcome/{id}")
    public String welcome(@PathVariable int id){
        return service.welcome(id);                                                                                                                                                                                                                                                                                                                                                                      
    }                                        
    

}
