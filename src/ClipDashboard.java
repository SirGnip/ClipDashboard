import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import com.juxtaflux.MyAppFramework;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ArgParseError extends Exception {
    ArgParseError(String msg) { super(msg); }
}

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

    public static Integer[] parseSliceSyntax(String sliceExpr) throws ArgParseError {
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
};

class StatusBar extends Label {
    static Paint defaultColor;

    /** Must call this after primaryStage.show() as the real colors get set at that point. I'm guessing that is when CSS is applied. */
    public void cacheTextFillColor() {
        defaultColor = getTextFill();
    }
    public void show(String msg) {
        this.setTextFill(defaultColor);
        this.setText(msg);
    }
    public void showErr(String msg) {
        this.setTextFill(Config.STATUS_BAR_ERROR_COLOR);
        this.setText("ERROR: " + msg);
    }
}

class ClipBuffer {
    String clip;
    ClipBuffer(String msg) {
        clip = msg;
    }
    public String toString() {
        int lineCount = StringUtils.countMatches(clip, "\n") + 1;
        String formatted = clip.replace("\n", " ");
        if (formatted.length() > Config.BUFFER_CROP_LENGTH) {
            formatted = formatted.format("%s... (%d chars, %d lines)", formatted.substring(0, Config.BUFFER_CROP_LENGTH), formatted.length(), lineCount);
        }
        return formatted;
    }
}

public class ClipDashboard extends Application {

    private ListView<ClipBuffer> items;
    private ObservableList<ClipBuffer> clips;
    private TextArea log;
    private StatusBar statusBar;
    private CheckMenuItem chkStoreOnFocus;

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setSpacing(Config.UI_SPACING);

        statusBar = new StatusBar();

        initMenu(vbox);

