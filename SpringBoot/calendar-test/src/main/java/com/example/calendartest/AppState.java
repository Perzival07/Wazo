package com.example.calendartest;

import org.springframework.stereotype.Component;

@Component
public class AppState {


    private volatile boolean singedIn = false;

    public boolean isSignedIn() {return singedIn;}
    public void setSignedIn(boolean v) {this.singedIn = v; }


}
