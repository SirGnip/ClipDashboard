package com.juxtaflux;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Arrays;
import java.util.List;

/** Local config settings */
class Config {
    static final boolean DEBUG = false; // toggle debugging (extra logging, etc.)
    static final int APP_WIDTH = 700;
    static final int APP_HEIGHT = 700;
    static final String APP_TITLE = "ClipDashboard";
    static final List<String> INITIAL_CLIPS = Arrays.asList("abc", "def", "ghijklmnop", "q", "rstuv", "wxyz");
    static final Paint STATUS_BAR_ERROR_COLOR = Color.RED;
    static final int BUFFER_CROP_LENGTH = 70;
    static final double DRAG_N_DROP_ENTER_OPACITY = 0.2;
    static final double DRAG_N_DROP_EXIT_OPACITY = 1.0;
    static final int WORDS_FOR_FILE_NAMING = 4;

    // diffing
    static final String TEMP_FILE_EXT = ".txt";
    static final String DIFF_TEMP_FILE_A = "ClipDashboard_buffA_";
    static final String DIFF_TEMP_FILE_B = "ClipDashboard_buffB_";
    static final String VIEW_APP = "notepad";
    static final String VIEW_TEMP_FILE = "ClipDashboard_notepad_";
    static final String DIFF_APP = "C:\\Program Files (x86)\\Meld\\Meld.exe";
}
