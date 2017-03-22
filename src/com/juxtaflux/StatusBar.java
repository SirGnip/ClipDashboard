package com.juxtaflux;

import javafx.scene.control.Label;
import javafx.scene.paint.Paint;

public class StatusBar extends Label {
    private static Paint defaultColor;

    /** Must call this after primaryStage.show() as the real colors get set at that point. I'm guessing that is when CSS is applied. */
    void cacheTextFillColor() {
        defaultColor = getTextFill();
    }
    void show(String msg) {
        this.setTextFill(defaultColor);
        this.setText(msg);
    }
    void showErr(String msg) {
        this.setTextFill(Config.STATUS_BAR_ERROR_COLOR);
        this.setText("ERROR: " + msg);
    }
}