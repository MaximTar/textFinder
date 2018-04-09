package com.github.textFinder.model;

import com.github.textFinder.utilities.*;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by maxtar.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class FindTask extends Task<Map<String, List<String>>> {

    private List<Path> paths = new ArrayList<>();
    private final Map<String, List<String>> results = new HashMap<>();
    private Path userPath;
    private String textToFind;

    private final static Logger LOGGER = Logger.getLogger(FindTask.class.getName());

    static {
        LOGGER.setUseParentHandlers(false);
        FileHandler fh = TextFinderLogger.getFileHandler();
        LOGGER.addHandler(fh);
    }

    @Override
    protected Map<String, List<String>> call() throws Exception {

        // todo think about second task to list files
        try {
            listFiles(userPath);
        } catch (IOException e) {
            new AlertException(Alert.AlertType.ERROR, "There was an error while reading chosen directory.\n" +
                    "Maybe program could not access the directory.\n" +
                    "Grant the rights to the program so that it can access the filesystem.");
            LOGGER.log(Level.WARNING, "IOException while reading chosen directory. StackTrace: "
                    + Arrays.toString(e.getStackTrace()));
        }

        long count = paths.size();
        long counter = 0;
        for (Path path : paths) {
            File file = new File(path.toString());
            TikaHandler handler = new TikaHandler(file, this);
            String text = handler.parse();
            counter++;
            this.updateProgress(counter, count);
        }
        return results;
    }

    private void listFiles(Path path) throws IOException {
        paths = Files.walk(Paths.get(path.toUri()))
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        // todo second variant, check later
//        paths = Files.find(Paths.get(path.toUri()),
//                Integer.MAX_VALUE,
//                (filePath, fileAttr) -> fileAttr.isRegularFile())
//                .collect(Collectors.toList());
    }

    private long countFiles(Path path) throws IOException {
        return Files.walk(path)
                .parallel()
                .filter(p -> !p.toFile().isDirectory())
                .count();
    }

    public void setUserPath(Path userPath) {
        this.userPath = userPath;
    }

    public void setTextToFind(String textToFind) {
        this.textToFind = textToFind;
    }

    public Map<String, List<String>> getResults() {
        return results;
    }

    public String getTextToFind() {
        return textToFind;
    }
}
