package com.juxtaflux;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


class ArgParseError extends Exception {
    ArgParseError(String msg) { super(msg); }
}

/** Debugging functions */
class Debug {
    static void dumpDragboard(Dragboard b) {
        System.out.printf("Dragboard %s %s %s %s %s %s\n",
                b.hasString(),
                b.hasUrl(),
                b.hasFiles(),
                b.hasHtml(),
                b.hasRtf(),
                b.hasImage()
        );
        b.getContentTypes().forEach(df -> System.out.println("MIMETYPE: " + df.getClass().getName() + " " + df + " - " + b.getContent(df).getClass().getName() + " >" + b.getContent(df))); // reference: http://stackoverflow.com/questions/30923817/javafx-dnd-third-party-program-to-javafx-app
    }

    /** dump each character of a string, its index, and its ascii value */
    static void dumpString(String s) {
        // 10 LF (\n)    13 CR (\r)
        // Unix and modern Mac's   :   LF     (\n)
        // Windows                 :   CR LF  (\r\n)
        System.out.println("length " + s.length());
        System.out.println(s);
        for (int i = 0; i < s.length(); ++i) {
            System.out.println(i + "> " + ((int) s.charAt(i)) + " " + s.charAt(i));
        }
    }
}

/** generic utility functions */
class Functions {
    private static Integer parseToken(String token) throws ArgParseError {
        if (token.trim().equals("")) {
            return null;
        }

        try {
            return Integer.valueOf(token);
            // there is a difference between a parsing error and no value existing
            // a parse error is an error. An empty string means NULL.
        } catch (NumberFormatException exc) {
            throw new ArgParseError("Could not parse \"" + token + "\" into an integer");
        }
    }

    static Integer[] parseSliceSyntax(String sliceExpr) throws ArgParseError {
        int colonCount = StringUtils.countMatches(sliceExpr, ":");
        Integer singleIdx = null;
        Integer startIdx = null;
        Integer endIdx = null;
        if (colonCount == 0) {
            singleIdx = parseToken(sliceExpr);
            if (singleIdx == null) {
                throw new ArgParseError("The single value slice argument must be an integer");
            }
        } else if (colonCount == 1) {
            String[] tokens = StringUtils.splitByWholeSeparatorPreserveAllTokens(sliceExpr, ":");
            startIdx = parseToken(tokens[0]);
            endIdx = parseToken(tokens[1]);
        } else {
            throw new ArgParseError("Too many colons (" + colonCount + ") in slice argument: \"" + sliceExpr + "\"");
        }
        Integer[] idxs = {singleIdx, startIdx, endIdx};
        return idxs;
    }
    static Path writeToTempFile(String prefix, String suffix, byte[] text) {
        Path result = null;
        try {
            result = Files.createTempFile(prefix, suffix);
            result.toFile().deleteOnExit(); // register file to be deleted when app exits
            // NOTE: Files.write() writes the file with linux-style line endings. Or, maybe it just passes
            // through whatever is in the String is and doesn't automatically treat "\n" as "\r\n" on Windows.
            Files.write(result, text);
        } catch(Exception exc) {
            result = null;
        }
        return result;
    }
}



public class Controller implements Initializable {
    @FXML
    private ListView<ClipBuffer> buffers;
    private ObservableList<ClipBuffer> clips = FXCollections.observableArrayList(Config.INITIAL_CLIPS.stream().map(c -> new ClipBuffer(c)).collect(Collectors.toList()));
    @FXML
    private Button btnRetrieve;
    @FXML
    private TextArea log;
    @FXML
    private StatusBar statusBar;

    @FXML
    private CheckMenuItem chkStoreOnFocus;
    @FXML
    private CheckMenuItem chkRetrieveOnFocus;
    @FXML
    private CheckMenuItem chkVariableSubstitution;

    @FXML
    private TextField txtArg1;
    @FXML
    private TextField txtArg2;
    @FXML
    private Text txtHelpHeader;
    @FXML
    private Text txtHelpBody;

    private HashMap<String, ButtonSetup> buttonMap = new HashMap();
    Stage primaryStage;

    class ButtonSetup {
        public List<TextField> disable;
        public String helpHeader;
        public String helpBody;

        public ButtonSetup(List<TextField> disable, String helpHeader, String helpBody) {
            this.disable = disable;
            this.helpHeader = helpHeader;
            this.helpBody = helpBody;
        }
    }

