package com.github.textFinder.controller;

import com.github.textFinder.model.FindTask;
import com.github.textFinder.utilities.AlertException;
import com.github.textFinder.utilities.TextFinderLogger;
import com.github.textFinder.view.ResultBox;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public class Controller implements Initializable {

    public enum TYPE {FILE, NAME, BOTH}

    @FXML
    private ToggleGroup groupWhereFind;
    @FXML
    private ToggleButton inFile;
    @FXML
    private ToggleButton inName;
    @FXML
    private ToggleButton both;
    @FXML
    private Label folderLabel;
    @FXML
    private TextField textToFind;
    @SuppressWarnings("FieldCanBeLocal")
    private final String folderLabelText = "Here will be the path to the folder";
    private Toggle lastSelected = new ToggleButton();

    private final static Logger LOGGER = Logger.getLogger(Controller.class.getName());

    static {
        LOGGER.setUseParentHandlers(false);
        FileHandler fh = TextFinderLogger.getFileHandler();
        LOGGER.addHandler(fh);
    }

    @FXML
    private void handleFindButtonAction(ActionEvent actionEvent) {
        if (Objects.equals(folderLabel.getText(), folderLabelText)) {
            new AlertException(Alert.AlertType.INFORMATION, "Select folder first!");
        } else if (textToFind.getText().isEmpty()) {
            new AlertException(Alert.AlertType.INFORMATION, "Enter text first!");
        } else {

            Path userPath = Paths.get(folderLabel.getText());

            FindTask findTask = new FindTask();
            findTask.setUserPath(userPath);
            findTask.setTextToFind(textToFind.getText());
            findTask.setType(groupWhereFind.getSelectedToggle().getUserData().toString());

            Stage progressStage = new Stage();
            HBox progressBox = new HBox();
            progressBox.setPadding(new Insets(20));
            ProgressBar progressBar = new ProgressBar();
            ProgressIndicator progressIndicator = new ProgressIndicator();
            final Label statusLabel = new Label();
            statusLabel.setMinWidth(250);
            statusLabel.setTextFill(Color.BLUE);
            progressBox.getChildren().addAll(progressBar, progressIndicator, statusLabel);
            progressStage.setScene(new Scene(progressBox));
            progressStage.show();

            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(findTask.progressProperty());
            progressIndicator.progressProperty().unbind();
            progressIndicator.progressProperty().bind(findTask.progressProperty());
            statusLabel.textProperty().unbind();
            statusLabel.textProperty().bind(findTask.messageProperty());

            findTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                    event -> {
                        progressStage.close();
                        Map<String, List<String>> results = findTask.getValue();
                        ResultBox resultBox = new ResultBox(results);
                        Node source = (Node) actionEvent.getSource();
                        Stage stage = (Stage) source.getScene().getWindow();
                        stage.setScene(new Scene(resultBox));
                    });

            new Thread(findTask).start();
        }
    }

    @FXML
    private void handleSelectFolderButtonAction(ActionEvent actionEvent) {
        Node source = (Node) actionEvent.getSource();
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        final File selectedDirectory = directoryChooser.showDialog(source.getScene().getWindow());
        if (selectedDirectory != null) {
            folderLabel.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        inFile.setUserData(TYPE.FILE);
        inName.setUserData(TYPE.NAME);
        both.setUserData(TYPE.BOTH);

        groupWhereFind.selectedToggleProperty().addListener((observableValue, toggle, t1) -> {
            if (groupWhereFind.getSelectedToggle() != null) {
                lastSelected = groupWhereFind.getSelectedToggle();
            } else if (groupWhereFind.getSelectedToggle() == null) {
                groupWhereFind.selectToggle(lastSelected);
            }
        });
    }
}
