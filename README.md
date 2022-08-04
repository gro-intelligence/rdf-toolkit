<img src="https://spec.edmcouncil.org/fibo/htmlpages/develop/latest/img/logo.66a988fe.png" width="150" align="right"/>

# Introduction

The `RDF Toolkit` is a 'swiss army knife' tool for reading and writing RDF files in multiple formats.

The primary reason for creating this tool was to have a reference serializer for the FIBO ontologies as they are stored in [Github FIBO repository](https://github.com/edmcouncil/fibo). However, the tool is not in any way specific to FIBO and it can be used with any ontology or RDF file. In this capacity it can be used in a commit-hook to make sure that all RDF files in the repo are stored in the same way.

# Requirements

## Minimize the ontology review effort 

For the purposes of git-based version control of ontology files we want to have as few differences between commits as possible.
Most ontology editors can encode RDF graphs, including OWL ontologies, in several formats, including the W3C normative RDF exchange formats (syntaxes): RDF/XML and Turtle. However, even these normative formats are not canonical. Therefore, an editor tool may change many aspects of how ontology entities are serialized every time the ontology is saved (such as adding/changing comments or changing the order and organization of statements) leading to difficulties in analyzing actual changes in the underlying semantics.

## Handle intelligent IRIs

We want to be albe to include actionable information as part of IRIs, e.g., git tags, and then deference them from ontology tools like Protege.

## Recommended output format

The recommended output format at this time is RDF/XML because that is the format that the OMG requires for submissions. 
The EDM Council develops the FIBO Ontologies and submits them as RDF/XML, serialized by the `rdf-toolkit` to the OMG. 
So that is why we also use RDF/XML in Github itself. 

# Usage

## Download

Download the RDF Toolkit binary from [here](https://jenkins.edmcouncil.org/view/rdf-toolkit/job/rdf-toolkit-build/lastSuccessfulBuild/artifact/target/rdf-toolkit.jar).

## Run

One can use RDF Toolkit as a standalong application, which can be run from the command line or as a part of the git commit mechanism.

### Standalone RDF Toolkit Application

#### Operating Systems

##### Linux or Mac OS X
On Linux or Mac OS X you can execute the rdf-toolkit as follows:

1. Open a Terminal
2. Type the name of the `rdf-toolkit.jar` file on the command prompt and supply the `--help` option:
```
$ java -jar rdf-toolkit.jar --help
```

##### Windows

1. Open a Command Shell by going to the Start menu and type `cmd.exe`.
2. Ensure that Java is installed by typing `java -version` on the command line. The Java version should be at least 1.7 (i.e. Java 7).
3. Then launch the rdf-toolkit's help function as follows:
```
java -jar rdf-toolkit.jar --help
```

#### Options

```
usage: RdfFormatter (rdf-toolkit version 1.11.0)
 -bi,--base-iri <arg>                    set IRI to use as base URI
 -dtd,--use-dtd-subset                   for XML, use a DTD subset in order to allow prefix-based
                                         IRI shortening
 -h,--help                               print out details of the command-line arguments for the
                                         program
 -i,--indent <arg>                       sets the indent string.  Default is a single tab character
 -ibi,--infer-base-iri                   use the OWL ontology IRI as the base URI.  Ignored if an
                                         explicit base IRI has been set
 -ibn,--inline-blank-nodes               use inline representation for blank nodes.  NOTE: this will
                                         fail if there are any recursive relationships involving
                                         blank nodes.  Usually OWL has no such recursion involving
                                         blank nodes.  It also will fail if any blank nodes are a
                                         triple subject but not a triple object.
 -ip,--iri-pattern <arg>                 set a pattern to replace in all IRIs (used together with
                                         --iri-replacement)
 -ir,--iri-replacement <arg>             set replacement text used to replace a matching pattern in
                                         all IRIs (used together with --iri-pattern)
 -lc,--leading-comment <arg>             sets the text of the leading comment in the ontology.  Can
                                         be repeated for a multi-line comment
 -ln,--line-end <arg>                    sets the end-line character(s); supported characters: \n
                                         (LF), \r (CR). Default is the LF character
 -osl,--override-string-language <arg>   sets an override language that is applied to all strings
 -s,--source <arg>                       source (input) RDF file to format
 -sd,--source-directory <arg>            source (input) directory of RDF files to format.  This is a
                                         directory processing option
 -sdp,--source-directory-pattern <arg>   relative file path pattern (regular expression) used to
                                         select files to format in the source directory.  This is a
                                         directory processing option
 -sdt,--string-data-typing <arg>         sets whether string data values have explicit data types,
                                         or not; one of: explicit, implicit [default]
 -sfmt,--source-format <arg>             source (input) RDF format; one of: auto (select by
                                         filename) [default], binary, json-ld (JSON-LD), n3, n-quads
                                         (N-quads), n-triples (N-triples), rdf-a (RDF/A), rdf-json
                                         (RDF/JSON), rdf-xml (RDF/XML), trig (TriG), trix (TriX),
                                         turtle (Turtle)
 -sip,--short-iri-priority <arg>         set what takes priority when shortening IRIs: prefix
                                         [default], base-iri
 -t,--target <arg>                       target (output) RDF file
 -tc,--trailing-comment <arg>            sets the text of the trailing comment in the ontology.  Can
                                         be repeated for a multi-line comment
 -td,--target-directory <arg>            target (output) directory for formatted RDF files.  This is
                                         a directory processing option
 -tdp,--target-directory-pattern <arg>   relative file path pattern (regular expression) used to
                                         construct file paths within the target directory.  This is
                                         a directory processing option
 -tfmt,--target-format <arg>             target (output) RDF format: one of: json-ld (JSON-LD),
                                         rdf-xml (RDF/XML), turtle (Turtle) [default]
 -v,--version                            print out version details
```

### RDF Toolkit For Git

TBA
(For now, see:https://github.com/edmcouncil/fibo/blob/master/CONTRIBUTING.md#fibo-serialization-tools)

# Serialisation Algorithm

TBA
