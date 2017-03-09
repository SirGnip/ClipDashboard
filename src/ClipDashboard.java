import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import com.juxtaflux.AppFramework;
import org.apache.commons.lang3.StringUtils;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

public class ClipDashboard extends Application {
    private ListView<ClipBuffer> buffers;
    private ObservableList<ClipBuffer> clips;
    private TextArea log;
    private StatusBar statusBar;
    private CheckMenuItem chkStoreOnFocus;
    private CheckMenuItem chkVariableSubstitution;

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setSpacing(Config.UI_SPACING);

        statusBar = new StatusBar();

        initMenu(vbox, primaryStage);

        clips = FXCollections.observableArrayList(Config.INITIAL_CLIPS.stream().map(c -> new ClipBuffer(c)).collect(Collectors.toList()));
        buffers = new ListView(clips);
        buffers.setTooltip(new Tooltip("A list of clipboard buffers.\nDelete key will delete selected buffers."));
        buffers.setMinHeight(Config.LIST_VIEW_HEIGHT);
        buffers.setMaxHeight(Config.LIST_VIEW_HEIGHT);
        buffers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        buffers.setOnKeyPressed(((e) -> {
            if (e.getCode() == KeyCode.DELETE) {
                ObservableList<Integer> idxs = buffers.getSelectionModel().getSelectedIndices();
                int startingSize = idxs.size();
                // delete items in reverse index order to not offset indexes while iterating
                for (int i = idxs.size()-1; i >= 0; i--) {
                    clips.remove((int) idxs.get(i));
                }
                statusBar.show("Deleted " + startingSize + " selected clip buffer(s)\n");
            }
        }));

