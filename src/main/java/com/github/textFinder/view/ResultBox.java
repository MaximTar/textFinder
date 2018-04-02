package com.github.textFinder.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by maxtar on 02.04.18.
 */
public class ResultBox extends VBox {

    public ResultBox(Map<File, List<String>> map) {

        setPadding(new Insets(25));
        setSpacing(10);

        for (Map.Entry<File, List<String>> entry : map.entrySet()) {
            Label label = new Label(entry.getKey().getAbsolutePath());
            VBox vBox = new VBox();
            vBox.setSpacing(5);
            for (String string : entry.getValue()) {
                Text text = new Text(string);
                vBox.getChildren().add(text);
            }
            getChildren().addAll(label, vBox);
        }
    }
}
