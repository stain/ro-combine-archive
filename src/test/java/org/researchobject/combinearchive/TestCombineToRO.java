package org.researchobject.combinearchive;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.purl.wf4ever.robundle.Bundle;
import org.purl.wf4ever.robundle.Bundles;
import org.purl.wf4ever.robundle.manifest.Agent;
import org.purl.wf4ever.robundle.manifest.Manifest;
import org.purl.wf4ever.robundle.manifest.PathMetadata;

public class TestCombineToRO {

	@BeforeClass
	public static void setLogging() {
		Logger logger = Logger.getLogger("");
		//logger.setLevel(Level.FINER);
		ConsoleHandler console = new ConsoleHandler();
		console.setLevel(Level.FINEST);
		logger.addHandler(console);
		Logger.getLogger("org.researchobject").setLevel(Level.FINEST);
	}
	
	
	@Test
	public void convertAslanidi() throws Exception {
		Path file = Files.createTempFile("aslanidi", ".zip");
		try (InputStream src = getClass().getResourceAsStream("/aslanidi_purkinje_model_2009.zip")) {
			Files.copy(src, file, StandardCopyOption.REPLACE_EXISTING);
		}
		CombineToRO.combineToRO(file);
		System.out.println(file);
		try (Bundle bundle = Bundles.openBundle(file)) {
			Manifest manifest = manifest = bundle.getManifest();
			Path manifestXml = bundle.getRoot().resolve("manifest.xml");
			//assertTrue("manifest.xml not listed in " + manifest.getManifest(), 
			//		manifest.getManifest().contains(manifestXml));
			
//			Agent createdBy = manifest.getCreatedBy().get(0);
//			assertEquals("Gary Mirams", createdBy.getName());
//			assertEquals("mbox:gary.mirams@cs.ox.ac.uk", createdBy.getUri());
			
			Path csvPath = bundle.getRoot().resolve("outputs_degree_of_block.csv");			
			PathMetadata csv = manifest.getAggregation(csvPath);
			assertEquals("text/csv", csv.getMediatype());
			Agent csvCreator = csv.getCreatedBy().get(0);
			assertEquals("Gary Mirams", csvCreator.getName());
			assertEquals("mbox:gary.mirams@cs.ox.ac.uk", csvCreator.getUri().toString());
			assertEquals("2014-02-06T22:01:58Z", csv.getCreatedOn().toString());
			
			
		}
	}
	
	@Test
	public void convertBoris() throws Exception {
		Path file = Files.createTempFile("Boris", ".omex");
		try (InputStream src = getClass().getResourceAsStream("/Boris.omex")) {
			Files.copy(src, file, StandardCopyOption.REPLACE_EXISTING);
		}
		CombineToRO.combineToRO(file);
		System.out.println(file);		
	}
	
	@Test
	public void convertDirectoryMadness() throws Exception {
		Path file = Files.createTempFile("DirectoryMadness", ".omex");
		try (InputStream src = getClass().getResourceAsStream("/DirectoryMadness.omex")) {
			Files.copy(src, file, StandardCopyOption.REPLACE_EXISTING);
		}
		CombineToRO.combineToRO(file);
		System.out.println(file);		
	}
	
	@Test
	public void convertDirectoryMadnessZipped() throws Exception {
		Path file = Files.createTempFile("DirectoryMadnessZipped", ".omex");
		try (InputStream src = getClass().getResourceAsStream("/DirectoryMadnessZipped.omex")) {
			Files.copy(src, file, StandardCopyOption.REPLACE_EXISTING);
		}
		CombineToRO.combineToRO(file);
		System.out.println(file);		
	}
}