        buffers.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2) {
                // double click on ListView
                try {
                    String clip = chkVariableSubstitution.isSelected() ? retrieveVarSubstitutedClipFromBuffer() : retrieveClipFromBuffer();
                    statusBar.show("Retrieving " + (StringUtils.countMatches(clip, "\n") + 1) + " line(s) and " + clip.length() + " chars from buffer and storing to the clipboard");
                } catch (IllegalArgumentException exc) {
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
        });
        buffers.setOnDragOver((e) -> {
            // Should accept files, directories, URL's, strings
            e.acceptTransferModes(TransferMode.ANY);
            e.consume();
        });
        buffers.setOnDragEntered((e) -> {
            if (Config.DEBUG) { Debug.dumpDragboard(e.getDragboard()); }
            buffers.setOpacity(Config.DRAG_N_DROP_ENTER_OPACITY);
            e.consume();
        });
        buffers.setOnDragExited((e) -> {
            buffers.setOpacity(Config.DRAG_N_DROP_EXIT_OPACITY);
            e.consume();
        });
        buffers.setOnDragDropped((e) -> {
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
        });

        // Buffer operations
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(Config.UI_SPACING));
        Label lblBuffers = new Label("Buffers: ");
        Button btnRetrieve = new Button("Retrieve");
        btnRetrieve.setMinHeight(45);
        btnRetrieve.setStyle("-fx-font-size: 1.5em");
        btnRetrieve.setMaxWidth(Double.MAX_VALUE);
        btnRetrieve.setTooltip(
                new Tooltip(
                        "Retrieve contents of selected buffer and store to clipboard.\n" +
                        "If multiple buffers are selected, cycle through each buffer in turn.\n" +
                        "Double clicking a buffer is a shortcut for \"Retrieve\".\n" +
                        "(See \"Join\" to store all selected buffers in clipboard)"
                ));
        Button btnStore = new Button("store");
        Button btnReplace = new Button("replace");
        Button btnPrepend = new Button("prepend");
        Button btnAppend = new Button("append");
        Button btnJoin = new Button("join");
        Button btnDiff = new Button("diff");
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
        Button btnStrWordWrap = new Button("word wrap");
        Button btnStrSplit = new Button("split");
        Button btnStrReplace = new Button("replace");
        Button btnStrRegexReplace = new Button("regex repl");
        btnStrRegexReplace.setTooltip(new Tooltip("Replace string matched by given regex with replacement string. Supports backreferences in replacement string\nExample: '$2 $1'"));

        TextField txtStrArg1 = new TextField(",");
        txtStrArg1.setPrefWidth(Config.ARG_WIDTH);
        TextField txtStrArg2 = new TextField("\\n");
        txtStrArg2.setPrefWidth(Config.ARG_WIDTH);
        argsStrOpsHBox.getChildren().addAll(btnStrPrepend, btnStrAppend, btnStrWordWrap, btnStrReplace, btnStrRegexReplace, txtStrArg1, txtStrArg2);

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
        btnStrWordWrap.setOnAction((e) -> {
            try {
                int width = Integer.valueOf(txtStrArg1.getText());
                statusBar.show("Word wrapped the current clipboard contents to " + width + " columns wide");
                SysClipboard.write(WordUtils.wrap(SysClipboard.read(), width));
            } catch (NumberFormatException exc) {
                statusBar.showErr("Invalid argument for word wrap. It must be an integer. (" + exc.getMessage() + ")");
            }
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
        btnStrRegexReplace.setOnAction((e) -> {
            String regex = txtStrArg1.getText();
            String repl = txtStrArg2.getText();
            statusBar.show("Replaced regex '" + regex + "' with '" + repl + "' in current clipboard");
            SysClipboard.write(SysClipboard.read().replaceAll(regex, repl));
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
        Button btnListStore = new Button("store");
        btnListStore.setTooltip(new Tooltip("Store each line from the clipboard into a separate buffer"));
        noArgListOpsHBox.getChildren().addAll(btnListLTrim, btnListTrim, btnListRTrim, btnListCollapse, btnListUniq, btnListSort, btnListReverse, btnListStats, btnListStore);

        HBox argsListOpsHBox = new HBox();
        Button btnListPrepend = new Button("prepend");
        btnListPrepend.setTooltip(new Tooltip("Prepend given text to the beginning of each line in the clipboard\n(arg1: text)"));
        Button btnListAppend = new Button("append");
        btnListAppend.setTooltip(new Tooltip("Append given text to the end of each line in the clipboard\n(arg1: text)"));
        Button btnListCenter = new Button("center");
        btnListCenter.setTooltip(new Tooltip("Center each line in the clipboard\n(arg1: width)"));
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
        Button btnListRegexRepl = new Button("regex repl");
        btnListRegexRepl.setTooltip(new Tooltip("Replace string matched by given regex with replacement string in each line in the clipboard. Replacement string supports backreferences in replacement string\n(arg1: regex, arg2: replacement_string)\nExample regex='(\\S+) (\\S+)': repl='$2 $1'"));
        TextField txtListArg1 = new TextField("_");
        txtListArg1.setPrefWidth(Config.ARG_WIDTH);
        TextField txtListArg2 = new TextField("_");
        txtListArg2.setPrefWidth(Config.ARG_WIDTH);
        argsListOpsHBox.getChildren().addAll(btnListPrepend, btnListAppend, btnListCenter, btnListSlice, btnListJoin, btnListContains, btnListRegex, btnListRegexFull, btnListRegexRepl, txtListArg1, txtListArg2);

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
        btnListStore.setOnAction((e) -> {
            List<String> lines = SysClipboard.readAsLines();
            Collections.reverse(lines);
            for (String line : lines) {
                appendToClipBuffers(line);
            }
            statusBar.show("Stored " + lines.size() + " line(s) into individual buffers");
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
        btnListCenter.setOnAction((e) -> {
            try {
                int width = Integer.valueOf(txtListArg1.getText());
                List<String> result = new ClipboardAsListMutatorByLine( (line) -> StringUtils.center(line, width) ).mutate();
                statusBar.show("Centered " + result.size() + " lines in current clipboard");
            } catch (NumberFormatException exc) {
                statusBar.showErr("Invalid argument for center. It must be an integer. (" + exc.getMessage() + ")");
            }
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
        btnListRegexRepl.setOnAction((e) -> {
            String regex = txtListArg1.getText();
            String repl = txtListArg2.getText();
            List<String> result = new ClipboardAsListMutatorByLine( (line) -> line.replaceAll(regex, repl)).mutate();
            statusBar.show("Replaced regex '" + regex + "' with '" + repl + "' in lines in current clipboard");
        });

        Tab tabActions = new Tab("Actions");
        tabActions.setClosable(false);
        modificationTabPane.getTabs().add(tabActions);

        VBox actionTabVBox = new VBox();
        actionTabVBox.setSpacing(Config.UI_SPACING);

        HBox actionTabHBox = new HBox();
        Button btnNotepadAction = new Button("notepad");
        btnNotepadAction.setTooltip(new Tooltip("Open contents of system clipboard in Notepad"));
        Button btnOpenUrl = new Button("open URL's");
        btnOpenUrl.setTooltip(new Tooltip("Open contents of system clipboard as URL's (supports newline separated lists of URI's)"));
        Button btnOpenFiles = new Button("open files/folders");
        btnOpenFiles.setTooltip(new Tooltip("Open contents of system clipboard as files/folders (supports newline separated lists of paths)"));

        actionTabHBox.getChildren().addAll(btnNotepadAction, btnOpenUrl, btnOpenFiles);
        actionTabVBox.getChildren().addAll(actionTabHBox);
        tabActions.setContent(actionTabVBox);

        btnNotepadAction.setOnAction((e) -> {
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
        });
        btnOpenUrl.setOnAction((e) -> {
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
        });
        btnOpenFiles.setOnAction((e) -> {
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
        });

        // Log
        log = new TextArea();
        vbox.setVgrow(log, Priority.ALWAYS);

        vbox.getChildren().add(buffers);
        vbox.getChildren().add(btnRetrieve);
        vbox.getChildren().add(hbox);
        hbox.getChildren().addAll(lblBuffers, btnStore, btnReplace, btnPrepend, btnAppend, btnJoin, btnDiff, btnUp, btnDown);
        vbox.getChildren().add(new Label("System Clipboard:"));
        vbox.getChildren().add(modificationTabPane);
        vbox.getChildren().add(log);
        vbox.getChildren().add(statusBar);

        btnStore.setOnAction((e) -> {
            appendToClipBuffersAndShowStatus(SysClipboard.read());
        });

        btnPrepend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
            statusBar.show("Prepend " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
                clips.set(i, new ClipBuffer(clipboard + clips.get(i).clip));
            }
        });

        btnAppend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
            statusBar.show("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, new ClipBuffer(clips.get(i).clip + clipboard));
            }
        });

        btnReplace.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = buffers.getSelectionModel().getSelectedIndices();
            statusBar.show("Replace " + indices.size() + " buffer(s) with " + clipboard.length() + " characters");
            for (Integer i : indices) {
                clips.set(i, new ClipBuffer(clipboard));
            }
        });

        btnRetrieve.setOnAction((e) -> {
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
            } catch (IllegalArgumentException exc) {
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
        });

        btnRetrieve.setOnDragOver((e) -> {
            if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
                e.acceptTransferModes(TransferMode.ANY);
            }
            e.consume();
        });
        btnRetrieve.setOnDragEntered((e) -> {
            if (Config.DEBUG) { Debug.dumpDragboard(e.getDragboard()); }
            if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
                btnRetrieve.setOpacity(Config.DRAG_N_DROP_ENTER_OPACITY);
            }
            e.consume();
        });
        btnRetrieve.setOnDragExited((e) -> {
            if (shouldAcceptDropOnRetrieve(e.getDragboard())) {
                btnRetrieve.setOpacity(Config.DRAG_N_DROP_EXIT_OPACITY);
            }
            e.consume();
        });
        btnRetrieve.setOnDragDropped((e) -> {
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
        });

        btnJoin.setOnAction((e) -> {
            ObservableList<ClipBuffer> selectedBuffers = buffers.getSelectionModel().getSelectedItems();
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
            buffers.getFocusModel().focus(1);
        });

        btnDiff.setOnAction((e) -> {
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
        });

        btnUp.setOnAction((e) -> {
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
        });

        btnDown.setOnAction((e) -> {
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
                        appendToClipBuffersAndShowStatus(SysClipboard.read());
                    }
                }
            }
        });

        if (chkStoreOnFocus.isSelected()) {
            appendToClipBuffersAndShowStatus(SysClipboard.read()); // read and store first clip when app first opens
        }

        AppFramework.dump(vbox);
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

    private void initMenu(Pane root, Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        root.getChildren().add(menuBar);

        // File Menu
        Menu fileMenu = new Menu("File");
        menuBar.getMenus().add(fileMenu);

        MenuItem saveAllBuffers = new MenuItem("Save all buffers");
        fileMenu.getItems().add(saveAllBuffers);
        MenuItem saveSelectedBuffers = new MenuItem("Save selected buffers");
        fileMenu.getItems().add(saveSelectedBuffers);
        fileMenu.getItems().add(new SeparatorMenuItem());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction((e) -> {
            Platform.exit();
        });
        saveAllBuffers.setOnAction((e) -> {
            saveClipsToDisk(clips, primaryStage);
        });
        saveSelectedBuffers.setOnAction((e) -> {
            saveClipsToDisk(buffers.getSelectionModel().getSelectedItems(), primaryStage);
        });

        // Buffer Menu
        Menu bufferMenu = new Menu("Buffer");

        chkStoreOnFocus = new CheckMenuItem("Store clipboard to buffer when app gets focus");
        chkStoreOnFocus.setSelected(false);
        bufferMenu.getItems().add(chkStoreOnFocus);

        chkVariableSubstitution = new CheckMenuItem("Substitute variables (ex: {0}, {1}) when retrieving a buffer into the clipboard");
        chkVariableSubstitution.setSelected(false);
        bufferMenu.getItems().add(chkVariableSubstitution);

        // MenuBar
        menuBar.getMenus().add(bufferMenu);
        fileMenu.getItems().add(exitItem);
    }

    private String retrieveClipFromBuffer() {
        ClipBuffer buffer = buffers.getFocusModel().getFocusedItem();
        SysClipboard.write(buffer.clip);
        return buffer.clip;
    }

    private String retrieveVarSubstitutedClipFromBuffer() {
        ClipBuffer buffer = buffers.getFocusModel().getFocusedItem();
        String txt = MessageFormat.format(buffer.clip, clips.toArray()); // tolerant of variables in string that don't have a value for... Just doesn't substitute that variable.
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


