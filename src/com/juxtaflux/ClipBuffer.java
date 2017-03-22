package com.juxtaflux;

import org.apache.commons.lang3.StringUtils;

class ClipBuffer {
    String clip;
    ClipBuffer(String msg) {
        clip = msg;
    }

    /** Reformat string for display in ListView. If it is too long, truncate it. If it has multiple lines, display char/line summary. */
    public String toString() {
        int lineCount = StringUtils.countMatches(clip, System.lineSeparator()) + 1;
        String formatted = clip.replace(System.lineSeparator(), " ");
        if (formatted.length() > Config.BUFFER_CROP_LENGTH || lineCount > 1) {
            String suffix = formatted.length() > Config.BUFFER_CROP_LENGTH ? "..." : "";
            formatted = formatted.format("%s%s (%d chars, %d lines)", formatted.substring(0, Math.min(formatted.length(), Config.BUFFER_CROP_LENGTH)), suffix, formatted.length(), lineCount);
        }
        return formatted;
    }
}