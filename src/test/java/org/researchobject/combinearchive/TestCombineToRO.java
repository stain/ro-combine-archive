package org.researchobject.combinearchive;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Ignore;
import org.junit.Test;

public class TestCombineToRO {

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
