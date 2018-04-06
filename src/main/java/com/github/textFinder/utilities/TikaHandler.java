package com.github.textFinder.utilities;

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
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by maxtar.
 */
public class TikaHandler {

    private boolean isPackage = false;
    private String objectLocation;
    private File file;

    public TikaHandler(File file) {
        this.file = file;
    }

    public String parse() throws IOException, SAXException, TikaException {
        Metadata metadata = new Metadata();
        TikaConfig tika = TikaConfig.getDefaultConfig();
        Parser parser = new AutoDetectParser(tika);

        ParserDecorator recurseWith;
        recurseWith = new RecursiveTrackingMetadataParser(parser, file.getAbsolutePath());
        ParseContext context = new ParseContext();
        context.set(Parser.class, recurseWith);

        ContentHandler content = new BodyContentHandler();
        TikaInputStream stream = TikaInputStream.get(new File(file.getAbsolutePath()));
        parser.parse(stream, content, metadata, context);

//        System.out.println("METADATA = " + metadata);
//        System.out.println("CONTENT = " + content.toString());

//        if (metadata.toString().contains("X-Parsed-By=org.apache.tika.parser.pkg.")) {
//            System.out.println("CONTAINS!!!");
//        } else {
//            System.out.println("----");
//            System.out.println(content.toString());
//            System.out.println("----");
//        }

        return content.toString();
    }

    public boolean isPackage() {
        return isPackage;
    }

    public void setPackage(boolean isPackage) {
        this.isPackage = isPackage;
    }

    public String getObjectLocation() {
        return objectLocation;
    }

    public void setObjectLocation(String objectLocation) {
        this.objectLocation = objectLocation;
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
                InputStream stream, ContentHandler content,
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

            // Fetch the contents, and recurse if possible
            Parser preContextParser = context.get(Parser.class);
            context.set(Parser.class, new RecursiveTrackingMetadataParser(getWrappedParser(), objectLocation));
            super.parse(stream, content, metadata, context);
            context.set(Parser.class, preContextParser);

            // fixme this hardcode is written to check if object is package
            // (https://tika.apache.org/1.11/formats.html#Full_list_of_Supported_Formats)
            if (metadata.toString().contains("X-Parsed-By=org.apache.tika.parser.pkg.")) {
                setPackage(true);
            } else {
                setPackage(false);
            }


            // Report what this one is
            setObjectLocation(objectLocation);

//            if (metadata.toString().contains("X-Parsed-By=org.apache.tika.parser.pkg.")) {
//                System.out.println("CONTAINS!!!");
//            } else {
//                System.out.println("----");
//                System.out.println(content.toString());
//                System.out.println("----");
//            }
        }
    }

}
