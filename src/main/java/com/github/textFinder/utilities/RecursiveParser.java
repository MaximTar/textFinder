package com.github.textFinder.utilities;

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by maxtar on 03.04.18.
 */
public class RecursiveParser {

    public static void main(String[] args) throws Exception {
        Parser parser = new RecursiveMetadataParser(new AutoDetectParser());
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        ContentHandler handler = new DefaultHandler();
        Metadata metadata = new Metadata();

        try (InputStream stream = TikaInputStream.get(new File("/home/maxtar/1test/test.zip"))) {
            parser.parse(stream, handler, metadata, context);
        }
    }

    private static class RecursiveMetadataParser extends ParserDecorator {
        public RecursiveMetadataParser(Parser parser) {
            super(parser);
        }

        @Override
        public void parse(
                InputStream stream, ContentHandler ignore,
                Metadata metadata, ParseContext context)
                throws IOException, SAXException, TikaException {
            ContentHandler content = new BodyContentHandler();
            super.parse(stream, content, metadata, context);
            System.out.println("----");
            System.out.println(metadata);
            System.out.println("----");
            System.out.println(content.toString());
        }
    }
}
