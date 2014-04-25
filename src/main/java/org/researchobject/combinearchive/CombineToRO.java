package org.researchobject.combinearchive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.purl.wf4ever.robundle.Bundle;
import org.purl.wf4ever.robundle.Bundles;
import org.purl.wf4ever.robundle.manifest.Agent;
import org.purl.wf4ever.robundle.manifest.Manifest;
import org.purl.wf4ever.robundle.manifest.PathAnnotation;
import org.purl.wf4ever.robundle.manifest.PathMetadata;
import org.purl.wf4ever.robundle.utils.RDFUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class CombineToRO {
	
	private static final String sparqlPrefixes = 			
			"PREFIX foaf:  <http://xmlns.com/foaf/0.1/> \n"+ 
			"PREFIX vcard: <http://www.w3.org/2006/vcard/ns#> \n"+ 
			"PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#> \n" +
			"PREFIX dct:   <http://purl.org/dc/terms/> \n";

	private static Logger logger = Logger.getLogger(CombineToRO.class.getCanonicalName());

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			Path file = Paths.get(arg);
			combineToRO(file);
		}
	}

	public static void combineToRO(Path file) throws IOException, SAXException, ParserConfigurationException {		
		try (Bundle bundle = Bundles.openBundle(file)) {
			parseManifestXML(bundle);
			findAnnotations(bundle);
		}
	}

	private static void findAnnotations(Bundle bundle) throws IOException {
		Path metadataRdf = bundle.getRoot().resolve("metadata.rdf");
		if (! Files.exists(metadataRdf)) { 
			return;
		}

		Model metadata;
		try {
			metadata = parseRDF(metadataRdf);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Can't read " + metadataRdf, e);
			return;
		} catch (RiotException e) {
			logger.log(Level.WARNING, "Can't parse " + metadataRdf, e);
			return;
		}

		Manifest manifest = bundle.getManifest();
		for (URI about : bundleSubjects(bundle)) {
			Resource resource = metadata.getResource(about.toString());
			if (! metadata.containsResource(resource)) {
				continue;
			}

			PathAnnotation ann = new PathAnnotation();
			ann.setAbout(manifest.relativeToBundleRoot(about));
			ann.setContent(manifest.relativeToBundleRoot(metadataRdf.toUri()));
			manifest.getAnnotations().add(ann);
			
			// Extract information that could be in our manifest
			PathMetadata pathMetadata = manifest.getAggregation(about);
			
			// Created date. We'll prefer dcModified.
			Property dcCreated = metadata.getProperty("http://purl.org/dc/terms/created");
			Property dcModified = metadata.getProperty("http://purl.org/dc/terms/modified");
			Statement createdSt = resource.getProperty(dcModified);
			if (createdSt == null) {
				createdSt = resource.getProperty(dcCreated);
			}
			if (createdSt != null) {
				FileTime fileTime = RDFUtils.literalAsFileTime(createdSt.getObject());
				if (fileTime == null && createdSt.getResource().isResource()) { 
					// perhaps one of those strange mixups of XML and RDF...
					Property dcW3CDTF = metadata.getProperty("http://purl.org/dc/terms/W3CDTF");					
					Statement w3cSt = createdSt.getResource().getProperty(dcW3CDTF);
					if (w3cSt != null) {
						fileTime = RDFUtils.literalAsFileTime(w3cSt.getObject());
					}

				}
				if (fileTime != null) { 
					pathMetadata.setCreatedOn(fileTime);
					if (pathMetadata.getFile() != null) {
						Files.setLastModifiedTime(pathMetadata.getFile(), fileTime);
					}
				}
			}
			
			for (RDFNode s : creatingAgentsFor(resource)) {
				if (s.isLiteral()) {
					pathMetadata.getCreatedBy().add(new Agent(s.asLiteral().getLexicalForm()));
				} else {
					Resource agentResource = s.asResource();
					Agent agent = new Agent();
					if (agentResource.isURIResource()) {
						URI agentUri = URI.create(agentResource.getURI());
						if (agentResource.getURI().startsWith("http://orcid.org/")) {
							agent.setOrcid(agentUri);
						} else {
							agent.setUri(agentUri);
						}
					} else { 
						Resource mbox = mboxForAgent(agentResource);
						if (mbox != null && mbox.isURIResource()) {
							agent.setUri(URI.create(mbox.getURI()));
						}
					}
					agent.setName(nameForAgent(agentResource));
					pathMetadata.getCreatedBy().add(agent);
				}
			}
			if (pathMetadata.getFile().equals(bundle.getRoot()) || 
					pathMetadata.getFile().equals(metadataRdf)) { 
				// Statements where about the RO itself
				manifest.setCreatedOn(pathMetadata.getCreatedOn());
				manifest.setCreatedBy(pathMetadata.getCreatedBy());
			}
			
			
			
		}
	}

	private static List<RDFNode> creatingAgentsFor(Resource r) {
		logger.fine("Finding creator of "  + r);
		String queryStr = sparqlPrefixes + 
				"SELECT ?agent WHERE { \n"
				+ " { \n"
				+ "  ?r dct:creator [ \n"
				+ "	    rdfs:member ?agent \n"
				+ "  ] \n"
				+ " } UNION { \n"
				+ "   ?r dct:creator ?agent .\n "
				+ "   FILTER NOT EXISTS { ?agent rdfs:member ?member } \n"
				+ " } \n"
				+ "} \n";
		logger.finer(QueryFactory.create(queryStr).toString());
		QueryExecution qexec = QueryExecutionFactory.create(queryStr, r.getModel());
		QuerySolutionMap binding = new QuerySolutionMap();
		binding.add("r", r);
		qexec.setInitialBinding(binding);
		ResultSet select = qexec.execSelect();
		List<RDFNode> agents = new ArrayList<>();
		
		while (select.hasNext()) {
			RDFNode agent = select.next().get("agent");
			logger.fine("Found: " + agent);
			agents.add(agent);
		}
		return agents;
	}
	
	private static String nameForAgent(Resource agentResource) {
		logger.fine("Finding name of "  + agentResource);
		String queryStr = sparqlPrefixes + 
			"SELECT ?name WHERE { \n"+ 
			"		{ ?agent foaf:name ?name } \n"+ 
			"	UNION  \n"+ 
			"		{ ?agent vcard:fn ?name } \n"+  
			"	UNION  \n"+ 
			"		{ ?agent vcard:FN ?name } \n"+ // legacy  
			"	UNION  \n"+ 
			"		{ ?agent rdfs:label ?name } \n"
			+ " UNION  \n"
			+ "     { \n"
			+ "         { ?agent vcard:n ?n } UNION { ?agent vcard:hasName ?n } \n"
			+ "         ?n vcard:family-name ?family ; \n"
			+ "            vcard:given-name ?given . \n"
			+ "          BIND(CONCAT(?given, \" \", ?family) AS ?name) \n"
			+ "     } \n"
			+ " UNION \n"
			+ "     { "
			+ "         ?agent foaf:givenName ?given ; \n"
			+ "                foaf:familyName ?family \n"
			+ "          BIND(CONCAT(?given, \" \", ?family) AS ?name) \n"
			+ "     } \n"  
			+ " UNION \n"
			+ "     { "
			+ "         ?agent foaf:firstName ?given ; \n"
			+ "                foaf:surname ?family \n"
			+ "          BIND(CONCAT(?given, \" \", ?family) AS ?name) \n"
			+ "     } \n"+  
			"	}  \n";
		logger.finer(QueryFactory.create(queryStr).toString());
		QueryExecution qexec = QueryExecutionFactory.create(queryStr, agentResource.getModel());
		QuerySolutionMap binding = new QuerySolutionMap();
		binding.add("agent", agentResource);
		qexec.setInitialBinding(binding);
		ResultSet select = qexec.execSelect();
		if (select.hasNext()) {
			String name = select.next().getLiteral("name").getString();			
			logger.fine(name);
			return name;
		}
		logger.fine("(null)");
		return null; 
	}

	private static Resource mboxForAgent(Resource agentResource) {
		logger.fine("Finding mbox of "  + agentResource);
		String queryStr = sparqlPrefixes + "SELECT ?mbox WHERE { \n"
				+ "		{ ?agent foaf:mbox ?mbox } \n" + "	UNION  \n"
				+ "		{ ?agent vcard:hasEmail ?mbox } \n" + "	UNION  \n"
				+ "		{ ?agent vcard:email ?email .  \n"
				+ "       BIND(IRI(CONCAT(\"mbox:\", ?email)) AS ?mbox) \n" // legacy
				+ "	    } \n"
				+ "} \n";
		logger.finer(QueryFactory.create(queryStr).toString());
		QueryExecution qexec = QueryExecutionFactory.create(queryStr, agentResource.getModel());
		QuerySolutionMap binding = new QuerySolutionMap();
		binding.add("agent", agentResource);
		qexec.setInitialBinding(binding);
		ResultSet select = qexec.execSelect();
		if (select.hasNext()) {
			Resource mbox = select.next().getResource("mbox");		
			logger.fine("Found mbox: " + mbox);
			return mbox;
		}
		logger.fine("mbox not found");
		return null; 
	}
	

	
	private static Collection<URI> bundleSubjects(Bundle bundle) throws IOException {
		Set<URI> subjects = new HashSet<>();
		subjects.add(bundle.getRoot().toUri());
		for (PathMetadata pathMetadata : bundle.getManifest().getAggregates()) {
			subjects.add(pathMetadata.getUri());
			if (pathMetadata.getFile() != null) { 
				subjects.add(pathMetadata.getFile().toUri());
			}
			if (pathMetadata.getFolder() != null) { 
				subjects.add(pathMetadata.getFolder().toUri());
			}			
//			subjects.add(pathMetadata.getProxy());
		}
		for (PathAnnotation a : bundle.getManifest().getAnnotations()) {
			subjects.add(a.getAnnotation());
		}
		subjects.remove(null);
		return subjects;
	}

	private static Model parseRDF(Path metadata) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		try (InputStream in = Files.newInputStream(metadata)) { 
			RDFDataMgr.read(model, in, metadata.toUri().toASCIIString(), RDFLanguages.RDFXML);
		}
		return model;
	}

	private static void parseManifestXML(Bundle bundle) throws IOException,
			ParserConfigurationException, SAXException {
		Path manifestXml = bundle.getRoot().resolve("manifest.xml");
		if (! Files.exists(manifestXml)) {
			return;
		}
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
				URI loc = manifestXml.toUri().resolve(location);
				if (loc.equals(manifestXml.toUri()) || loc.equals(bundle.getRoot().toUri())) {
					// Avoid aggregating the RO or its manifest
					continue;
				}
				logger.finest(loc.toString());
				if (! bundle.getRoot().toUri().relativize(loc).isAbsolute() &&
						! Files.exists(bundle.getFileSystem().provider().getPath(loc))) { 
					logger.warning("File missing from archive: " + location);
					continue;
				}
				PathMetadata aggr = roManifest.getAggregation(loc);
				
				
				String format = item.getAttributes().getNamedItem("format").getTextContent();
				URI formatUri;
				try { 
					formatUri = URI.create(format);
					if (formatUri.isAbsolute()) {
						aggr.setConformsTo(formatUri);
					} else {
						// "Relative URI" is not really - it's just at media type
						aggr.setMediatype(format);
					}				
				} catch (IllegalArgumentException ex) {
					aggr.setMediatype(format);
				}
			}
		}
	}

}
 