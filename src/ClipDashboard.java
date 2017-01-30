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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.juxtaflux.*;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

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

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));

        Button btnRetrieve = new Button("Retrieve");
        btnRetrieve.setMaxWidth(Double.MAX_VALUE);
        Button btnStore = new Button("Store");
        Button btnPrepend = new Button("Prepend");
        Button btnAppend = new Button("Append");
        Button btnReplace = new Button("Replace");
        Button btnDelete = new Button("Del");

        TabPane modificationTabPane = new TabPane();
        modificationTabPane.setMinHeight(120);
        Tab tab1 = new Tab("Clip operations");
        HBox tabHBox = new HBox();
        Button btnClipPrepend = new Button("prepend");
        TextField txtClipInput = new TextField("<txt>");
        btnClipPrepend.setOnAction((e) -> {
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Prepend to " + indices.size() + " buffer(s): " + txtClipInput.getText());
            for (Integer idx : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
                log.insertText(0, idx.toString() + "\n");
                clips.set(idx, txtClipInput.getText() + clips.get(idx));
            }
        });
        Button btnClipAppend = new Button("append");
        btnClipAppend.setOnAction((e) -> {
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Append to " + indices.size() + " buffer(s): " + txtClipInput.getText());
            for (Integer idx : indices) {
                log.insertText(0, idx.toString() + "\n");
                clips.set(idx, clips.get(idx) + txtClipInput.getText());
            }
        });


        tabHBox.getChildren().add(btnClipPrepend);
        tabHBox.getChildren().add(btnClipAppend);
        tabHBox.getChildren().add(txtClipInput);

        tab1.setContent(tabHBox);

        tab1.setClosable(false);
        modificationTabPane.getTabs().add(tab1);
        Tab tab2 = new Tab("Line operations");
        tab2.setClosable(false);
        modificationTabPane.getTabs().add(tab2);

        log = new TextArea();
        vbox.setVgrow(log, Priority.ALWAYS);

        vbox.getChildren().add(items);
        vbox.getChildren().add(btnRetrieve);
        vbox.getChildren().add(hbox);
        hbox.getChildren().addAll(btnStore, btnPrepend, btnAppend, btnReplace, btnDelete);
//        vbox.getChildren().add(chkStoreOnFocus);
        vbox.getChildren().add(modificationTabPane);
        vbox.getChildren().add(log);
        vbox.getChildren().add(statusBar);

        btnStore.setOnAction((e) -> {
            appendToClipBuffers(readSysClipboard());
        });

        btnPrepend.setOnAction((e) -> {
            String clipboard = readSysClipboard();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Prepend " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, clipboard + clips.get(i));
            }
        });

        btnAppend.setOnAction((e) -> {
            String clipboard = readSysClipboard();
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            statusBar.setText("Append " + clipboard.length() + " characters to " + indices.size() + " buffer(s)");
            for (Integer i : indices) {
                clips.set(i, clips.get(i) + clipboard);
            }
        });

        btnReplace.setOnAction((e) -> {
            String clipboard = readSysClipboard();
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


        Scene scene = new Scene(vbox, 500, 700);
        primaryStage.setTitle("ClipDashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (ov.getValue()) {
                    if (chkStoreOnFocus.isSelected()) {
                        log.insertText(0, "got focus\n");
                        appendToClipBuffers(readSysClipboard());
                    }
                }
            }
        });

        if (chkStoreOnFocus.isSelected()) {
            appendToClipBuffers(readSysClipboard()); // read and store first clip when app first opens
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
        storeToSysClipboard(msg);
    }

    private String readSysClipboard() {
        String msg = "";
        try {
            msg = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch(UnsupportedFlavorException|IOException exc) {
            log.insertText(0, "Read clipboard FAILED: " + exc.getClass().getName() + " " + exc);
        }
        return msg;
    }

    private void storeToSysClipboard(String s) {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }

    private void appendToClipBuffers(String clip) {
        statusBar.setText(String.format("Storing %d chars to a buffer from clipboard\n", clip.length()));
        clips.add(0, clip);
        items.scrollTo(clip);
    }
}