    public void onMouseEnter(MouseEvent e) {
        Button b = (Button) e.getSource();
        if (! buttonMap.containsKey(b.getId())) { throw new RuntimeException("Control ID doesn't exist in map: " + b.getId()); };
        ButtonSetup stuff = buttonMap.get(b.getId());
        for (TextField f : stuff.disable) {
            f.setDisable(true);
        }
        txtHelpHeader.setText(stuff.helpHeader + "\n");
        txtHelpBody.setText(stuff.helpBody);
    }
    public void onMouseExit(MouseEvent e) {
        Button b = (Button) e.getSource();
        if (! buttonMap.containsKey(b.getId())) { throw new RuntimeException("Control ID doesn't exist in map: " + b.getId()); };
        ButtonSetup stuff = buttonMap.get(b.getId());
        for (TextField f : stuff.disable) {
            f.setDisable(false);
        }
        txtHelpHeader.setText("\n");
        txtHelpBody.setText("");
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buffers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        buffers.setItems(clips);
        statusBar.cacheTextFillColor();

        buttonMap.put("btnStore", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Store", "Add contents of clipboard to buffer"));
        buttonMap.put("btnReplace", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Replace", "Replace selected buffer with current clipboard contents"));
        buttonMap.put("btnPrepend", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Prepend", "Prepend clipboard contents to the beginning of each selected buffer"));
        buttonMap.put("btnAppend", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Append", "Append clipboard contents to the end of each selected buffer"));
        buttonMap.put("btnJoin", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Join", "Join selected buffers with newlines"));
        buttonMap.put("btnDiff", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Diff", "Diff two selected buffers"));
        buttonMap.put("btnUp", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Up", "Move selected buffers up"));
        buttonMap.put("btnDown", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Down", "Move selected buffers down"));

        buttonMap.put("btnStrLTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "String: Left trim", "Trim whitespace off the left side of the clipboard"));
        buttonMap.put("btnStrTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "String: Trim", "Trim whitespace off the left and right side of the clipboard"));
        buttonMap.put("btnStrRTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "String: Right trim", "Trim whitespace off the right side of the clipboard"));
        buttonMap.put("btnStrLower", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "String: Lowercase", "Convert clipboard to lowercase"));
        buttonMap.put("btnStrUpper", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "String: Uppercase", "Convert clipboard to uppercase"));
        buttonMap.put("btnStrPrepend", new ButtonSetup(Arrays.asList(txtArg2), "String: Prepend string", "Prepend text (arg1) to start of clipboard"));
        buttonMap.put("btnStrAppend", new ButtonSetup(Arrays.asList(txtArg2), "String: Append string", "Append text (arg1) to end of clipboard"));
        buttonMap.put("btnStrWordWrap", new ButtonSetup(Arrays.asList(txtArg2), "String: Word wrap", "Wrap the clipboard to the given width (arg1). If a line goes too long with no whitespace, it will not be truncated."));
        buttonMap.put("btnStrSplit", new ButtonSetup(Arrays.asList(txtArg2), "String: Split", "Split clipboard into multiple lines on given string (arg1)"));
        buttonMap.put("btnStrReplace", new ButtonSetup(Arrays.asList(), "String: Replace", "Replace all text that matches the search text (arg1), with the replacement string (arg2)"));
        buttonMap.put("btnStrRegexRepl", new ButtonSetup(Arrays.asList(), "String: Replace via regex", "Replace all text that matches the regex (arg1) with the replacement string (arg2). Supports backreferences in replacement string.\nExample: arg1='(\\w+) (\\w+)' and arg2='$2,$1' which turns 'foo bar' into 'bar,foo'"));

        buttonMap.put("btnListLTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Left trim", "Trim whitespace off the left side of each line in the clipboard"));
        buttonMap.put("btnListTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Trim", "Trim whitespace off the left and right side of each line in the clipboard"));
        buttonMap.put("btnListRTrim", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Right trim", "Trim whitespace off the right side of each line in the clipboard"));
        buttonMap.put("btnListCollapse", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Collapse", "Strip out all empty lines (might be useful to do a trim first)"));
        buttonMap.put("btnListUniq", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Uniquify", "Make items in list unique by removing duplicates next to each other (might be useful to do \"lower\" and \"sort\" operations first)"));
        buttonMap.put("btnListSort", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Sort", "Sort the lines alphabetically in the clipboard"));
        buttonMap.put("btnListReverse", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: List reverse", "Reverse the order of the lines in the clipboard"));
        buttonMap.put("btnListStats", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Show stats", "Calculate basics stats on the lines in the clipboard"));
        buttonMap.put("btnListStore", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "List: Store lines", "Store each line from the clipboard into a separate buffer"));
        buttonMap.put("btnListPrepend", new ButtonSetup(Arrays.asList(txtArg2), "List: Prepend to lines", "Prepend given text (arg1) to the beginning of each line in the clipboard"));
        buttonMap.put("btnListAppend", new ButtonSetup(Arrays.asList(txtArg2), "List: Append to lines", "Append given text (arg1) to the end of each line in the clipboard"));
        buttonMap.put("btnListCenter", new ButtonSetup(Arrays.asList(txtArg2), "List: Center lines", "Center each line in the clipboard with given column width (arg1)"));
        buttonMap.put("btnListSlice", new ButtonSetup(Arrays.asList(txtArg2), "List: Slice", "Apply Python-style slice syntax (arg1) on each line's characters\nExamples: '4', '3:5', '2:', ':-4'"));
        buttonMap.put("btnListJoin", new ButtonSetup(Arrays.asList(txtArg2), "List: Join lines with character", "Join each line in the clipboard with the given delimiter (arg1)"));
        buttonMap.put("btnListContains", new ButtonSetup(Arrays.asList(txtArg2), "List Filter: lines that contain...", "Keep lines in the clipboard that contain the given literal string (arg1)"));
        buttonMap.put("btnListRegex", new ButtonSetup(Arrays.asList(txtArg2), "List Filter: lines that match regex...", "Keep lines in the clipboard that match the regex (arg1)"));
        buttonMap.put("btnListRegexFull", new ButtonSetup(Arrays.asList(txtArg2), "List Filter: full lines that match regex", "Keep lines in the clipboard that match the regex (arg1) exactly. The regex must match the entire line."));
        buttonMap.put("btnListRegexRepl", new ButtonSetup(Arrays.asList(), "List: Regex replace", "Replace text in each line that matches the regex (arg1) with the replacement string (arg2). Supports backreferences in replacement string.\nExample: arg1='(\\w+) (\\w+)' and arg2='$2,$1' which turns 'foo bar' into 'bar,foo'"));

        buttonMap.put("btnActionNotepad", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Action: Open in Notepad", "Open contents of clipboard in Notepad"));
        buttonMap.put("btnActionOpenUrl", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Action: Open as URL", "Open contents of system clipboard as URL's (supports newline separated lists of URL's)"));
        buttonMap.put("btnActionOpenFiles", new ButtonSetup(Arrays.asList(txtArg1, txtArg2), "Action: Open files in explorer", "Open contents of system clipboard as files/folders (supports newline separated lists of paths)"));
    }

    public void onReady(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (ov.getValue()) {
                    if (chkStoreOnFocus.isSelected()) {
                        log.insertText(0, "Storing clip to buffer when app gets focus\n");
                        appendToClipBuffersAndShowStatus(SysClipboard.read());
                    }
                    if (chkRetrieveOnFocus.isSelected()) {
                        log.insertText(0, "Retrieve buffer to clipboard when app gets focus\n");
                        retrieveClip();
                    }
                }
            }
        });

        if (chkStoreOnFocus.isSelected()) {
            appendToClipBuffersAndShowStatus(SysClipboard.read()); // read and store first clip when app first opens
        }

        txtHelpHeader.setText("my code header\n");
        txtHelpBody.setText("the body\nand the end goes ehre djs ladfkj aldskjf alskdjf a;lksjfd");
    }

    public void onBuffersKeyPressed(KeyEvent e) {
        if (e.getCode() == KeyCode.DELETE) {
            ObservableList<Integer> idxs = buffers.getSelectionModel().getSelectedIndices();
            int startingSize = idxs.size();
            // delete items in reverse index order to not offset indexes while iterating
            for (int i = idxs.size()-1; i >= 0; i--) {
                clips.remove((int) idxs.get(i));
            }
            statusBar.show("Deleted " + startingSize + " selected clip buffer(s)");
        }
    }
    public void onBuffersMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            // double click on ListView
            try {
                String clip = chkVariableSubstitution.isSelected() ? retrieveVarSubstitutedClipFromBuffer() : retrieveClipFromBuffer();
                statusBar.show("Retrieving " + (StringUtils.countMatches(clip, "\n") + 1) + " line(s) and " + clip.length() + " chars from buffer and storing to the clipboard");
            } catch (Exception exc) {
                System.out.println("problem");
                statusBar.showErr("Problem substituting variables in buffer: " + exc.toString());
            }
        } else {
            // single click - reset focus to top of list every time selection list changes (provide user a consistent behavior)
            List<Integer> selectedIdxs = buffers.getSelectionModel().getSelectedIndices();
            if (selectedIdxs.size() == 0) {
                return;
            }
            int firstSelected = selectedIdxs.get(0);
            buffers.getFocusModel().focus(firstSelected);
        }
    }
    public void onBuffersDragOver(DragEvent e) {
        // Should accept files, directories, URL's, strings
        e.acceptTransferModes(TransferMode.ANY);
        e.consume();
    }
    public void onBuffersDragEntered(DragEvent e) {
        if (Config.DEBUG) { Debug.dumpDragboard(e.getDragboard()); }
        buffers.setOpacity(Config.DRAG_N_DROP_ENTER_OPACITY);
        e.consume();
    }
    public void onBuffersDragExited(DragEvent e) {
        buffers.setOpacity(Config.DRAG_N_DROP_EXIT_OPACITY);
        e.consume();
    }
    public void onBuffersDragDropped(DragEvent e) {
        // if dropped file is a directory, each file is loaded into its own buffer.
        Dragboard b = e.getDragboard();
        URL url = getDragboardUrl(b);

        if (b.hasFiles()) {
            List<File> items = b.getFiles();
            int bufferCount = 0;
            for (File item : items) {
                if (item.isFile()) {
                    if (readFileAndStoreInBuffer(item)) {
                        bufferCount += 1;
                    }
                } else if (item.isDirectory()) {
                    File[] dirItems = item.listFiles();
                    for (File dirItem : dirItems) {
                        if (readFileAndStoreInBuffer(dirItem)) {
                            bufferCount += 1;
                        }
                    }
                } else {
                    // TODO: not sure if this case is even possible
                }
                statusBar.show("Read " + bufferCount + " files and stored contents in buffers");
            }
        } else if (url != null) {
            System.out.println("dropped a URL");
            String txt = getDataFromURL(url);
            int lineCount = StringUtils.countMatches(txt, "\n") + 1;
            statusBar.show("Storing " + lineCount + " lines and " + txt.length() + " characters to buffer from '" + url.toString() + "'");
            appendToClipBuffers(txt);
            e.setDropCompleted(true);
        } else if (b.hasString()) {
            String txt = b.getString();
            int lineCount = StringUtils.countMatches(txt, "\n") + 1;
            statusBar.show("Storing " + lineCount + " lines and " + txt.length() + " characters from drag and dropped string to a buffer");
            appendToClipBuffers(txt);
            e.setDropCompleted(true);
        } else {
            statusBar.showErr("Unexpected content dropped on control");
            e.setDropCompleted(false);
        }

        e.consume();
    }
    public void onBtnStrLTrim(ActionEvent e) {
        statusBar.show("Left-trimmed current clipboard contents");
        SysClipboard.write(StringUtil.ltrim(SysClipboard.read()));
    }
    public void onBtnStrTrim(ActionEvent e) {
        statusBar.show("Trimmed current clipboard contents");
        SysClipboard.write(SysClipboard.read().trim());
    }
    public void onBtnStrRTrim(ActionEvent e) {
        statusBar.show("Right-trimmed current clipboard contents");
        SysClipboard.write(StringUtil.rtrim(SysClipboard.read()));
    }
    public void onBtnStrLower(ActionEvent e) {
        statusBar.show("Lower-cased current clipboard contents");
        SysClipboard.write(SysClipboard.read().toLowerCase());
    }
    public void onBtnStrUpper(ActionEvent e) {
        statusBar.show("Upper-cased current clipboard contents");
        SysClipboard.write(SysClipboard.read().toUpperCase());
    }
    public void onBtnStrPrepend(ActionEvent e) {
        String arg = txtArg1.getText();
        statusBar.show("Prepended " + arg.length() + " character to current clipboard");
        SysClipboard.write(arg + SysClipboard.read());
    }
    public void onBtnStrAppend(ActionEvent e) {
        String arg = txtArg1.getText();
        statusBar.show("Appended " + arg.length() + " character to current clipboard");
        SysClipboard.write(SysClipboard.read() + arg);
    }
    public void onBtnStrWordWrap(ActionEvent e) {
        try {
            int width = Integer.valueOf(txtArg1.getText());
            statusBar.show("Word wrapped the current clipboard contents to " + width + " columns wide");
            SysClipboard.write(WordUtils.wrap(SysClipboard.read(), width));
        } catch (NumberFormatException exc) {
            statusBar.showErr("Invalid argument for word wrap. It must be an integer. (" + exc.getMessage() + ")");
        }
    }
    public void onBtnStrSplit(ActionEvent e) {
        String arg = txtArg1.getText();
        String clipboard = SysClipboard.read();
        int origSize = clipboard.length();
        clipboard = clipboard.replace(arg, System.lineSeparator());
        String[] array = StringUtils.splitByWholeSeparatorPreserveAllTokens(clipboard, System.lineSeparator());
        statusBar.show("Split " + origSize + " character(s) using '" + arg + "' into " + array.length + " line(s) in current clipboard");
        SysClipboard.write(clipboard);
    }
    public void onBtnStrReplace(ActionEvent e) {
        Debug.dumpString(SysClipboard.read());
        String trg = txtArg1.getText();
        String repl = txtArg2.getText();
        trg = StringUtil.replaceSpecialChars(trg);
        repl = StringUtil.replaceSpecialChars(repl);
        statusBar.show("Replaced '" + trg + "' with '" + repl + "' in current clipboard");
        SysClipboard.write(SysClipboard.read().replace(trg, repl));
        Debug.dumpString(SysClipboard.read());
    }
    public void onBtnStrRegexReplace(ActionEvent e) {
        String regex = txtArg1.getText();
        String repl = txtArg2.getText();
        statusBar.show("Replaced regex '" + regex + "' with '" + repl + "' in current clipboard");
        try {
            String result = SysClipboard.read().replaceAll(regex, repl);
            SysClipboard.write(result);
        } catch (Exception exc) {
            statusBar.showErr("Problem doing the regex substitution: " + exc);
        }
    }
    public void onBtnListLTrim(ActionEvent e) {
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtil.ltrim(line) ).mutate();
        statusBar.show("Left-trimmed " + result.size() + " lines in current clipboard");
    }
    public void onBtnListTrim(ActionEvent e) {
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> line.trim() ).mutate();
        statusBar.show("Trimmed " + result.size() + " lines in current clipboard");
    }
    public void onBtnListRTrim(ActionEvent e) {
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtil.rtrim(line) ).mutate();
        statusBar.show("Right-trimmed " + result.size() + " lines in current clipboard");
    }
    public void onBtnListSort(ActionEvent e) {
        List<String> result = new ClipboardAsListMutator( (list) -> Collections.sort(list) ).mutate();
        statusBar.show("Sorted " + result.size() + " lines in current clipboard");
    }
    public void onBtnListReverse(ActionEvent e) {
        List<String> result = new ClipboardAsListMutator( (list) -> Collections.reverse(list) ).mutate();
        statusBar.show("Reversed " + result.size() + " lines in current clipboard");
    }
    public void onBtnListStats(ActionEvent e) {
        String clipboard = SysClipboard.read();
        String[] array = StringUtils.splitByWholeSeparatorPreserveAllTokens(clipboard, System.lineSeparator());
        List<String> list = Arrays.asList(array);
        String[] words = clipboard.split("\\s+"); // want to collapse identical, adjacent tokens

        Integer minLineLen = Integer.MAX_VALUE;
        Integer maxLineLen = 0;
        Integer totalLineLen = 0;
        for (String line : list) {
            Integer lineLength = line.length();
            minLineLen = Math.min(minLineLen, lineLength);
            maxLineLen = Math.max(maxLineLen, lineLength);
            totalLineLen += lineLength;
        }
        String msg = String.format(
                "List stats: lines=%d chars=%d words=%d min/max/avgLineLength=%d / %d / %.1f",
                list.size(), clipboard.length(), words.length,
                minLineLen, maxLineLen, (float) totalLineLen/list.size()
        );
        statusBar.show(msg);
    }
    public void onBtnListStore(ActionEvent e) {
        List<String> lines = SysClipboard.readAsLines();
        Collections.reverse(lines);
        for (String line : lines) {
            appendToClipBuffers(line);
        }
        statusBar.show("Stored " + lines.size() + " line(s) into individual buffers");
    }
    public void onBtnListPrepend(ActionEvent e) {
        String arg = txtArg1.getText();
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> arg + line ).mutate();
        statusBar.show("Prepended " + arg.length() + " character(s) to " + result.size() + " lines in current clipboard");
    }
    public void onBtnListAppend(ActionEvent e) {
        String arg = txtArg1.getText();
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> line + arg ).mutate();
        statusBar.show("Appended " + arg.length() + " character(s) to " + result.size() + " lines in current clipboard");
    }
    public void onBtnListCenter(ActionEvent e) {
        try {
            int width = Integer.valueOf(txtArg1.getText());
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtils.center(line, width) ).mutate();
            statusBar.show("Centered " + result.size() + " lines in current clipboard");
        } catch (NumberFormatException exc) {
            statusBar.showErr("Invalid argument for center. It must be an integer. (" + exc.getMessage() + ")");
        }
    }
    public void onBtnListSlice(ActionEvent e) {
        // parse slice argument syntax
        String sliceExpr = txtArg1.getText();
        Integer[] idxs;
        try {
            idxs = Functions.parseSliceSyntax(sliceExpr);
        } catch(ArgParseError exc) {
            statusBar.showErr(exc.getMessage());
            return;
        }
        Integer singleIdx = idxs[0];
        Integer startIdx = idxs[1];
        Integer endIdx = idxs[2];

        List<String> result = singleIdx != null
                ? new ClipboardAsListMutatorByLine( (line) -> StringUtil.slice(line, singleIdx)).mutate()
                : new ClipboardAsListMutatorByLine( (line) -> StringUtil.slice(line, startIdx, endIdx)).mutate();
        statusBar.show("Applied slice substring expression \"" + sliceExpr + "\" to " + result.size() + " line(s) in current clipboard");
    }
    public void onBtnListJoin(ActionEvent e) {
        String clipboard = SysClipboard.read();
        String arg = txtArg1.getText();
        statusBar.show("Joined " + StringUtils.splitByWholeSeparatorPreserveAllTokens(clipboard, System.lineSeparator()).length + " lines with '" + arg + "' in current clipboard");
        SysClipboard.write(clipboard.replace(System.lineSeparator(), arg));
    }
    public void onBtnListCollapse(ActionEvent e) {
        Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.length() > 0 ).filter();
        statusBar.show("Collapsed " + result.getLeft().size() + " lines down to " + result.getRight().size() + " by removing empty lines in current clipboard");
    }
    public void onBtnListUniq(ActionEvent e) {
        List<String> list = SysClipboard.readAsLines();
        List<String> filtered = new ArrayList();
        String prev = null;
        for (String line : list) {
            if (! line.equals(prev)) {
                filtered.add(line);
                prev = line;
            }
        }
        statusBar.show("Made " + list.size() + " lines " + filtered.size() + " by removing adjacent duplicates in current clipboard");
        SysClipboard.write(String.join("\n", filtered));
    }
    public void onBtnListContains(ActionEvent e) {
        String arg = txtArg1.getText();
        Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.contains(arg) ).filter();
        statusBar.show("Filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
    }
    public void onBtnListRegex(ActionEvent e) {
        String rawRegex = txtArg1.getText();
        String regex = "^.*" + rawRegex + ".*$";
        Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.matches(regex) ).filter();
        statusBar.show("Regex filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
    }
    public void onBtnListRegexFull(ActionEvent e) {
        String regex = txtArg1.getText();
        Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.matches(regex) ).filter();
        statusBar.show("Regex (full) filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
    }
    public void onBtnListRegexRepl(ActionEvent e) {
        String regex = txtArg1.getText();
        String repl = txtArg2.getText();
        List<String> result = new ClipboardAsListMutatorByLine( (line) -> line.replaceAll(regex, repl)).mutate();
        statusBar.show("Replaced regex '" + regex + "' with '" + repl + "' in lines in current clipboard");
    }
    public void onBtnNotepadAction(ActionEvent e) {
        Path result = Functions.writeToTempFile(Config.VIEW_TEMP_FILE, Config.TEMP_FILE_EXT, SysClipboard.read().getBytes());
        if (result != null) {
            try {
                new ProcessBuilder(Config.VIEW_APP, result.toString()).start();
            } catch(Exception exc) {
                statusBar.showErr("Couldn't launch notepad tool");
            }
        } else {
            statusBar.showErr("Couldn't write temp file for opening with Notepad");
        }
    }
    public void onBtnOpenUrl(ActionEvent e) {
        try {
            int count = 0;
            for (String uri : SysClipboard.readAsLines()) {
                System.out.println(new URI(uri));
                Desktop.getDesktop().browse(new URI(uri));
                count += 1;
            }
            statusBar.show("Opened " + count + " URI(s) in the browser");
        } catch(Exception exc) {
            statusBar.showErr("Couldn't launch URI's in clipboard because: " + exc.getMessage());
        }
    }
    public void onBtnOpenFiles(ActionEvent e) {
        try {
            int count = 0;
            for (String folder : SysClipboard.readAsLines()) {
                System.out.println(new File(folder));
                Desktop.getDesktop().open(new File(folder));
                count += 1;
            }
            statusBar.show("Opened " + count + " file(s)/folder(s) in the browser");
        } catch(Exception exc) {
            statusBar.showErr("Couldn't launch file/folder in clipboard because: " + exc.getMessage());
        }
    }