        clips = FXCollections.observableArrayList(Config.INITIAL_CLIPS.stream().map(c -> new ClipBuffer(c)).collect(Collectors.toList()));
        items = new ListView(clips);
        items.setMinHeight(Config.LIST_VIEW_HEIGHT);
        items.setMaxHeight(Config.LIST_VIEW_HEIGHT);
        items.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        items.setOnMouseClicked((e) -> {
            // double click on ListView
            if (e.getClickCount() == 2) {
                retrieveClipFromBufferAndShowStatus();
            }
        });
        items.setOnDragOver((e) -> {
            Dragboard board = e.getDragboard();
//            System.out.println("drag over " + board.hasString() + " " + board.hasUrl() + " " + board.hasFiles() + " " + board.hasImage());
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        items.setOnDragEntered((e) -> {
            System.out.println("on drag entered");
            e.consume();
        });
        items.setOnDragExited((e) -> {
            System.out.println("on drag exited");
            e.consume();
        });
        items.setOnDragDropped((e) -> {
            // if dropped file is a directory, each file is loaded into its own buffer?
            Dragboard b = e.getDragboard();
            System.out.printf("on drag dropped %s %s %s %s %s %s\n",
                b.hasString(),
                b.hasUrl(),
                b.hasFiles(),
                b.hasHtml(),
                b.hasRtf(),
                b.hasImage()
            );

            b.getContentTypes().forEach(df -> System.out.println("MIMETYPE: " + df.getClass().getName() + " " + df + " - " + b.getContent(df).getClass().getName() + " >" + b.getContent(df))); // reference: http://stackoverflow.com/questions/30923817/javafx-dnd-third-party-program-to-javafx-app

            URL url = null;
            try {
                url = new URL(b.getUrl());
                System.out.println("was a URL " + url);
            } catch(Exception x) {
                System.out.println("was NOT a URL");
                System.out.println(x);
            }

            if (b.hasFiles()) {
                Object o = b.getFiles();  System.out.println("**Files:" + o.getClass().getName() + " " + o);
                List<File> x = b.getFiles();
                System.out.println(">>>" + x);
                for (File f : x) {
                    System.out.println("  file: " + f.getAbsolutePath() + " " + f.isFile() + " " + f.isDirectory());
                    if (f.isDirectory()) {
                        System.out.println(f.list());
                        String[] ss = f.list();
                        File[] fs = f.listFiles();
                        for (int i = 0; i < fs.length; ++i) {
                            System.out.println("     dir contents:" + fs[i]);
                        }
                    }
                }
            } else if (url != null) {
                System.out.println("dropped a URL");
                String txt = getDataFromURL(url);
                int lineCount = StringUtils.countMatches(txt, "\n") + 1;
                statusBar.show("Put " + lineCount + " lines and " + txt.length() + " characters to system clipboard from '" + url.toString() + "'");
                SysClipboard.write(txt);

//                try {
//                    Object o = url.getContent().;
//                    System.out.println(o);
//                } catch(Exception exc) {
//                    System.out.println("error getting url");
//                }

            }

//            if (b.hasHtml()) {
//                Object o = b.getHtml();  System.out.println("**Html:" + o.getClass().getName() + " " + o);
//            }


//            if (e.getDragboard().hasString()) {
//                System.out.println("writing");
//                appendToClipBuffers(e.getDragboard().getString());
//                e.setDropCompleted(true);
//            } else if (e.getDragboard().hasFiles()) {
//                try {
//                    for (File f : e.getDragboard().getFiles()) {
//                        appendToClipBuffers("FILE: " + f.getPath());
//                    }
//                } catch (Exception exc) {
//                    System.out.println("clipboard error");
//                }
//            } else {
//                e.setDropCompleted(false);
//            }
            e.consume();
        });



        // Buffer operations
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(Config.UI_SPACING));
        Label lblBuffers = new Label("Buffers: ");
        Button btnRetrieve = new Button("Retrieve");
        btnRetrieve.setMaxWidth(Double.MAX_VALUE);
        btnRetrieve.setTooltip(
                new Tooltip(
                        "Retrieve contents of selected buffer and store to clipboard.\n" +
                        "If multiple buffers are selected, cycle through each buffer in turn.\n" +
                        "Double clicking a buffer is a shortcut for \"Retrieve\".\n" +
                        "(See \"Join\" to store all selected buffers in clipboard)"
                ));
        Button btnStore = new Button("Store");
        Button btnPrepend = new Button("Prepend");
        Button btnAppend = new Button("Append");
        Button btnReplace = new Button("Replace");
        Button btnDelete = new Button("Del");
        Button btnJoin = new Button("Join");
        Button btnDiff = new Button("Diff");
        Button btnUp = new Button("^");
        Button btnDown = new Button("v");

        // String/List tabs
        TabPane modificationTabPane = new TabPane();
        modificationTabPane.setMinHeight(Config.MODIFICATION_TAB_HEIGHT);

        // String operation tab
        Tab tab1 = new Tab("String operations");
        tab1.setClosable(false);
        modificationTabPane.getTabs().add(tab1);

        VBox strTabVBox = new VBox();
        strTabVBox.setSpacing(Config.UI_SPACING);

        HBox noArgStrOpsHBox = new HBox();
        Button btnStrLTrim = new Button("L");
        Button btnStrTrim = new Button("trim");
        Button btnStrRTrim = new Button("R");
        Button btnStrLower = new Button("lower");
        Button btnStrUpper = new Button("upper");
        noArgStrOpsHBox.getChildren().addAll(btnStrLTrim, btnStrTrim, btnStrRTrim, btnStrLower, btnStrUpper);

        HBox argsStrOpsHBox = new HBox();
        Button btnStrPrepend = new Button("prepend");
        Button btnStrAppend = new Button("append");
        Button btnStrSplit = new Button("split");
        Button btnStrReplace = new Button("replace");
        TextField txtStrArg1 = new TextField(",");
        txtStrArg1.setPrefWidth(Config.ARG_WIDTH);
        TextField txtStrArg2 = new TextField("\\n");
        txtStrArg2.setPrefWidth(Config.ARG_WIDTH);
        argsStrOpsHBox.getChildren().addAll(btnStrPrepend, btnStrAppend, btnStrSplit, btnStrReplace, txtStrArg1, txtStrArg2);

        strTabVBox.getChildren().addAll(noArgStrOpsHBox, argsStrOpsHBox);
        tab1.setContent(strTabVBox);

        btnStrLTrim.setOnAction((e) -> {
            statusBar.show("Left-trimmed current clipboard contents");
            SysClipboard.write(StringUtil.ltrim(SysClipboard.read()));
        });
        btnStrTrim.setOnAction((e) -> {
            statusBar.show("Trimmed current clipboard contents");
            SysClipboard.write(SysClipboard.read().trim());
        });
        btnStrRTrim.setOnAction((e) -> {
            statusBar.show("Right-trimmed current clipboard contents");
            SysClipboard.write(StringUtil.rtrim(SysClipboard.read()));
        });
        btnStrLower.setOnAction((e) -> {
            statusBar.show("Lower-cased current clipboard contents");
            SysClipboard.write(SysClipboard.read().toLowerCase());
        });
        btnStrUpper.setOnAction((e) -> {
            statusBar.show("Upper-cased current clipboard contents");
            SysClipboard.write(SysClipboard.read().toUpperCase());
        });
        btnStrPrepend.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            statusBar.show("Prepended " + arg.length() + " character to current clipboard");
            SysClipboard.write(arg + SysClipboard.read());
        });
        btnStrAppend.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            statusBar.show("Appended " + arg.length() + " character to current clipboard");
            SysClipboard.write(SysClipboard.read() + arg);
        });
        btnStrSplit.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            String clipboard = SysClipboard.read();
            int origSize = clipboard.length();
            clipboard = clipboard.replace(arg, "\n");
            String[] array = clipboard.split("\n");
            statusBar.show("Split " + origSize + " character(s) using '" + arg + "' into " + array.length + " line(s) in current clipboard");
            SysClipboard.write(clipboard);
        });
        btnStrReplace.setOnAction((e) -> {
            String trg = txtStrArg1.getText();
            String repl = txtStrArg2.getText();
            trg = StringUtil.replaceSpecialChars(trg);
            repl = StringUtil.replaceSpecialChars(repl);
            statusBar.show("Replaced '" + trg + "' with '" + repl + "' in current clipboard");
            SysClipboard.write(SysClipboard.read().replace(trg, repl));
        });

        // List operations tab
        // NOTE: List operations assume each "item" of the "list" is a line of text, each separated by a carriage returns.
        Tab tab2 = new Tab("List operations");
        tab2.setClosable(false);
        modificationTabPane.getTabs().add(tab2);

        VBox listTabVBox = new VBox();
        listTabVBox.setSpacing(Config.UI_SPACING);

        HBox noArgListOpsHBox = new HBox();
        Button btnListLTrim = new Button("L");
        btnListLTrim.setTooltip(new Tooltip("Trim whitespace from the left/beginning of each line in the clipboard"));
        Button btnListTrim = new Button("trim");
        btnListTrim.setTooltip(new Tooltip("Trim whitespace from both sides of each line in the clipboard"));
        Button btnListRTrim = new Button("R");
        btnListRTrim.setTooltip(new Tooltip("Trim whitespace from the right/end of each line in the clipboard"));
        Button btnListCollapse = new Button("collapse");
        btnListCollapse.setTooltip(new Tooltip("Strip out all empty lines (might be useful to do a trim first)"));
        Button btnListUniq = new Button("uniq");
        btnListUniq.setTooltip(new Tooltip("Make items in list unique by removing duplicates next to each other (might be useful to do \"lower\" and \"sort\" operations first)"));
        Button btnListSort = new Button("sort");
        btnListSort.setTooltip(new Tooltip("Alphabetically Sort the lines in the clipboard"));
        Button btnListReverse = new Button("reverse");
        btnListReverse.setTooltip(new Tooltip("Reverse the order of the lines in the clipboard"));
        Button btnListStats = new Button("stats");
        btnListStats.setTooltip(new Tooltip("Calculate basics stats on the lines in the clipboard"));
        noArgListOpsHBox.getChildren().addAll(btnListLTrim, btnListTrim, btnListRTrim, btnListCollapse, btnListUniq, btnListSort, btnListReverse, btnListStats);

        HBox argsListOpsHBox = new HBox();
        Button btnListPrepend = new Button("prepend");
        btnListPrepend.setTooltip(new Tooltip("Prepend given text to the beginning of each line in the clipboard\n(arg1: text)"));
        Button btnListAppend = new Button("append");
        btnListAppend.setTooltip(new Tooltip("Append given text to the end of each line in the clipboard\n(arg1: text)"));
        Button btnListSlice = new Button("slice");
        btnListSlice.setTooltip(new Tooltip("Do a Python-style slice on each line in the clipboard\n(arg1: slice expression)\nExample: \"5:-1\" slices each line to be the substring between 6th character and one character before the end of the line"));
        Button btnListJoin = new Button("join");
        btnListJoin.setTooltip(new Tooltip("Join each line in the clipboard with the character string given\n(arg1: delimiter)"));
        Button btnListContains = new Button("contains");
        btnListContains.setTooltip(new Tooltip("Keep lines in the clipboard that contain the literal string\n(arg1: substring)"));
        Button btnListRegex = new Button("regex");
        btnListRegex.setTooltip(new Tooltip("Keep lines in the clipboard that contain a match to the regex\n(arg1: regex)"));
        Button btnListRegexFull = new Button("regex full");
        btnListRegexFull.setTooltip(new Tooltip("Keep lines in the clipboard that match the regex exactly. The regex must match the entire line.\n(arg1: regex)"));
        TextField txtListArg1 = new TextField("_");
        txtListArg1.setPrefWidth(Config.ARG_WIDTH);
        argsListOpsHBox.getChildren().addAll(btnListPrepend, btnListAppend, btnListSlice, btnListJoin, btnListContains, btnListRegex, btnListRegexFull, txtListArg1);

        listTabVBox.getChildren().addAll(noArgListOpsHBox, argsListOpsHBox);

        tab2.setContent(listTabVBox);
        btnListLTrim.setOnAction((e) -> {
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtil.ltrim(line) ).mutate();
            statusBar.show("Left-trimmed " + result.size() + " lines in current clipboard");
        });
        btnListTrim.setOnAction((e) -> {
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> line.trim() ).mutate();
            statusBar.show("Trimmed " + result.size() + " lines in current clipboard");
        });
        btnListRTrim.setOnAction((e) -> {
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtil.rtrim(line) ).mutate();
            statusBar.show("Right-trimmed " + result.size() + " lines in current clipboard");
        });
        btnListSort.setOnAction((e) -> {
            List<String> result = new ClipboardAsListMutator( (list) -> Collections.sort(list) ).mutate();
            statusBar.show("Sorted " + result.size() + " lines in current clipboard");
        });
        btnListReverse.setOnAction((e) -> {
            List<String> result = new ClipboardAsListMutator( (list) -> Collections.reverse(list) ).mutate();
            statusBar.show("Reversed " + result.size() + " lines in current clipboard");
        });
        btnListStats.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            String[] array = clipboard.split("\n");
            List<String> list = Arrays.asList(array);
            String[] words = clipboard.split("\\s+");

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
        });
        btnListPrepend.setOnAction((e) -> {
            String arg = txtListArg1.getText();
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> arg + line ).mutate();
            statusBar.show("Prepended " + arg.length() + " character(s) to " + result.size() + " lines in current clipboard");
        });
        btnListAppend.setOnAction((e) -> {
            String arg = txtListArg1.getText();
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> line + arg ).mutate();
            statusBar.show("Appended " + arg.length() + " character(s) to " + result.size() + " lines in current clipboard");
        });
        btnListSlice.setOnAction((e) -> {
            // parse slice argument syntax
            String sliceExpr = txtListArg1.getText();
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
        });
        btnListJoin.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            String arg = txtListArg1.getText();
            statusBar.show("Joined " + clipboard.split("\n").length + " lines with '" + arg + "' in current clipboard");
            SysClipboard.write(clipboard.replace("\n", arg));
        });
        btnListCollapse.setOnAction((e) -> {
            Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.length() > 0 ).filter();
            statusBar.show("Collapsed " + result.getLeft().size() + " lines down to " + result.getRight().size() + " by removing empty lines in current clipboard");
        });
        btnListUniq.setOnAction((e) -> {
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
        });
        btnListContains.setOnAction((e) -> {
            String arg = txtListArg1.getText();
            Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.contains(arg) ).filter();
            statusBar.show("Filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
        });
        btnListRegex.setOnAction((e) -> {
            String rawRegex = txtListArg1.getText();
            String regex = "^.*" + rawRegex + ".*$";
            Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.matches(regex) ).filter();
            statusBar.show("Regex filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
        });
        btnListRegexFull.setOnAction((e) -> {
            String regex = txtListArg1.getText();
            Pair<List<String>, List<String>> result = new ClipboardAsListFilter( (line) -> line.matches(regex) ).filter();
            statusBar.show("Regex (full) filtered " + result.getLeft().size() + " lines down to " + result.getRight().size() + " in current clipboard");
        });

        // Log
        log = new TextArea();
        vbox.setVgrow(log, Priority.ALWAYS);

        vbox.getChildren().add(items);
        vbox.getChildren().add(btnRetrieve);
        vbox.getChildren().add(hbox);
        hbox.getChildren().addAll(lblBuffers, btnStore, btnPrepend, btnAppend, btnReplace, btnDelete, btnJoin, btnDiff, btnUp, btnDown);
        vbox.getChildren().add(new Label("System Clipboard:"));
        vbox.getChildren().add(modificationTabPane);
        vbox.getChildren().add(log);
        vbox.getChildren().add(statusBar);

        btnStore.setOnAction((e) -> {
            appendToClipBuffers(SysClipboard.read());
        });

        btnPrepend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.show("Prepend " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
                clips.set(i, new ClipBuffer(clipboard + clips.get(i).clip));
            }
        });

        btnAppend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.show("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, new ClipBuffer(clips.get(i).clip + clipboard));
            }
        });

        btnReplace.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.show("Replace " + indices.size() + " buffer(s) with " + clipboard.length() + " characters");
            for (Integer i : indices) {
                clips.set(i, new ClipBuffer(clipboard));
            }
        });

        btnRetrieve.setOnAction((e) -> {
            // Cycle through selected buffers

            // Give list control focus so we can see current focus, but focus makes no visual difference to items that are selected.
            // items.requestFocus();

            Integer focusIdx = items.getFocusModel().getFocusedIndex();
            List<Integer> selIdxs = items.getSelectionModel().getSelectedIndices();
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
                items.getFocusModel().focus(focusIdx);
                ordinalOfSelected = 0 + 1;
            } else {
                for (int i = 0; i < selIdxs.size(); ++i) {
                    if (selIdxs.get(i) >= focusIdx) {
                        focusIdx = selIdxs.get(i);
                        items.getFocusModel().focus(focusIdx);
                        ordinalOfSelected = i + 1;
                        break;
                    }
                }
            }

            // Set clipboard
            String clip = retrieveClipFromBuffer();
            System.out.println(clip);
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

            // Advance focus to next selected item
            if (focusIdx >= lastSelIdx) {
                focusIdx = selIdxs.get(0);
                items.getFocusModel().focus(focusIdx);
            } else {
                for (Integer selIdx : selIdxs) {
                    if (selIdx > focusIdx) {
                        focusIdx = selIdx;
                        items.getFocusModel().focus(focusIdx);
                        System.out.println("new focus " + focusIdx);
                        break;
                    }
                }
            }
        });

        btnDelete.setOnAction((e) -> {
            ObservableList<Integer> idxs = items.getSelectionModel().getSelectedIndices();
            int startingSize = idxs.size();
            // delete items in reverse index order to not offset indexes while iterating
            for (int i = idxs.size()-1; i >= 0; i--) {
                clips.remove((int) idxs.get(i));
            }
            statusBar.show("Deleted " + startingSize + " selected clip buffer(s)\n");
        });

        btnJoin.setOnAction((e) -> {
            ObservableList<ClipBuffer> selectedBuffers = items.getSelectionModel().getSelectedItems();
            ArrayList<String> clips = new ArrayList<String>();
            for (ClipBuffer buf : selectedBuffers) {
                clips.add(buf.clip);
            }
            String clip = String.join("\n", clips);
            String msg = String.format("Joining the %d selected buffers and storing %d chars to the clipboard",
                    selectedBuffers.size(),
                    clip.length()
            );
            SysClipboard.write(clip);
            statusBar.show(msg);
            items.getFocusModel().focus(1);
        });

        btnDiff.setOnAction((e) -> {
            ObservableList<ClipBuffer> selectedBuffers = items.getSelectionModel().getSelectedItems();
            if (selectedBuffers.size() != 2) {
                statusBar.showErr("Need two buffers selected to do a diff");
            } else {
                try {
                    Path fileA = Files.createTempFile(Config.DIFF_TEMP_FILE_A, Config.DIFF_TEMP_FILE_EXT);
                    Path fileB = Files.createTempFile(Config.DIFF_TEMP_FILE_B, Config.DIFF_TEMP_FILE_EXT);
                    // NOTE: Files.write() writes the file with linux-style line endings. Or, maybe it just passes
                    // through whatever is in the String is and doesn't automatically treat "\n" as "\r\n" on Windows.
                    Files.write(fileA, selectedBuffers.get(0).clip.getBytes());
                    Files.write(fileB, selectedBuffers.get(1).clip.getBytes());
                    Process proc = new ProcessBuilder(Config.DIFF_APP, fileA.toString(), fileB.toString()).start();
                    statusBar.show("Diffing the two selected buffers with " + Config.DIFF_APP);
                } catch(Exception exc) {
                    statusBar.showErr("Can't launch diff tool");
                }
            }
        });

        btnUp.setOnAction((e) -> {
            ObservableList<Integer> selectedIdxs = items.getSelectionModel().getSelectedIndices();
            ArrayList<Integer> selIdxs = new ArrayList(selectedIdxs); // create copy
            Integer topBoundIdx = 0;
            for (int i : selIdxs) {
                // Don't swap past top of list
                if (i <= topBoundIdx) {
                    continue;
                }
                // Don't swap if item above you is selected (for when selected items bunch at the top of the list)
                Integer prevIdx = i - 1;
                if (prevIdx >= topBoundIdx && items.getSelectionModel().isSelected(prevIdx)) {
                    continue;
                }
                swapBuffers(i, prevIdx);
            }
        });

        btnDown.setOnAction((e) -> {
            ObservableList<Integer> selectedIdxs = items.getSelectionModel().getSelectedIndices();
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
                if (nextIdx <= bottomBoundIdx && items.getSelectionModel().isSelected(nextIdx)) {
                    continue;
                }
                swapBuffers(i, nextIdx);
            }
        });

        Scene scene = new Scene(vbox, Config.APP_WIDTH, Config.APP_HEIGHT);
        primaryStage.setTitle(Config.APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.show();
        statusBar.cacheTextFillColor();

        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (ov.getValue()) {
                    if (chkStoreOnFocus.isSelected()) {
                        log.insertText(0, "got focus\n");
                        appendToClipBuffers(SysClipboard.read());
                    }
                }
            }
        });

        if (chkStoreOnFocus.isSelected()) {
            appendToClipBuffers(SysClipboard.read()); // read and store first clip when app first opens
        }

        MyAppFramework.dump(vbox);
    }

    private void initMenu(Pane root) {
        MenuBar menuBar = new MenuBar();
        root.getChildren().add(menuBar);

        // File Menu
        Menu fileMenu = new Menu("File");
        menuBar.getMenus().add(fileMenu);

        MenuItem stuffItem = new MenuItem("stuff");
        fileMenu.getItems().add(stuffItem);
        fileMenu.getItems().add(new SeparatorMenuItem());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction((e) -> {
            Platform.exit();
        });

        // Buffer Menu
        Menu bufferMenu = new Menu("Buffer");

        chkStoreOnFocus = new CheckMenuItem("Store clipboard to buffer when app gets focus");
        chkStoreOnFocus.setSelected(false);
        bufferMenu.getItems().add(chkStoreOnFocus);

        // MenuBar
        menuBar.getMenus().add(bufferMenu);
        fileMenu.getItems().add(exitItem);
    }

    private String retrieveClipFromBufferAndShowStatus() {
        if (items.getFocusModel().getFocusedItem() == null) {
            statusBar.showErr("No item selected");
            return "";
        }
        String msg = retrieveClipFromBuffer();
        statusBar.show(String.format("Retrieving %d line(s) and %d chars from buffer and storing to the clipboard\n",
                StringUtils.countMatches(msg, "\n") + 1,
                msg.length()));
        return msg;
    }

    private String retrieveClipFromBuffer() {
        ClipBuffer buffer = items.getFocusModel().getFocusedItem();
        SysClipboard.write(buffer.clip);
        return buffer.clip;
    }

    private void appendToClipBuffers(String clip) {
        statusBar.show(String.format("Storing %d line(s) and %d chars from the clipboard to a buffer\n",
                StringUtils.countMatches(clip, "\n") + 1,
                clip.length()));
        ClipBuffer buffer = new ClipBuffer(clip);
        clips.add(0, buffer);
        items.scrollTo(buffer);
    }

    private void swapBuffers(int idx1, int idx2) {
        // NOTE: this function does not attempt to do anything sane with focus. It seems that focus can cause issues
        // with extra rows getting selected as I move them around. I think this may have something to do with the focus?
        // I'm just guessing.
        ClipBuffer tmpBuff1 = clips.get(idx1);
        boolean tmpSelectState1 = items.getSelectionModel().isSelected(idx1);
        ClipBuffer tmpBuff2 = clips.get(idx2);
        boolean tmpSelectState2 = items.getSelectionModel().isSelected(idx2);

        // NOTE: if you change the code below to set idx1 value and then change its selection, then do the same for idx2,
        // moving with multiple items selected will often cause additional rows to get selected as you move. The order
        // of the code below seems to avoid this problem for some reason.
        clips.set(idx1, tmpBuff2);
        clips.set(idx2, tmpBuff1);

        if (tmpSelectState2) {
            items.getSelectionModel().select(idx1);
        } else {
            items.getSelectionModel().clearSelection(idx1);
        }

        if (tmpSelectState1) {
            items.getSelectionModel().select(idx2);
        } else {
            items.getSelectionModel().clearSelection(idx2);
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
}


