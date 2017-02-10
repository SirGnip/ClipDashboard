import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.juxtaflux.MyAppFramework;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class StatusBar extends Label {
    public void show(String msg) {
        this.setTextFill(Color.BLACK);
        this.setText(msg);
    }
    public void showErr(String msg) {
        this.setTextFill(Color.RED);
        this.setText("ERROR: " + msg);
    }
}

public class ClipDashboard extends Application {

    private ListView items;
    private ObservableList<String> clips;
    private TextArea log;
    private StatusBar statusBar;
    private CheckMenuItem chkStoreOnFocus;

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setSpacing(5);

        statusBar = new StatusBar();

        initMenu(vbox);

        clips = FXCollections.observableArrayList (
                "abc", "def", "ghijklmnop", "q", "rstuv", "wxyz");
        items = new ListView(clips);
        items.setMinHeight(250);
        items.setMaxHeight(250);
        items.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        items.setOnMouseClicked((e) -> {
            // double click on ListView
            if (e.getClickCount() == 2) {
                retrieveClipFromBufferAndShowStatus();
            }
        });

        // Buffer operations
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));
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
        modificationTabPane.setMinHeight(120);

        // String operation tab
        Tab tab1 = new Tab("String operations");
        tab1.setClosable(false);
        modificationTabPane.getTabs().add(tab1);

        VBox strTabVBox = new VBox();
        strTabVBox.setSpacing(5);

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
        txtStrArg1.setPrefWidth(80);
        TextField txtStrArg2 = new TextField("\\n");
        txtStrArg2.setPrefWidth(80);
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
        // NOTE: List operations assume each "item" of the "list" is a line of text, each separated from the other by carriage returns.
        Tab tab2 = new Tab("List operations");
        tab2.setClosable(false);
        modificationTabPane.getTabs().add(tab2);

        VBox listTabVBox = new VBox();
        listTabVBox.setSpacing(5);

        HBox noArgListOpsHBox = new HBox();
        Button btnListLTrim = new Button("L");
        btnListLTrim.setTooltip(new Tooltip("Trim whitespace from the left/beginning of each line in the clipboard"));
        Button btnListTrim = new Button("trim");
        btnListTrim.setTooltip(new Tooltip("Trim whitespace from both sides of each line in the clipboard"));
        Button btnListRTrim = new Button("R");
        btnListRTrim.setTooltip(new Tooltip("Trim whitespace from the right/end of each line in the clipboard"));
        Button btnListSort = new Button("sort");
        btnListSort.setTooltip(new Tooltip("Alphabetically Sort the lines in the clipboard"));
        Button btnListReverse = new Button("reverse");
        btnListReverse.setTooltip(new Tooltip("Reverse the order of the lines in the clipboard"));
        Button btnListStats = new Button("stats");
        btnListStats.setTooltip(new Tooltip("Calculate basics stats on the lines in the clipboard"));
        noArgListOpsHBox.getChildren().addAll(btnListLTrim, btnListTrim, btnListRTrim, btnListSort, btnListReverse, btnListStats);

        HBox argsListOpsHBox = new HBox();
        Button btnListPrepend = new Button("prepend");
        btnListPrepend.setTooltip(new Tooltip("Prepend given text to the beginning of each line in the clipboard\n(arg1: text)"));
        Button btnListAppend = new Button("append");
        btnListAppend.setTooltip(new Tooltip("Append given text to the end of each line in the clipboard\n(arg1: text)"));
        Button btnListJoin = new Button("join");
        btnListJoin.setTooltip(new Tooltip("Join each line in the clipboard with the character string given\n(arg1: delimiter)"));
        Button btnListContains = new Button("contains");
        btnListContains.setTooltip(new Tooltip("Keep lines in the clipboard that contain the literal string\n(arg1: substring)"));
        Button btnListRegex = new Button("regex");
        btnListRegex.setTooltip(new Tooltip("Keep lines in the clipboard that contain a match to the regex\n(arg1: regex)"));
        Button btnListRegexFull = new Button("regex full");
        btnListRegexFull.setTooltip(new Tooltip("Keep lines in the clipboard that match the regex exactly. The regex must match the entire line.\n(arg1: regex)"));
        TextField txtListArg1 = new TextField("_");
        txtListArg1.setPrefWidth(80);
        argsListOpsHBox.getChildren().addAll(btnListPrepend, btnListAppend, btnListJoin, btnListContains, btnListRegex, btnListRegexFull, txtListArg1);

        listTabVBox.getChildren().addAll(noArgListOpsHBox, argsListOpsHBox);

        tab2.setContent(listTabVBox);
        btnListLTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.show("Left-trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, StringUtil.ltrim(list.get(i)));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.show("Trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, list.get(i).trim());
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListRTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.show("Right-trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, StringUtil.rtrim(list.get(i)));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListSort.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.show("Sorted " + list.size() + " lines in current clipboard");
            Collections.sort(list);
            SysClipboard.write(String.join("\n", list));
        });
        btnListReverse.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.show("Reversed " + list.size() + " lines in current clipboard");
            Collections.reverse(list);
            SysClipboard.write(String.join("\n", list));
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
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            String arg = txtListArg1.getText();
            statusBar.show("Prepended " + arg.length() + " character(s) to " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, arg + list.get(i));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListAppend.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            String arg = txtListArg1.getText();
            statusBar.show("Appended " + arg.length() + " character(s) to " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, list.get(i) + arg);
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListJoin.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            String arg = txtListArg1.getText();
            statusBar.show("Joined " + clipboard.split("\n").length + " lines with '" + arg + "' in current clipboard");
            SysClipboard.write(clipboard.replace("\n", arg));
        });
        btnListContains.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            List<String> filtered = new ArrayList();
            String arg = txtListArg1.getText();
            int origLineCount = list.size();
            for (String line : list) {
                if (line.contains(arg)) {
                    filtered.add(line);
                }
            }
            statusBar.show("Filtered " + origLineCount + " lines down to " + filtered.size() + " in current clipboard");
            SysClipboard.write(String.join("\n", filtered));

        });
        btnListRegex.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            List<String> filtered = new ArrayList();
            String regex = txtListArg1.getText();
            regex = "^.*" + regex + ".*$";
            int origLineCount = list.size();
            for (String line : list) {
                if (line.matches(regex)) {
                    filtered.add(line);
                }
            }
            statusBar.show("Regex filtered " + origLineCount + " lines down to " + filtered.size() + " in current clipboard");
            SysClipboard.write(String.join("\n", filtered));
        });
        btnListRegexFull.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            List<String> filtered = new ArrayList();
            String regex = txtListArg1.getText();
            int origLineCount = list.size();
            for (String line : list) {
                if (line.matches(regex)) {
                    filtered.add(line);
                }
            }
            statusBar.show("Regex (full) filtered " + origLineCount + " lines down to " + filtered.size() + " in current clipboard");
            SysClipboard.write(String.join("\n", filtered));
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
                clips.set(i, clipboard + clips.get(i));
            }
        });

        btnAppend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.show("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, clips.get(i) + clipboard);
            }
        });

        btnReplace.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.show("Replace " + indices.size() + " buffer(s) with " + clipboard.length() + " characters");
            for (Integer i : indices) {
                clips.set(i, clipboard);
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
                String msg = String.format("Retrieving %d chars from buffer (#%d of %d) and storing to the clipboard",
                        clip.length(),
                        ordinalOfSelected,
                        selIdxs.size()
                        );
                statusBar.show(msg);
            } else {
                statusBar.show("Retrieving " + clip.length() + " chars from buffer and storing to the clipboard");
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
            ObservableList<String> selectedBuffers = items.getSelectionModel().getSelectedItems();
            String clip = String.join("\n", selectedBuffers);
            String msg = String.format("Joining the %d selected buffers and storing %d chars to the clipboard",
                    selectedBuffers.size(),
                    clip.length()
            );
            SysClipboard.write(clip);
            statusBar.show(msg);
            items.getFocusModel().focus(1);
        });

        btnDiff.setOnAction((e) -> {
            ObservableList<String> selectedBuffers = items.getSelectionModel().getSelectedItems();
            if (selectedBuffers.size() != 2) {
                statusBar.showErr("Need two buffers selected to do a diff");
            } else {
                try {
                    Path fileA = Files.createTempFile("ClipDashboard_buffA_", ".txt");
                    Path fileB = Files.createTempFile("ClipDashboard_buffB_", ".txt");
                    // NOTE: Files.write() writes the file with linux-style line endings. Or, maybe it just writes whatever
                    // the String is and doesn't treat "\n" as "\r\n" on Windows.
                    Files.write(fileA, selectedBuffers.get(0).getBytes());
                    Files.write(fileB, selectedBuffers.get(1).getBytes());
                    Process proc = new ProcessBuilder("C:\\Program Files (x86)\\Meld\\Meld.exe", fileA.toString(), fileB.toString()).start();
                    statusBar.show("Diffing the two selected buffers");
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

        Scene scene = new Scene(vbox, 600, 700);
        primaryStage.setTitle("ClipDashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

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
        statusBar.show(String.format("Retrieving %d chars from buffer and storing to the clipboard\n", msg.length()));
        return msg;
    }

    private String retrieveClipFromBuffer() {
        Object selected = items.getFocusModel().getFocusedItem();
        String msg = ((String) selected);
        SysClipboard.write(msg);
        return msg;
    }

    private void appendToClipBuffers(String clip) {
        statusBar.show(String.format("Storing %d chars to a buffer from clipboard\n", clip.length()));
        clips.add(0, clip);
        items.scrollTo(clip);
    }

    private void swapBuffers(int idx1, int idx2) {
        // NOTE: this function does not attempt to do anything sane with focus. It seems that focus can cause issues
        // with extra rows getting selected as I move them around. I think this may have something to do with the focus?
        // I'm just guessing.
        String tmpString1 = clips.get(idx1);
        boolean tmpSelectState1 = items.getSelectionModel().isSelected(idx1);
        String tmpString2 = clips.get(idx2);
        boolean tmpSelectState2 = items.getSelectionModel().isSelected(idx2);

        // NOTE: if you chnage the code below to set idx1 value and then change its selection, then do the same for idx2,
        // moving with multiple items selected will often cause additional rows to get selected as you move. The order
        // of the code below seems to avoid this problem for some reason.
        clips.set(idx1, tmpString2);
        clips.set(idx2, tmpString1);

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
}


