package com.github.textFinder;

import com.github.textFinder.utilities.AlertException;
import com.github.textFinder.utilities.FileEncoding;
import com.github.textFinder.utilities.TextFinderLogger;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileNotFoundException;
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
@SuppressWarnings("WeakerAccess")
public class FindTask extends Task<Map<File, List<String>>> {

    private List<Path> paths = new ArrayList<>();
    private final Map<File, List<String>> results = new HashMap<>();
    private Path userPath;
    private String textToFind;

    private final static Logger LOGGER = Logger.getLogger(FindTask.class.getName());

    static {
        LOGGER.setUseParentHandlers(false);
        FileHandler fh = TextFinderLogger.getFileHandler();
        LOGGER.addHandler(fh);
    }

    @Override
    protected Map<File, List<String>> call() throws Exception {

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
            List<String> fileResults = new ArrayList<>();
            try {
                if (FileEncoding.SINGLE_INSTANCE.contentIsText(file, true)) {
                    try {
                        Scanner scanner = new Scanner(file);
                        int lineNum = 0;
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            lineNum++;
                            if (line.contains(textToFind)) {
                                fileResults.add(lineNum + " " + line);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        new AlertException(Alert.AlertType.ERROR, "There was an error while scanning " +
                                file.getName() + " file\n" + "Maybe program could not access the file.\n" +
                                "Grant the rights to the program so that it can access the filesystem.");
                        LOGGER.log(Level.WARNING, "FileNotFoundException while reading" +
                                file.getName() + " file. StackTrace: " + Arrays.toString(e.getStackTrace()));
                    }
                }
            } catch (IOException e) {
                new AlertException(Alert.AlertType.ERROR, "There was an error while reading " +
                        file.getName() + " file\n" + "Maybe program could not access the file.\n" +
                        "Grant the rights to the program so that it can access the filesystem.");
                LOGGER.log(Level.WARNING, "IOException while reading" + file.getName() +
                        " file (checking if content is text). StackTrace: " + Arrays.toString(e.getStackTrace()));
            }
            if (!fileResults.isEmpty()) {
                results.put(file, fileResults);
            }
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
}
