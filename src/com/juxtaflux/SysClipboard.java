package com.juxtaflux;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

/** Interact with contents of system clipboard */
public class SysClipboard {
    public static String read() {
        return Clipboard.getSystemClipboard().getString();
    }

    public static List<String> readAsLines() {
        String[] array = StringUtils.splitByWholeSeparatorPreserveAllTokens(SysClipboard.read(), System.lineSeparator());
        return Arrays.asList(array);
    }

    public static void write(String s) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(s);
        clipboard.setContent(content);
    }
}
