package com.example.shuttlecabtracker;

public class shuttleCabs {
    String cabID;
    double cabx;
    double caby;
    int cabpax;

    public shuttleCabs(){

    }

    public shuttleCabs(String cabID, double cabx, double caby, int cabpax) {
        this.cabID = cabID;
        this.cabx = cabx;
        this.caby = caby;
        this.cabpax = cabpax;
    }

    public String getCabID() {
        return cabID;
    }

    public double getCabx() {
        return cabx;
    }

    public double getCaby() {
        return caby;
    }

    public int getCabpax() {
        return cabpax;
    }
}
