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
import javafx.stage.Stage;
import com.juxtaflux.MyAppFramework;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClipDashboard extends Application {

    private ListView items;
    private ObservableList<String> clips;
    private TextArea log;
    private Label statusBar;
    private CheckMenuItem chkStoreOnFocus;

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setSpacing(5);

        statusBar = new Label();

        initMenu(vbox);

        clips = FXCollections.observableArrayList (
                "abc", "def", "ghijklmnop", "q", "rstuv", "wxyz");
        items = new ListView(clips);
        items.setMinHeight(250);
        items.setMaxHeight(250);
        items.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        items.setOnMouseClicked((e) -> {
            if (e.getClickCount() == 2) {
                retrieveClipFromBuffer();
            }
        });

        // Buffer operations
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));
        Label lblBuffers = new Label("Buffers: ");
        Button btnRetrieve = new Button("Retrieve");
        btnRetrieve.setMaxWidth(Double.MAX_VALUE);
        Button btnStore = new Button("Store");
        Button btnPrepend = new Button("Prepend");
        Button btnAppend = new Button("Append");
        Button btnReplace = new Button("Replace");
        Button btnDelete = new Button("Del");

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
            statusBar.setText("Left-trimmed current clipboard contents");
            SysClipboard.write(StringUtil.ltrim(SysClipboard.read()));
        });
        btnStrTrim.setOnAction((e) -> {
            statusBar.setText("Trimmed current clipboard contents");
            SysClipboard.write(SysClipboard.read().trim());
        });
        btnStrRTrim.setOnAction((e) -> {
            statusBar.setText("Right-trimmed current clipboard contents");
            SysClipboard.write(StringUtil.rtrim(SysClipboard.read()));
        });
        btnStrLower.setOnAction((e) -> {
            statusBar.setText("Lower-cased current clipboard contents");
            SysClipboard.write(SysClipboard.read().toLowerCase());
        });
        btnStrUpper.setOnAction((e) -> {
            statusBar.setText("Upper-cased current clipboard contents");
            SysClipboard.write(SysClipboard.read().toUpperCase());
        });
        btnStrPrepend.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            statusBar.setText("Prepended " + arg.length() + " character to current clipboard");
            SysClipboard.write(arg + SysClipboard.read());
        });
        btnStrAppend.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            statusBar.setText("Appended " + arg.length() + " character to current clipboard");
            SysClipboard.write(SysClipboard.read() + arg);
        });
        btnStrSplit.setOnAction((e) -> {
            String arg = txtStrArg1.getText();
            String clipboard = SysClipboard.read();
            int origSize = clipboard.length();
            clipboard = clipboard.replace(arg, "\n");
            String[] array = clipboard.split("\n");
            statusBar.setText("Split " + origSize + " character(s) using '" + arg + "' into " + array.length + " line(s) in current clipboard");
            SysClipboard.write(clipboard);
        });
        btnStrReplace.setOnAction((e) -> {
            String trg = txtStrArg1.getText();
            String repl = txtStrArg2.getText();
            trg = StringUtil.replaceSpecialChars(trg);
            repl = StringUtil.replaceSpecialChars(repl);
            statusBar.setText("Replaced '" + trg + "' with '" + repl + "' in current clipboard");
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
        Button btnListTrim = new Button("trim");
        Button btnListRTrim = new Button("R");
        Button btnListSort = new Button("sort");
        Button btnListReverse = new Button("reverse");
        Button btnListStats = new Button("stats");
        noArgListOpsHBox.getChildren().addAll(btnListLTrim, btnListTrim, btnListRTrim, btnListSort, btnListReverse, btnListStats);

        HBox argsListOpsHBox = new HBox();
        Button btnListPrepend = new Button("prepend");
        Button btnListAppend = new Button("append");
        Button btnListJoin = new Button("join");
        TextField txtListArg1 = new TextField("_");
        txtListArg1.setPrefWidth(80);
        argsListOpsHBox.getChildren().addAll(btnListPrepend, btnListAppend, btnListJoin, txtListArg1);

        listTabVBox.getChildren().addAll(noArgListOpsHBox, argsListOpsHBox);

        tab2.setContent(listTabVBox);
        btnListLTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.setText("Left-trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, StringUtil.ltrim(list.get(i)));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.setText("Trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, list.get(i).trim());
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListRTrim.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.setText("Right-trimmed " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, StringUtil.rtrim(list.get(i)));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListSort.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.setText("Sorted " + list.size() + " lines in current clipboard");
            Collections.sort(list);
            SysClipboard.write(String.join("\n", list));
        });
        btnListReverse.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            statusBar.setText("Reversed " + list.size() + " lines in current clipboard");
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
            statusBar.setText(msg);
        });
        btnListPrepend.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            String arg = txtListArg1.getText();
            statusBar.setText("Prepended " + arg.length() + " character(s) to " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, arg + list.get(i));
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListAppend.setOnAction((e) -> {
            String[] array = SysClipboard.read().split("\n");
            List<String> list = Arrays.asList(array);
            String arg = txtListArg1.getText();
            statusBar.setText("Appended " + arg.length() + " character(s) to " + list.size() + " lines in current clipboard");
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, list.get(i) + arg);
            }
            SysClipboard.write(String.join("\n", list));
        });
        btnListJoin.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            String arg = txtListArg1.getText();
            statusBar.setText("Joined " + clipboard.split("\n").length + " lines with '" + arg + "' in current clipboard");
            SysClipboard.write(clipboard.replace("\n", arg));
        });

        // Log
        log = new TextArea();
        vbox.setVgrow(log, Priority.ALWAYS);

        vbox.getChildren().add(items);
        vbox.getChildren().add(btnRetrieve);
        vbox.getChildren().add(hbox);
        hbox.getChildren().addAll(lblBuffers, btnStore, btnPrepend, btnAppend, btnReplace, btnDelete);
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
            statusBar.setText("Prepend " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
                clips.set(i, clipboard + clips.get(i));
            }
        });

        btnAppend.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, clips.get(i) + clipboard);
            }
        });

        btnReplace.setOnAction((e) -> {
            String clipboard = SysClipboard.read();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Replace " + indices.size() + " buffer(s) with " + clipboard.length() + " characters");
            for (Integer i : indices) {
                clips.set(i, clipboard);
            }
        });

        btnRetrieve.setOnAction((e) -> {
            retrieveClipFromBuffer();
        });

        btnDelete.setOnAction((e) -> {
            ObservableList<Integer> idxs = items.getSelectionModel().getSelectedIndices();
            int startingSize = idxs.size();
            // delete items in reverse index order to not offset indexes while iterating
            for (int i = idxs.size()-1; i >= 0; i--) {
                clips.remove((int) idxs.get(i));
            }
            statusBar.setText("Deleted " + startingSize + " selected clip buffer(s)\n");
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

    private void retrieveClipFromBuffer() {
        if (items.getSelectionModel().isEmpty()) {
            statusBar.setText("No item selected");
            return;
        }
        Object selected = items.getSelectionModel().getSelectedItem();
        String msg = ((String) selected);
        statusBar.setText(String.format("Retrieving %d chars from buffer and storing to the clipboard\n", msg.length()));
        SysClipboard.write(msg);
    }

    private void appendToClipBuffers(String clip) {
        statusBar.setText(String.format("Storing %d chars to a buffer from clipboard\n", clip.length()));
        clips.add(0, clip);
        items.scrollTo(clip);
    }
}