    public void onBtnStore(ActionEvent e) {
        appendToClipBuffersAndShowStatus(SysClipboard.read());
    }

    public void onBtnPrepend(ActionEvent e) {
        String clipboard = SysClipboard.read();
        ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
        statusBar.show("Prepend " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
        for (Integer i : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
            clips.set(i, new ClipBuffer(clipboard + clips.get(i).clip));
        }
    }

    public void onBtnAppend(ActionEvent e) {
        String clipboard = SysClipboard.read();
        ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
        statusBar.show("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
        for (Integer i : indices) {
            clips.set(i, new ClipBuffer(clips.get(i).clip + clipboard));
        }
    }

    public void onBtnReplace(ActionEvent e) {
        String clipboard = SysClipboard.read();
        ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
        statusBar.show("Replace " + indices.size() + " buffer(s) with " + clipboard.length() + " characters");
        for (Integer i : indices) {
            clips.set(i, new ClipBuffer(clipboard));
        }
    }

    public void onBtnRetrieveClick(ActionEvent e) {
        retrieveClip();
    }

    public void retrieveClip() {
        // Generic function that handles the "retrieve" action and all of its different functionality

        // Cycle through selected buffers

        // Give list control focus so we can see current focus, but focus makes no visual difference to items that are selected.
        // items.requestFocus();
        Integer focusIdx = buffers.getFocusModel().getFocusedIndex();
        List<Integer> selIdxs = buffers.getSelectionModel().getSelectedIndices();
        Integer lastSelIdx = selIdxs.get(selIdxs.size() - 1);

        // Check for no selected items
        if (selIdxs.size() == 0) {
            statusBar.showErr("No item selected");
            return;
        }

        // Advance focus to next selected item if focused item not already selected
        Integer ordinalOfSelected = -1; // first? second?
        if (focusIdx > lastSelIdx) {
            focusIdx = selIdxs.get(0);
            buffers.getFocusModel().focus(focusIdx);
            ordinalOfSelected = 0 + 1;
        } else {
            for (int i = 0; i < selIdxs.size(); ++i) {
                if (selIdxs.get(i) >= focusIdx) {
                    focusIdx = selIdxs.get(i);
                    buffers.getFocusModel().focus(focusIdx);
                    ordinalOfSelected = i + 1;
                    break;
                }
            }
        }

        // Set clipboard
        try {
            String clip = chkVariableSubstitution.isSelected() ? retrieveVarSubstitutedClipFromBuffer() : retrieveClipFromBuffer();
            if (selIdxs.size() > 1) {
                String msg = String.format("Retrieving %d line(s) and %d chars from buffer (#%d of %d) and storing to the clipboard",
                        StringUtils.countMatches(clip, "\n") + 1,
                        clip.length(),
                        ordinalOfSelected,
                        selIdxs.size()
                );
                statusBar.show(msg);
            } else {
                statusBar.show("Retrieving " + (StringUtils.countMatches(clip, "\n") + 1) + " line(s) and " + clip.length() + " chars from buffer and storing to the clipboard");
            }
        } catch (Exception exc) {
            System.out.println("problem");
            statusBar.showErr("Problem substituting variables in buffer: " + exc.toString());
        }

        // Advance focus to next selected item
        if (focusIdx >= lastSelIdx) {
            focusIdx = selIdxs.get(0);
            buffers.getFocusModel().focus(focusIdx);
        } else {
            for (Integer selIdx : selIdxs) {
                if (selIdx > focusIdx) {
                    focusIdx = selIdx;
                    buffers.getFocusModel().focus(focusIdx);
                    System.out.println(" new focus" + focusIdx);
                    break;
                }
            }
        }
    }

    public void onBtnRetrieveDragOver(DragEvent e) {
        if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
            e.acceptTransferModes(TransferMode.ANY);
        }
        e.consume();
    }
    public void onBtnRetrieveDragEntered(DragEvent e) {
        if (Config.DEBUG) { Debug.dumpDragboard(e.getDragboard()); }
        if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
            btnRetrieve.setOpacity(Config.DRAG_N_DROP_ENTER_OPACITY);
        }
        e.consume();
    }
    public void onBtnRetrieveDragExited(DragEvent e) {
        if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
            btnRetrieve.setOpacity(Config.DRAG_N_DROP_EXIT_OPACITY);
        }
        e.consume();
    }
    public void onBtnRetrieveDragDropped(DragEvent e) {
        Dragboard b = e.getDragboard();
        URL url = getDragboardUrl(b);

        if (b.hasFiles()) {
            List<File> items = b.getFiles();
            if (items.size() == 1 && items.get(0).isFile()) {
                File f = items.get(0);
                List<String> lines = readFileAndStoreInClipboard(f);
                if (lines != null) {
                    statusBar.show("Read " + lines.size() + " lines from file (" + f.getName() + ") into system clipboard");
                } else {
                    statusBar.showErr("Problem reading from file " + f.getName());
                }
            }
        } else if (url != null) {
            System.out.println("dropped a URL");
            String txt = getDataFromURL(url);
            int lineCount = StringUtils.countMatches(txt, "\n") + 1;
            statusBar.show("Storing " + lineCount + " lines and " + txt.length() + " characters from '" + url.toString() + "' to system clipboard");
            SysClipboard.write(txt);
            e.setDropCompleted(true);
        } else if (b.hasString()) {
            String txt = b.getString();
            int lineCount = StringUtils.countMatches(txt, "\n") + 1;
            statusBar.show("Storing " + lineCount + " lines and " + txt.length() + " characters from drag and dropped string to system clipboard");
            SysClipboard.write(txt);
            e.setDropCompleted(true);
        } else {
            statusBar.showErr("Unexpected content dropped on control");
            e.setDropCompleted(false);
        }
        e.consume();
    }

    public void onBtnJoin(ActionEvent e) {
        ObservableList<ClipBuffer> selectedBuffers = buffers.getSelectionModel().getSelectedItems();
        ArrayList<String> clips = new ArrayList<String>();
        for (ClipBuffer buf : selectedBuffers) {
            clips.add(buf.clip);
        }
        String clip = String.join(System.lineSeparator(), clips);
        String msg = String.format("Joining the %d selected buffers and storing %d chars to the clipboard",
                selectedBuffers.size(),
                clip.length()
        );
        SysClipboard.write(clip);
        statusBar.show(msg);
        buffers.getFocusModel().focus(1);
    }

    public void onBtnDiff(ActionEvent e) {
        ObservableList<ClipBuffer> selectedBuffers = buffers.getSelectionModel().getSelectedItems();
        if (selectedBuffers.size() != 2) {
            statusBar.showErr("Need two buffers selected to do a diff");
        } else {
            try {
                Path fileA = Functions.writeToTempFile(Config.DIFF_TEMP_FILE_A, Config.TEMP_FILE_EXT, selectedBuffers.get(0).clip.getBytes());
                Path fileB = Functions.writeToTempFile(Config.DIFF_TEMP_FILE_B, Config.TEMP_FILE_EXT, selectedBuffers.get(1).clip.getBytes());
                new ProcessBuilder(Config.DIFF_APP, fileA.toString(), fileB.toString()).start();
                statusBar.show("Diffing the two selected buffers with " + Config.DIFF_APP);
            } catch(Exception exc) {
                statusBar.showErr("Can't launch diff tool");
            }
        }
    }

    public void onBtnUp(ActionEvent e) {
        ObservableList<Integer> selectedIdxs = buffers.getSelectionModel().getSelectedIndices();
        ArrayList<Integer> selIdxs = new ArrayList(selectedIdxs); // create copy
        Integer topBoundIdx = 0;
        for (int i : selIdxs) {
            // Don't swap past top of list
            if (i <= topBoundIdx) {
                continue;
            }
            // Don't swap if item above you is selected (for when selected items bunch at the top of the list)
            Integer prevIdx = i - 1;
            if (prevIdx >= topBoundIdx && buffers.getSelectionModel().isSelected(prevIdx)) {
                continue;
            }
            swapBuffers(i, prevIdx);
        }
    }

    public void onBtnDown(ActionEvent e) {
        ObservableList<Integer> selectedIdxs = buffers.getSelectionModel().getSelectedIndices();
        List<Integer> selIdxs = new ArrayList(selectedIdxs);
        Collections.reverse(selIdxs);  // reverse is not supported by ObservableList
        Integer bottomBoundIdx = clips.size() - 1;
        for (int i : selIdxs) {
            // Don't swap past bottom of list
            if (i >= bottomBoundIdx) {
                continue;
            }
            // Don't swap if item below you is selected (for when selected items bunch at the bottom of the list)
            Integer nextIdx = i + 1;
            if (nextIdx <= bottomBoundIdx && buffers.getSelectionModel().isSelected(nextIdx)) {
                continue;
            }
            swapBuffers(i, nextIdx);
        }
    }

    /** Reads contents of file and stores in clipboard buffer */
    // BUG: Nearly silently fails when can't read contents of a file (usually when reading a binary file)
    private boolean readFileAndStoreInBuffer(File file) {
        if (file.isFile()) {
            Path f = Paths.get(file.getAbsolutePath());
            try {
                List<String> lines = Files.readAllLines(f);
                String txt = String.join("\n", lines);
                appendToClipBuffers(txt);
                return true;
            } catch (Exception exc) {
                System.out.println("Problem reading '" + f.toString() + "'. Exception: " + exc);
            }
        }
        return false;
    }

    private List<String> readFileAndStoreInClipboard(File file) {
        if (file.isFile()) {
            Path f = Paths.get(file.getAbsolutePath());
            try {
                List<String> lines = Files.readAllLines(f);
                String txt = String.join("\n", lines);
                SysClipboard.write(txt);
                return lines;
            } catch (Exception exc) {
                System.out.println("Problem reading '" + f.toString() + "'. Exception: " + exc);
            }
        }
        return null;
    }
    public void onMenuExitItem(ActionEvent e) {
        Platform.exit();
    }
    public void onMenuSaveAllBuffers(ActionEvent e) {
        saveClipsToDisk(clips, primaryStage);
    }
    public void onMenuSaveSelectedBuffers(ActionEvent e) {
        saveClipsToDisk(buffers.getSelectionModel().getSelectedItems(), primaryStage);
    }
    private String retrieveClipFromBuffer() {
        ClipBuffer buffer = buffers.getFocusModel().getFocusedItem();
        SysClipboard.write(buffer.clip);
        return buffer.clip;
    }

    private String retrieveVarSubstitutedClipFromBuffer() {
        ClipBuffer buffer = buffers.getFocusModel().getFocusedItem();
        HashMap<String, String> dat = new HashMap<>();
        for (int i = 0; i < clips.size(); ++i) {
            dat.put(Integer.toString(i), clips.get(i).clip);
        }
        dat.put("clip", SysClipboard.read());
        StrSubstitutor sub = new StrSubstitutor(dat);
        String txt = sub.replace(buffer.clip);
        SysClipboard.write(txt);
        return txt;
    }

    private void appendToClipBuffers(String clip) {
        ClipBuffer buffer = new ClipBuffer(clip);
        clips.add(0, buffer);
        buffers.scrollTo(buffer);
    }

    private void appendToClipBuffersAndShowStatus(String clip) {
        appendToClipBuffers(clip);
        statusBar.show(String.format("Storing %d line(s) and %d chars from the clipboard to a buffer\n",
                StringUtils.countMatches(clip, "\n") + 1,
                clip.length()));
    }

    private void swapBuffers(int idx1, int idx2) {
        // NOTE: this function does not attempt to do anything sane with focus. It seems that focus can cause issues
        // with extra rows getting selected as I move them around. I think this may have something to do with the focus?
        // I'm just guessing.
        ClipBuffer tmpBuff1 = clips.get(idx1);
        boolean tmpSelectState1 = buffers.getSelectionModel().isSelected(idx1);
        ClipBuffer tmpBuff2 = clips.get(idx2);
        boolean tmpSelectState2 = buffers.getSelectionModel().isSelected(idx2);

        // NOTE: if you change the code below to set idx1 value and then change its selection, then do the same for idx2,
        // moving with multiple items selected will often cause additional rows to get selected as you move. The order
        // of the code below seems to avoid this problem for some reason.
        clips.set(idx1, tmpBuff2);
        clips.set(idx2, tmpBuff1);

        if (tmpSelectState2) {
            buffers.getSelectionModel().select(idx1);
        } else {
            buffers.getSelectionModel().clearSelection(idx1);
        }

        if (tmpSelectState1) {
            buffers.getSelectionModel().select(idx2);
        } else {
            buffers.getSelectionModel().clearSelection(idx2);
        }
    }

    private void saveClipsToDisk(List<ClipBuffer> buffers, Stage primaryStage) {
        if (buffers.size() == 0) {
            statusBar.showErr("There were no buffers selected");
            return;
        }
        DirectoryChooser dirChooser = new DirectoryChooser();
        File dir = dirChooser.showDialog(primaryStage);
        int idx = 1;
        boolean failed = false;
        for (ClipBuffer buff : buffers) {
            String fileName = String.format("buffer_%03d", idx);
            String desc = StringUtil.extractInitialWords(buff.clip, Config.WORDS_FOR_FILE_NAMING);
            if (desc.length() > 0) {
                fileName += "_" + desc;
            }
            fileName += ".txt";
            ++idx;
            File file = new File(dir, fileName);
            Path p = Paths.get(file.getAbsolutePath());
            try {
                Files.write(p, buff.clip.getBytes());
            } catch (Exception exc) {
                statusBar.showErr("Problem writing file: " + p);
                failed = true;
                break;
            }
        }
        if (! failed) {
            statusBar.show("Wrote " + buffers.size() + " buffer(s) to " + dir);
        }
    }

    private String getDataFromURL(URL url) {
        // Reference: https://docs.oracle.com/javase/tutorial/networking/urls/readingURL.html
        String text = "";
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                text += line + "\n"; // this adds an extra \n at the end. Should read into a list and then join with "\n"
            }
            rd.close();
        } catch (Exception exc) {
            System.out.println("error reading from url: " + url);
        }
        return text;
    }

    /** Tests whether there is a valid web URL in the dragboard. Dragboard.hasUrl() has a much broader interpretation of URL than I would like. */
    private static URL getDragboardUrl(Dragboard dragboard) {
        URL url = null;
        try {
            url = new URL(dragboard.getUrl());
        } catch(MalformedURLException ignored) {
        }
        return url;
    }

    /** Encapsulates logic of whether the retrieve button should be the target of a drop */
    // Should accept a single file, URL, or string
    private static boolean shouldAcceptDropOnRetrieve(Dragboard b) {
        if (b.hasFiles()) {
            List<File> files = b.getFiles();
            if (files.size() == 1 && files.get(0).isFile()) {
                return true;
            }
        } else if (getDragboardUrl(b) != null) {
            return true;
        } else if (b.hasString()) {
            return true;
        }
        return false;
    }
}
