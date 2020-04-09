package com.srl.polardatacollection;

public class MyEntry {
    public String id;
    public float time;
    public float heartrate;
    public float accelerometerX;
    public float accelerometerY;
    public float accelerometerZ;

    public String get_id() {
        return id;
    }
    public void set_id(String new_id) {
        this.id = new_id;
    }
    public float get_time() {
        return time;
    }
    public void set_time(float new_time) {
        this.time = new_time;
    }
    public float get_heartrate() {
        return heartrate;
    }
    public void set_heartrate(float new_heartrate) {
        this.heartrate = new_heartrate;
    }
    public float get_accelerometerX() {
        return accelerometerX;
    }
    public void set_accelerometerX(float new_accelerometerX) {
        this.accelerometerX = new_accelerometerX;
    }
    public float get_accelerometerY() {
        return accelerometerY;
    }
    public void set_accelerometerY(float new_accelerometerY) {
        this.accelerometerY = new_accelerometerY;
    }
    public float get_accelerometerZ() {
        return accelerometerZ;
    }
    public void set_accelerometerZ(float new_accelerometerZ) {
        this.accelerometerZ = new_accelerometerZ;
    }
}
