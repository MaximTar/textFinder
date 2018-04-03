package com.github.textFinder.utilities;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;

/**
 * Created by maxtar.
 */
public class TikaHandler {

    public static String parse(File file) throws IOException, SAXException, TikaException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata);
            System.out.println("!!!");
            System.out.println(parser.getClass().getName());
            return handler.toString();
        }

    }

    public String parseToString(File file) throws IOException, SAXException, TikaException {
        Tika tika = new Tika();
        try (InputStream stream = new FileInputStream(file.getAbsolutePath())) {
            return tika.parseToString(stream);
        }
    }


}
