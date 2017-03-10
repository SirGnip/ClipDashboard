import org.apache.commons.lang3.StringUtils;

class ClipBuffer {
    String clip;
    ClipBuffer(String msg) {
        clip = msg;
    }
    public String toString() {
        int lineCount = StringUtils.countMatches(clip, System.lineSeparator()) + 1;
        String formatted = clip.replace(System.lineSeparator(), " ");
        if (formatted.length() > Config.BUFFER_CROP_LENGTH) {
            formatted = formatted.format("%s... (%d chars, %d lines)", formatted.substring(0, Config.BUFFER_CROP_LENGTH), formatted.length(), lineCount);
        }
        return formatted;
    }
}