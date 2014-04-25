package org.researchobject.combinearchive;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

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
