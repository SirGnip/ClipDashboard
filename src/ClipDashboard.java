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

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setSpacing(5);

        initMenu(vbox);

        clips = FXCollections.observableArrayList (
                "abc", "def");
        items = new ListView(clips);
        items.setMinHeight(250);
        items.setMaxHeight(250);
        items.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));

        Button btnRetrieve = new Button("Retrieve");
        btnRetrieve.setMaxWidth(Double.MAX_VALUE);
        Button btnStore = new Button("Store");
        Button btnClear = new Button("Clear");
        CheckBox chkStoreOnFocus = new CheckBox("Store clip on receive focus");
        chkStoreOnFocus.setAllowIndeterminate(false);
        chkStoreOnFocus.setSelected(false);
        Tooltip tipStoreOnFocus = new Tooltip("Will automatically read the clipboard and store string when window receives the focus");
        chkStoreOnFocus.setTooltip(tipStoreOnFocus);

        TabPane modificationTabPane = new TabPane();
        modificationTabPane.setMinHeight(120);
        Tab tab1 = new Tab("Clip operations");
        HBox tabHBox = new HBox();
        Button btnClipPrepend = new Button("prepend");
        TextField txtClipInput = new TextField("<txt>");
        btnClipPrepend.setOnAction((e) -> {
            log.insertText(0, "Prepend: " + txtClipInput.getText() + "\n");
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
            for (Integer idx : indices) { // Can't use for loop with function that returns a generic? http://stackoverflow.com/questions/6271960/how-to-iterate-over-a-wildcard-generic
                log.insertText(0, idx.toString() + "\n");
                clips.set(idx, txtClipInput.getText() + clips.get(idx));
            }
        });
        Button btnClipAppend = new Button("append");
        btnClipAppend.setOnAction((e) -> {
            log.insertText(0, "Append: " + txtClipInput.getText() + "\n");
            ObservableList<Integer> indices = items.getSelectionModel().getSelectedIndices();
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
        hbox.getChildren().addAll(btnStore, btnClear);
        vbox.getChildren().add(chkStoreOnFocus);
        vbox.getChildren().add(modificationTabPane);
        vbox.getChildren().add(log);

        btnStore.setOnAction((e) -> {
            storeClipboard(readClipboard());
        });

        btnRetrieve.setOnAction((e) -> {
            if (items.getSelectionModel().isEmpty()) {
                log.insertText(0, "No item selected\n");
                return;
            }
            Object selected = items.getSelectionModel().getSelectedItem();
            String msg = ((String) selected);
            log.insertText(0, String.format("Retrieving %d chars and storing to the clipboard\n", msg.length()));
            StringSelection selection = new StringSelection(msg);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        });

        btnClear.setOnAction((e) -> {
            clips.clear();
            log.insertText(0, "Clear\n");
        });

        Scene scene = new Scene(vbox, 500, 600);
        primaryStage.setTitle("ClipDashboard");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (ov.getValue()) {
                    if (chkStoreOnFocus.isSelected()) {
                        log.insertText(0, "got focus\n");
                        storeClipboard(readClipboard());
                    }
                }
            }
        });

        if (chkStoreOnFocus.isSelected()) {
            storeClipboard(readClipboard()); // read and store first clip when app first opens
        }

        MyAppFramework.dump(vbox);
    }

    private void initMenu(Pane root) {
        MenuBar menuBar = new MenuBar();
        root.getChildren().add(menuBar);

        Menu miscMenu = new Menu("Misc");
        menuBar.getMenus().add(miscMenu);

        MenuItem stuffItem = new MenuItem("stuff");
        miscMenu.getItems().add(stuffItem);
        miscMenu.getItems().add(new SeparatorMenuItem());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction((e) -> {
            Platform.exit();
        });
        miscMenu.getItems().add(exitItem);
    }

    private String readClipboard() {
        String msg = "";
        try {
            msg = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch(UnsupportedFlavorException|IOException exc) {
            log.insertText(0, "Read clipboard FAILED: " + exc.getClass().getName() + " " + exc);
        }
        return msg;
    }

    private void storeClipboard(String clip) {
        log.insertText(0, String.format("Storing %d chars from clipboard\n", clip.length()));
        clips.add(0, clip);
        items.scrollTo(clip);
    }
}
