package com.asp4rever.ticketbuyer;

public class Passenger {
    private final String surName;
    private final String name;
    private final boolean child;
    private final int place;

    Passenger(String surName, String name, boolean child, int place) {
        this.surName = surName;
        this.name = name;
        this.child = child;
        this.place = place;
    }

    public String getSurName() {
        return surName;
    }

    public String getName() {
        return name;
    }

    public int getPlace() {
        return place;
    }

    public boolean isChild() {
        return child;
    }
}
