package com.github.textFinder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/textFinderFirstView.fxml"));
        primaryStage.setTitle("Text Finder");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // Get your Tika object, eg
        Tika tika = new Tika();
        // Get the root parser
        CompositeParser parser = new CompositeParser();
        // Fetch the types it supports
        for (MediaType type : parser.getSupportedTypes(new ParseContext())) {
            String typeStr = type.toString();
        }
        // Fetch the parsers that make it up (note - may need to recurse if any are a CompositeParser too)
        for (Parser p : parser.getAllComponentParsers()) {
            String parserName = p.getClass().getName();
            System.out.println(parserName);
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
