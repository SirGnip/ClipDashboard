import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Arrays;
import java.util.List;

/** Local config settings */
class Config {
    public static final boolean DEBUG = false; // toggle debugging (extra logging, etc.)
    public static final int APP_WIDTH = 600;
    public static final int APP_HEIGHT = 700;
    public static final String APP_TITLE = "ClipDashboard";
    public static final List<String> INITIAL_CLIPS = Arrays.asList("abc", "def", "ghijklmnop", "q", "rstuv", "wxyz");
    public static final Paint STATUS_BAR_ERROR_COLOR = Color.RED;
    public static final int BUFFER_CROP_LENGTH = 70;
    public static final int LIST_VIEW_HEIGHT = 250;
    public static final int UI_SPACING = 5;
    public static final int MODIFICATION_TAB_HEIGHT = 120;
    public static final double DRAG_N_DROP_ENTER_OPACITY = 0.2;
    public static final double DRAG_N_DROP_EXIT_OPACITY = 1.0;
    public static final int ARG_WIDTH = 80;

    // diffing
    public static final String DIFF_TEMP_FILE_A = "ClipDashboard_buffA_";
    public static final String DIFF_TEMP_FILE_B = "ClipDashboard_buffB_";
    public static final String DIFF_TEMP_FILE_EXT = ".txt";
    public static final String DIFF_APP = "C:\\Program Files (x86)\\Meld\\Meld.exe";
}
