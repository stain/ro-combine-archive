ro-combine-archive
==================
 

[![Build Status](https://travis-ci.org/stain/ro-combine-archive.svg)](https://travis-ci.org/stain/ro-combine-archive)
[![doi:10.5281/zenodo.10439](https://zenodo.org/badge/doi/10.5281/zenodo.10439.png)](http://dx.doi.org/10.5281/zenodo.10439)

Convert/enrich [Combine Archive (OMEX)](http://co.mbine.org/documents/archive) to 
[Research Object Bundle](https://w3id.org/bundle).

(c) University of Manchester 2014
http://www.mygrid.org.uk/

License: [MIT License](LICENSE.md)


This tool enrich/convert
[OMEX Combine Archives](http://co.mbine.org/documents/archive)
so that they are also valid RO Bundles. This is achieved
by parsing the OMEX manifest and creating the equivalent
RO Bundle manifest using the [RO Bundle API](https://github.com/wf4ever/robundle).

It is planned for this tool to also perform annotation extraction
and do the conversion from RO bundle to OMEX.



# Authors
* [Stian Soiland-Reyes](http://orcid.org/0000-0001-9842-9718) &lt;soiland-reyes@cs.manchester.ac.uk&gt;
* [Matthew Gamble](http://orcid.org/0000-0003-4913-1485) &lt;matthew.gamble@gmail.com&gt;

# Slides

[![Slides](http://image.slidesharecdn.com/yedlttqatdov0se6ku2d-140613102315-phpapp01/95/slide-1-638.jpg?cb=1402673174)(http://www.slideshare.net/soilandreyes/2014-0613research-objects-in-the-wild)

[Slides 2014-06-13](https://onedrive.live.com/view.aspx?cid=37935FEEE4DF1087&resid=37935FEEE4DF1087!788&app=PowerPoint%20f) 

# Mechanism

OMEX Combine Archives have similar mechanism of describing the bundled resources in a manifest.

An OMEX archive can also be a valid RO Bundle, and an RO Bundle can also be an
OMEX archive, simply by having both manifests included in the ZIP archive. 
(as long as you don't then modify the archive without updating both manifests!)

The conversion therefore parses the OMEX manifest and creates equivalent entries
in the RO Bundle manifest, copying over the `format` information to either 
`mediatype` or `conformsTo` (depending if it is an absolute URI).

When parsing the OMEX `manifest.xml`, `location` is interpreted as a URI
reference relative to the `manifest.xml`. Absolute URIs in `location` are
supported, recorded as an `uri` aggregation in the RO bundle.

The base URI for files in the archive
is generated using the [app:// URI scheme](http://www.w3.org/TR/app-uri/)
according to the [RO bundle specifications for absolute
URIs](http://wf4ever.github.io/ro/bundle/#absolute-uris). So
this means URIs in files are parsed with a base URI like:
`app://5226267e-75b1-48c4-b9da-ea587023adda/manifest.xml` - thus
also allowing for slash-based URI references like `/` and `/metadata.rdf`.

In OMEX, metadata is always given in `metadata.*` files. We'll parse
`metadata.rdf`, and look for any subjects that are aggregated
by the archive (or is the archive itself), and then add `metadata.rdf` to
`annotations` in the RO bundle manifest for each described file. 
(TODO: Support additional metadata files)

The metadata file is also examined for `dcterms:created`, `dcterms:modified`
and `dcterms:modified` annotations, which are propagated into the RO Bundle
manifest. The timestamp of affected files in the ZIP archive will also be set
to the specified modified time.

Note that the [RO Bundle `mimetype`](http://wf4ever.github.io/ro/bundle/#ucf)
file is *not* added, as an OMEX archive (and its
specialization) can be viewed as a more application-specific specialization of
RO Bundles, as allowed for in the [RO bundle container
specification](http://wf4ever.github.io/ro/bundle/#ro-bundle-container). OMEX archives
however do not currently have such a specific mimetype, just an extension varying
with the application type (e.g. .sbex for SBML archive).


# Building

Prerequisites:
 * [Java/OpenJDK 7](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html)
 * [Maven 3.x](http://maven.apache.org/download.cgi)

To build, simply do `mvn clean install`:

    stain@biggie:~/src/ro-combine-archive$ mvn clean install
    [INFO] Scanning for projects...
    [INFO]                                                                         
    [INFO] ------------------------------------------------------------------------
    [INFO] Building Combine Archive / RO interoperability 0.1.0-SNAPSHOT
    [INFO] ------------------------------------------------------------------------
    ...
    [INFO] --- maven-install-plugin:2.4:install (default-install) @ ro-combine-archive ---
    [INFO] Installing /home/stain/src/ro-combine-archive/target/ro-combine-archive-0.1.0-SNAPSHOT.jar to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT.jar
    [INFO] Installing /home/stain/src/ro-combine-archive/pom.xml to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT.pom
    [INFO] Installing /home/stain/src/ro-combine-archive/target/ro-combine-archive-0.1.0-SNAPSHOT-sources.jar to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT-sources.jar
    [INFO] Installing /home/stain/src/ro-combine-archive/target/ro-combine-archive-0.1.0-SNAPSHOT-test-sources.jar to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT-test-sources.jar
    [INFO] Installing /home/stain/src/ro-combine-archive/target/ro-combine-archive-0.1.0-SNAPSHOT-tests.jar to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT-tests.jar
    [INFO] Installing /home/stain/src/ro-combine-archive/target/ro-combine-archive-0.1.0-SNAPSHOT-standalone.jar to /home/stain/.m2/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0-SNAPSHOT/ro-combine-archive-0.1.0-SNAPSHOT-standalone.jar
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time: 45.570s
    [INFO] Finished at: Fri Apr 25 19:51:41 BST 2014
    [INFO] Final Memory: 41M/303M
    [INFO] ------------------------------------------------------------------------

# Downloading

Alternatively you can download the [ro-combine-archive 0.1.0
standalone](http://build.mygrid.org.uk/maven/repository/org/researchobject/ro-combine-archive/ro-combine-archive/0.1.0/ro-combine-archive-0.1.0-standalone.jar).

Note that this download might not reflect the latest features as when building from source.


# Executing

To execute from the self-contained JAR, try:

    java -jar target/ro-combine-archive-0.1.0-SNAPSHOT-standalone.jar [omex-file ...]

Note that the given OMEX Combine Archive will be updated *in-place* to also
include `.ro/manifest.json`, making it an [RO bundle](https://w3id.org/bundle).


## Example

This examples copies the example `Boris.omex` file to `target` (because we'll change it), then
executes the `ro-combine-archive` converter:

    stain@biggie:~/src/ro-combine-archive$ cp src/test/resources/Boris.omex target
    
    stain@biggie:~/src/ro-combine-archive$ java -jar target/ro-combine-archive-0.1.0-SNAPSHOT-standalone.jar target/Boris.omex 
    log4j:WARN No appenders could be found for logger (org.apache.jena.riot.stream.JenaIOEnvironment).
    log4j:WARN Please initialize the log4j system properly.
    log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
    Apr 25, 2014 7:56:26 PM org.purl.wf4ever.robundle.utils.RDFUtils literalAsFileTime
    INFO: Literal not an XSDDateTime, but: class java.lang.String 2013-05-28T17:50:43.999+01:00

Inspecting the modified archive will reveal the added `.ro/manifest.json`:

    stain@biggie:~/src/ro-combine-archive$ cd target/
    stain@biggie:~/src/ro-combine-archive/target$ unzip Boris.omex
    Archive:  Boris.omex
      inflating: BorisEJB.xml            
      inflating: manifest.xml            
      inflating: metadata.rdf            
       creating: paper/
      inflating: paper/Kholodenko2000.pdf  
       creating: .ro/
      inflating: .ro/manifest.json       

Inspecting `.ro/manifest.json`:
      
```json      
    {
      "@context" : [ "https://w3id.org/bundle/context" ],
      "id" : "/",
      "manifest" : [ "/.ro/manifest.json", "/manifest.xml" ],
      "createdOn" : "2013-05-28T16:50:43.999Z",
      "aggregates" : [ {
        "file" : "/paper/Kholodenko2000.pdf",
        "folder" : "/paper/",
        "mediatype" : "application/pdf",
        "createdOn" : "2013-04-05T08:16:08Z",
        "proxy" : "urn:uuid:ca91dfa3-729b-4494-a059-b73b9b3c4261"
      }, {
        "file" : "/metadata.rdf",
        "folder" : "/",
        "createdOn" : "2014-04-25T18:17:00Z",
        "conformsTo" : "http://identifiers.org/combine.specifications/omex-metadata",
        "proxy" : "urn:uuid:850812ed-88d5-4999-9675-8257355f6e3e"
      }, {
        "file" : "/BorisEJB.xml",
        "folder" : "/",
        "createdOn" : "2012-10-29T10:58:38Z",
        "conformsTo" : "http://identifiers.org/combine.specifications/sbml",
        "proxy" : "urn:uuid:054c9840-0283-4a3c-8319-b0e24b6593b4"
      }, {
        "uri" : "http://www.ebi.ac.uk/biomodels-main/BIOMD0000000010",
        "conformsTo" : "http://identifiers.org/combine.specifications/sbml"
      } ],
      "annotations" : [ {
        "about" : "/",
        "content" : "/metadata.rdf"
      } ]
    }
```
    
