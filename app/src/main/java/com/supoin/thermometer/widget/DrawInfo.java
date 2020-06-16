package com.supoin.thermometer.widget;

import android.graphics.Rect;

public class DrawInfo {
    private Rect rect;
    private int color;

    public DrawInfo(Rect rect, int color) {
        this.rect = rect;
        this.color = color;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
