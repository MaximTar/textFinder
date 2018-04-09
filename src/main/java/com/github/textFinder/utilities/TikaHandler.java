package com.github.textFinder.utilities;

import com.github.textFinder.model.FindTask;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by maxtar.
 * Used examples of recursive parsing from
 * https://wiki.apache.org/tika/RecursiveMetadata
 */
@SuppressWarnings("WeakerAccess")
public class TikaHandler {

    private File file;
    private FindTask task;

    public TikaHandler(File file, FindTask task) {
        this.file = file;
        this.task = task;
    }

    public String parse() throws IOException, SAXException, TikaException {
        TikaConfig tika = TikaConfig.getDefaultConfig();
        Parser parser = new AutoDetectParser(tika);

        ParserDecorator recurseWith = new RecursiveTrackingMetadataParser(parser, file.getAbsolutePath());
        ParseContext context = new ParseContext();
        context.set(Parser.class, recurseWith);

        ContentHandler content = new BodyContentHandler();
        InputStream stream = new FileInputStream(file);
//        TikaInputStream stream = TikaInputStream.get(new File(filePath));
        parser.parse(stream, content, new Metadata(), context);

        return content.toString();
    }

    @SuppressWarnings("WeakerAccess")
    private class RecursiveTrackingMetadataParser extends ParserDecorator {
        private String location;
        private int unknownCount = 0;

        public RecursiveTrackingMetadataParser(Parser parser, String location) {
            super(parser);
            this.location = location;
            if (!this.location.endsWith("/")) {
                this.location += "/";
            }
        }

        @Override
        public void parse(
                InputStream stream, ContentHandler ignore,
                Metadata metadata, ParseContext context)
                throws IOException, SAXException, TikaException {
            String objectName;
            if (metadata.get(TikaMetadataKeys.RESOURCE_NAME_KEY) != null) {
                objectName = metadata.get(TikaMetadataKeys.RESOURCE_NAME_KEY);
            } else if (metadata.get(TikaMetadataKeys.EMBEDDED_RELATIONSHIP_ID) != null) {
                objectName = metadata.get(TikaMetadataKeys.EMBEDDED_RELATIONSHIP_ID);
            } else {
                objectName = "embedded-" + (++unknownCount);
            }
            String objectLocation = this.location + objectName;

            ContentHandler content = new BodyContentHandler();
            Parser preContextParser = context.get(Parser.class);
            context.set(Parser.class, new RecursiveTrackingMetadataParser(getWrappedParser(), objectLocation));
            super.parse(stream, content, metadata, context);
            context.set(Parser.class, preContextParser);

            // fixme this hardcode is written to check if object is package
            // (https://tika.apache.org/1.11/formats.html#Full_list_of_Supported_Formats)
            if (!metadata.toString().contains("X-Parsed-By=org.apache.tika.parser.pkg.")) {
                String text = content.toString();
                List<String> fileResults = new ArrayList<>();
                Scanner scanner = new Scanner(text);
                int lineNum = 0;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    lineNum++;
                    if (line.contains(task.getTextToFind())) {
                        fileResults.add(lineNum + " " + line);
                    }
                }
                if (!fileResults.isEmpty()) {
                    task.getResults().put(objectLocation, fileResults);
                }
            }
        }
    }

}
