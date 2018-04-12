package com.github.textFinder.model;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
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

    public void parse()
            throws IOException, SAXException, TikaException, ParserConfigurationException, TransformerException {
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        Parser parser = new AutoDetectParser(tikaConfig);

        ParserDecorator recurseWith = new RecursiveTrackingMetadataParser(parser, file.getAbsolutePath());
        ParseContext context = new ParseContext();
        context.set(Parser.class, recurseWith);

        ContentHandler content = new BodyContentHandler();
        InputStream stream = new FileInputStream(file);

        Tika tika = new Tika();
        String fileType = tika.detect(file);

        if (fileType.contains("spreadsheet")) {
            parseSpreadsheet();
        } else {
//            parser.parse(stream, content, new Metadata(), context);
            recurseWith.parse(stream, content, new Metadata(), context);
        }
    }

    private String parseSpreadsheet()
            throws IOException, SAXException, TikaException, ParserConfigurationException, TransformerException {
        AutoDetectParser parser = new AutoDetectParser();
        ContentHandler handler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();

        // todo add xml reader

        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata);
//            System.out.println(handler.toString());
            SpreadsheetParser spreadsheetParser = new SpreadsheetParser(handler.toString());
            List<String> fileResults = spreadsheetParser.parse();
            if (!fileResults.isEmpty()) {
                task.getResults().put(file.getAbsolutePath(), fileResults);
            }
            return handler.toString();
        }
    }

    @SuppressWarnings("WeakerAccess")
    private class RecursiveTrackingMetadataParser extends ParserDecorator {
        private String location;
//        private int unknownCount = 0;

        private RecursiveTrackingMetadataParser(Parser parser, String location) {
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
            String objectName = "";
            if (metadata.get(TikaMetadataKeys.RESOURCE_NAME_KEY) != null) {
                objectName = metadata.get(TikaMetadataKeys.RESOURCE_NAME_KEY);
            } else if (metadata.get(TikaMetadataKeys.EMBEDDED_RELATIONSHIP_ID) != null) {
                objectName = metadata.get(TikaMetadataKeys.EMBEDDED_RELATIONSHIP_ID);
            }
//            else {
//                objectName = "embedded-" + (++unknownCount);
//            }
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
                        fileResults.add("line " + lineNum + ": " + line);
                    }
                }
                if (!fileResults.isEmpty()) {
                    task.getResults().put(objectLocation, fileResults);
                }
            }
        }
    }

    private class SpreadsheetParser {

        private String input;

        private SpreadsheetParser(String spreadsheet) {
            this.input = spreadsheet;
        }

        private List<String> parse()
                throws ParserConfigurationException, IOException, SAXException, TransformerException {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputStream stream = IOUtils.toInputStream(input);
            Document doc = dBuilder.parse(stream);
            doc.getDocumentElement().normalize();

            NodeList tableList = doc.getElementsByTagName("table");
            NodeList namesList = doc.getElementsByTagName("h1");

            List<String> fileResults = new ArrayList<>();
            for (int i = 0; i < tableList.getLength(); i++) {
                Node tableNode = tableList.item(i);
                if (tableNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element table = (Element) tableNode;
                    NodeList trList = table.getElementsByTagName("tr");
                    for (int j = 0; j < trList.getLength(); j++) {
                        Node trNode = trList.item(j);
                        if (trNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element tr = (Element) trNode;
                            NodeList tdList = tr.getElementsByTagName("td");
                            String line = "";
                            for (int k = 0; k < tdList.getLength(); k++) {
                                line += tdList.item(k).getTextContent() + " ";
                                line = line.replace(System.getProperty("line.separator"), "");
                            }
                            if (line.contains(task.getTextToFind())) {
                                String pageName;
                                if (namesList.getLength() == tableList.getLength()) {
                                    pageName = namesList.item(i).getTextContent();
                                } else {
                                    pageName = Integer.toString(i);
                                }
                                fileResults.add("Sheet " + pageName + ", line " + (j + 1) + ": " + line);
                            }
                        } else {
                            fileResults = parseAsText(doc);
                        }
                    }
                } else {
                    fileResults = parseAsText(doc);
                }
            }
            return fileResults;
        }

        private List<String> parseAsText(Document doc) throws TransformerException {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String stringDoc = writer.getBuffer().toString();
            String[] lines = stringDoc.split(System.getProperty("line.separator"));

            List<String> fileResults = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains(task.getTextToFind())) {
                    fileResults.add("line " + (i + 1) + ": " + line);
                }
            }
            // todo add info about document (that parser could not read properly)

            return fileResults;
        }
    }
}
