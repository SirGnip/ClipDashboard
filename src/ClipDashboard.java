import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
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

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));

        ObservableList<String> clips = FXCollections.observableArrayList (
                "abc", "def");
        ListView items = new ListView(clips);
        items.setMinHeight(250);
        items.setMaxHeight(250);

        HBox hbox = new HBox();
        hbox.setPadding(new Insets(5));

        Button btnRead = new Button("Read");
        Button btnWrite = new Button("Write");
        Button btnClear = new Button("Clear");

        TextArea log = new TextArea();
        vbox.setVgrow(log, Priority.ALWAYS);

        vbox.getChildren().add(items);
        vbox.getChildren().add(hbox);
        hbox.getChildren().add(btnRead);
        hbox.getChildren().add(btnWrite);
        hbox.getChildren().add(btnClear);
        vbox.getChildren().add(log);

        btnRead.setOnAction((e) -> {
            try {
                String msg;
                msg = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                log.insertText(0, String.format("Read %d chars from clipboard\n", msg.length()));
                clips.add(msg);
                items.scrollTo(msg);
            } catch(UnsupportedFlavorException|IOException exc) {
                log.insertText(0, "Read clipboard FAILED: " + exc.getClass().getName() + " " + exc);
            }
        });

        btnWrite.setOnAction((e) -> {
            if (items.getSelectionModel().isEmpty()) {
                log.insertText(0, "No item selected\n");
                return;
            }
            Object selected = items.getSelectionModel().getSelectedItem();
            String msg = ((String) selected);
            log.insertText(0, String.format("Writing %d chars to the clipboard\n", msg.length()));
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

        MyAppFramework.dump(vbox);
    }
}
