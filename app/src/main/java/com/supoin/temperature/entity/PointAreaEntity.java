package com.supoin.temperature.entity;

import java.util.ArrayList;

public class PointAreaEntity {
    public ArrayList<Float> left_top;
    public ArrayList<Float> right_top;
    public ArrayList<Float> left_bottom;
    public ArrayList<Float> right_bottom;
    public ArrayList<Float> center;
    public ArrayList<Float> other;

    public PointAreaEntity() {
        left_top = new ArrayList<>();
        right_top = new ArrayList<>();
        left_bottom = new ArrayList<>();
        right_bottom = new ArrayList<>();
        center = new ArrayList<>();
        other = new ArrayList<>();
    }
}
