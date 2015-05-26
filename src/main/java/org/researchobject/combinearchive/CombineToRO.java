package org.researchobject.combinearchive;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.xml.sax.SAXException;

public class CombineToRO {
	

	private static Logger logger = Logger.getLogger(CombineToRO.class.getCanonicalName());

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			Path file = Paths.get(arg);
			combineToRO(file);
		}
	}

	public static void combineToRO(Path file) throws IOException, SAXException, ParserConfigurationException {		
		try (Bundle bundle = Bundles.openBundle(file)) {
			bundle.getManifest().writeAsJsonLD();
			bundle.getManifest().writeAsCombineManifest();
		}
	}
	

}
 