/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.textFinder.utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

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

/**
 * Examples of Recursive Parsing from
 * https://wiki.apache.org/tika/RecursiveMetadata
 */
public class RecursiveParsingExample {
    public static void main(String[] args) throws Exception {

        TikaConfig tika = TikaConfig.getDefaultConfig();
        Parser parser = new AutoDetectParser(tika);
        String file = "/home/maxtar/1test/testFolder/text.zip";
//        String file = "/home/maxtar/1test/test2.zip";

        ParserDecorator recurseWith;
        recurseWith = new RecursiveTrackingMetadataParser(parser, file);
        ParseContext context = new ParseContext();
        context.set(Parser.class, recurseWith);

        ContentHandler content = new BodyContentHandler();
        TikaInputStream stream = TikaInputStream.get(new File(file));
        parser.parse(stream, content, new Metadata(), context);
    }

    @SuppressWarnings("WeakerAccess")
    private static class RecursiveTrackingMetadataParser extends ParserDecorator {
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

            // Fetch the contents, and recurse if possible
            ContentHandler content = new BodyContentHandler();
            Parser preContextParser = context.get(Parser.class);
            context.set(Parser.class, new RecursiveTrackingMetadataParser(getWrappedParser(), objectLocation));
            super.parse(stream, content, metadata, context);
            context.set(Parser.class, preContextParser);

//            // Report what this one is
//            System.out.println("----");
//            System.out.println("Resource is " + objectLocation);
//            System.out.println("----");
//            System.out.println(metadata);
            // fixme this hardcode is written to check if object is package
            // (https://tika.apache.org/1.11/formats.html#Full_list_of_Supported_Formats)
            if (metadata.toString().contains("X-Parsed-By=org.apache.tika.parser.pkg.")) {
                System.out.println("CONTAINS!!!");
            } else {
                System.out.println("----");
                System.out.println(content.toString());
                System.out.println("----");
            }
        }
    }
}
