/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Enterprise Data Management Council
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.edmcouncil.rdf_toolkit.runner;

import static org.edmcouncil.rdf_toolkit.runner.constant.CommandLineOption.SOURCE_DIRECTORY;
import static org.edmcouncil.rdf_toolkit.runner.constant.CommandLineOption.SOURCE_DIRECTORY_PATTERN;
import static org.edmcouncil.rdf_toolkit.runner.constant.CommandLineOption.TARGET_DIRECTORY;
import static org.edmcouncil.rdf_toolkit.runner.constant.CommandLineOption.TARGET_DIRECTORY_PATTERN;

import com.jcabi.manifests.Manifests;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
import org.edmcouncil.rdf_toolkit.RdfFormatter;
import org.edmcouncil.rdf_toolkit.io.DirectoryWalker;
import org.edmcouncil.rdf_toolkit.runner.constant.CommandLineOption;
import org.edmcouncil.rdf_toolkit.runner.exception.RdfToolkitOptionHandlingException;
import org.edmcouncil.rdf_toolkit.util.Constants;
import org.edmcouncil.rdf_toolkit.writer.SortedRdfWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfToolkitRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(RdfToolkitRunner.class);

  private final Options options;
  private final ValueFactory valueFactory;

  public RdfToolkitRunner() {
    this.options = CommandLineOption.prepareOptions();
    this.valueFactory = new MemValueFactory();
  }

  public void run(String[] args) throws Exception {
    var commandLineArgumentsHandler = new CommandLineArgumentsHandler();
    var rdfToolkitOptions = commandLineArgumentsHandler.handleArguments(args);

    switch (rdfToolkitOptions.getRunningMode()) {
      case PRINT_USAGE_AND_EXIT:
      case EXIT:
        // Usage was already printed, so we don't need to do it now
        return;
      case PRINT_AND_EXIT:
        System.out.println(rdfToolkitOptions.getOutput());
        break;
      case RUN_ON_DIRECTORY:
        // Run the serializer over a directory of files
        runOnDirectory(rdfToolkitOptions.getCommandLine());
        break;
      case RUN_ON_FILE:
        runOnFile(rdfToolkitOptions);
        break;
      default:
        throw new RdfToolkitOptionHandlingException(
            "Unknown running mode: " + rdfToolkitOptions.getRunningMode());
    }
  }

  private void runOnFile(RdfToolkitOptions rdfToolkitOptions) throws Exception {
    var sourceModel = readModel(rdfToolkitOptions);
    boolean isIriPatternAndIriReplacementNotNull = (rdfToolkitOptions.getIriPattern() != null)
        && (rdfToolkitOptions.getIriReplacement() != null);

    Model replaceModel = new TreeModel();
    Set<Namespace> sourceNamespaces = sourceModel.getNamespaces();
    for (Namespace sourceNamespace : sourceNamespaces) {
      replaceModel.setNamespace(sourceNamespace.getPrefix(), sourceNamespace.getName());
    }

    for (Statement st : sourceModel) {
      Value modelObject = st.getObject();
      Resource replacedSubject = st.getSubject();
      IRI replacedPredicate = st.getPredicate();
      //Replaced language serialization
      if (modelObject instanceof Literal) {
        Optional<String> lang = ((Literal) modelObject).getLanguage();
        if (lang.isPresent() && lang.get().contains("-")) {
          String langString = lang.get();
          String[] langTab = langString.split("-");
          langTab[1] = langTab[1].toUpperCase();
          langString = String.join("-", langTab);
          String label = ((Literal) modelObject).getLabel();
          modelObject = valueFactory.createLiteral(label, langString);
        }
      }
      // Do any URI replacements
      if (isIriPatternAndIriReplacementNotNull) {
        if (replacedSubject instanceof IRI) {
          replacedSubject = valueFactory.createIRI(
              replacedSubject.stringValue().replaceFirst(
                  rdfToolkitOptions.getIriPattern(),
                  rdfToolkitOptions.getIriReplacement()));
        }
        replacedPredicate = valueFactory.createIRI(
            replacedPredicate.stringValue().replaceFirst(
                rdfToolkitOptions.getIriPattern(),
                rdfToolkitOptions.getIriReplacement()));

        if (modelObject instanceof IRI) {
          modelObject = valueFactory.createIRI(
              modelObject.stringValue().replaceFirst(
                  rdfToolkitOptions.getIriPattern(),
                  rdfToolkitOptions.getIriReplacement()));
        }
      }
      Statement statement = valueFactory.createStatement(replacedSubject, replacedPredicate,
          modelObject);
      replaceModel.add(statement);
    }

    if (isIriPatternAndIriReplacementNotNull) {
      // Do IRI replacements in namespaces as well.
      Set<Namespace> namespaces = sourceModel.getNamespaces();
      for (Namespace namespace : namespaces) {
        replaceModel.setNamespace(
            namespace.getPrefix(),
            namespace.getName().replaceFirst(
                rdfToolkitOptions.getIriPattern(),
                rdfToolkitOptions.getIriReplacement()));
      }
    }
    sourceModel = replaceModel;
    if (isIriPatternAndIriReplacementNotNull) {
      // This is also the right time to do IRI replacement in the base URI, if appropriate
      if (rdfToolkitOptions.getBaseIri() != null) {
        String newBaseIriString = rdfToolkitOptions.getBaseIriString().replaceFirst(
            rdfToolkitOptions.getIriPattern(),
            rdfToolkitOptions.getIriReplacement());
        rdfToolkitOptions.setBaseIriString(newBaseIriString);
        rdfToolkitOptions.setBaseIri(valueFactory.createIRI(newBaseIriString));
      }
    }
    // Infer the base URI, if requested
    IRI inferredBaseIri = null;
    if (rdfToolkitOptions.getInferBaseIri()) {
      LinkedList<IRI> owlOntologyIris = new LinkedList<>();
      for (Statement st : sourceModel) {
        if ((Constants.RDF_TYPE.equals(st.getPredicate()))
            && (Constants.owlOntology.equals(st.getObject()))
            && (st.getSubject() instanceof IRI)) {
          owlOntologyIris.add((IRI) st.getSubject());
        }
      }
      if (!owlOntologyIris.isEmpty()) {
        Comparator<IRI> iriComparator = Comparator.comparing(IRI::toString);
        owlOntologyIris.sort(iriComparator);
        inferredBaseIri = owlOntologyIris.getFirst();
      }
    }
    if (rdfToolkitOptions.getInferBaseIri() && (inferredBaseIri != null)) {
      rdfToolkitOptions.setBaseIri(inferredBaseIri);
    }

    OutputStream outputStream = System.out;
    if (!rdfToolkitOptions.isShouldUseStandardOutputStream()) {
      outputStream = new FileOutputStream(rdfToolkitOptions.getTargetFile());
    }

    Writer targetWriter = new OutputStreamWriter(
        outputStream,
        StandardCharsets.UTF_8);
    SortedRdfWriterFactory factory = new SortedRdfWriterFactory(
        rdfToolkitOptions.getTargetFormat());
    RDFWriter rdfWriter = factory.getWriter(targetWriter, rdfToolkitOptions.getOptions());
    Rio.write(sourceModel, rdfWriter);
    targetWriter.flush();
    targetWriter.close();
  }

  private Model readModel(RdfToolkitOptions rdfToolkitOptions) {
    Model sourceModel = null;
    try {
      sourceModel = Rio.parse(
          rdfToolkitOptions.getSourceInputStream(),
          rdfToolkitOptions.getBaseIriString(),
          rdfToolkitOptions.getRdf4jSourceFormat());
    } catch (Exception t) {
      LOGGER.error("{}: stopped by unexpected exception:", RdfFormatter.class.getSimpleName());
      LOGGER.error("Unable to parse input file: {}",
          rdfToolkitOptions.getSourceFile().getAbsolutePath());
      LOGGER.error("Command line arguments: {}", Arrays.toString(rdfToolkitOptions.getArgs()));
      LOGGER.error("{}: {}", t.getClass().getSimpleName(), t.getMessage());
      StringWriter stackTraceWriter = new StringWriter();
      t.printStackTrace(new PrintWriter(stackTraceWriter));
      LOGGER.error(stackTraceWriter.toString());
      usage(options);
      System.exit(1);
    }
    return sourceModel;
  }

  private void runOnDirectory(CommandLine line) throws Exception {
    // Construct list of common arguments passed on to every invocation of 'run'
    ArrayList<String> commonArgsList = new ArrayList<>();
    List<String> noPassArgs = Arrays.asList(
        SOURCE_DIRECTORY.getShortOpt(),
        SOURCE_DIRECTORY_PATTERN.getShortOpt(),
        TARGET_DIRECTORY.getShortOpt(),
        TARGET_DIRECTORY_PATTERN.getShortOpt());

    for (Option option : line.getOptions()) {
      if (noPassArgs.contains(option.getOpt())) {
        continue;
      }
      commonArgsList.add(String.format("-%s", option.getOpt()));
      if (option.hasArg()) {
        commonArgsList.add(option.getValue());
      }
    }

    // Check the input & output directories
    var sourceDir = new File(line.getOptionValue(SOURCE_DIRECTORY.getShortOpt()));
    if (!sourceDir.exists()) {
      LOGGER.error("Source directory does not exist: {}", sourceDir.getAbsolutePath());
      return;
    }
    if (!sourceDir.canRead()) {
      LOGGER.error("Source directory is not readable: {}", sourceDir.getAbsolutePath());
      return;
    }
    var sourceDirPattern = Pattern.compile(line.getOptionValue("sdp"));

    final File targetDir = new File(line.getOptionValue("td"));
    if (!targetDir.exists()) {
      targetDir.mkdirs();
    }
    if (!targetDir.exists()) {
      LOGGER.error("Target directory could not be created: {}", targetDir.getAbsolutePath());
      return;
    }
    if (!targetDir.canWrite()) {
      LOGGER.error("Target directory is not writable: {}", targetDir.getAbsolutePath());
      return;
    }
    final String targetDirPatternString = line.getOptionValue("tdp");

    // Iterate through matching files.
    final DirectoryWalker dw = new DirectoryWalker(sourceDir, sourceDirPattern);
    final String[] stringArray = new String[]{};
    for (DirectoryWalker.DirectoryWalkerResult sourceResult : dw.pathMatches()) {
      // Construct output path.
      final Matcher sourceMatcher = sourceDirPattern.matcher(sourceResult.getRelativePath());
      final String targetRelativePath = sourceMatcher.replaceFirst(targetDirPatternString);
      final File targetFile = new File(targetDir, targetRelativePath);

      // Run serializer
      List<String> runArgs = new ArrayList<>();
      runArgs.addAll(commonArgsList);
      runArgs.add("-s");
      runArgs.add(sourceResult.getFile().getAbsolutePath());
      runArgs.add("-t");
      runArgs.add(targetFile.getAbsolutePath());
      LOGGER.info("... formatting '{}' to '{}' ...", sourceResult.getRelativePath(),
          targetRelativePath);

      run(runArgs.toArray(stringArray));
    }
  }

  private void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp(getVersion(), options);
  }

  private String getVersion() {
    String implementationTitle = Manifests.read("Implementation-Title");
    String implementationVersion = Manifests.read("Implementation-Version");
    return String.format(
        "%s (%s version %s)",
        RdfFormatter.class.getSimpleName(),
        implementationTitle,
        implementationVersion);
  }
}