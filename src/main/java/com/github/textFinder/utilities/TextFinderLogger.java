package com.github.textFinder.utilities;

import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by maxtar.
 */
public class TextFinderLogger {

    private static FileHandler fileHandler;
    private final static Logger LOGGER = Logger.getLogger(TextFinderLogger.class.getName());

    static {
        try {
            fileHandler = new FileHandler("log.log");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            new AlertException(Alert.AlertType.ERROR, "Program could not create log file.\n" +
                    "Grant the rights to the program so that it can access the filesystem.\n");
        }
    }

    public static FileHandler getFileHandler() {
        return fileHandler;
    }
}
