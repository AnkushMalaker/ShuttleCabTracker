package com.example.shuttlecabtracker;

public class shuttleCabs {
    String cabID;
    double cabx;
    double caby;
    int full;
    int type;
    public shuttleCabs(){

    }

    public shuttleCabs(String cabID, double cabx, double caby,int full, int type) {
        this.cabID = cabID;
        this.cabx = cabx;
        this.caby = caby;
        this.full = full;
        this.type = type;
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

    public int getType() {
        return type;
    }

    public int getFull(){return full;}
}
