package org.researchobject.combinearchive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.purl.wf4ever.robundle.Bundle;
import org.purl.wf4ever.robundle.Bundles;
import org.purl.wf4ever.robundle.manifest.Manifest;
import org.purl.wf4ever.robundle.manifest.PathMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CombineToRO {

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			Path file = Paths.get(arg);
			combineToRO(file);
		}
	}

	public static void combineToRO(Path file) throws IOException, SAXException, ParserConfigurationException {		
		try (Bundle bundle = Bundles.openBundle(file)) {
			Path manifestXml = bundle.getFileSystem().getPath("/manifest.xml");
			if (Files.exists(manifestXml)) {
				Manifest roManifest = bundle.getManifest();
				roManifest.getManifest().add(manifestXml);
				roManifest.setCreatedOn(Files.getLastModifiedTime(manifestXml));
								
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				DocumentBuilder db = factory.newDocumentBuilder();
				
				try (InputStream input = Files.newInputStream(manifestXml)) {				
					Document a = db.parse(input);
					NodeList contentNodes = a.getElementsByTagNameNS("http://identifiers.org/combine.specifications/omex-manifest", "content");
					for (int i=0; i<contentNodes.getLength(); i++) {
						Node item = contentNodes.item(i);
						String location = item.getAttributes().getNamedItem("location").getTextContent();
						String format = item.getAttributes().getNamedItem("format").getTextContent();
						URI loc = manifestXml.toUri().resolve(location);
						if (loc.equals(manifestXml.toUri()) || loc.equals(bundle.getRoot().toUri())) {
							// Avoid aggregating the RO or its manifest
							continue;
						}						
						//System.out.println(loc);
						if (! bundle.getRoot().toUri().relativize(loc).isAbsolute() &&
								! Files.exists(bundle.getFileSystem().provider().getPath(loc))) { 
							System.out.println("Missing: " + location);
							continue;
						}
						PathMetadata aggr = roManifest.getAggregation(loc);
						aggr.setMediatype(format);
					}
				}
				
				
				
			}						
		}
	}

}
 