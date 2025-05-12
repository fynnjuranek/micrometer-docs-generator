/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.docs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import io.micrometer.common.util.internal.logging.InternalLogger;
import io.micrometer.common.util.internal.logging.InternalLoggerFactory;
import io.micrometer.docs.conventions.ObservationConventionsDocGenerator;
import io.micrometer.docs.metrics.MetricsDocGenerator;
import io.micrometer.docs.spans.SpansDocGenerator;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Entry point for document generation.
 *
 * @author Tadaya Tsuyukubo
 */
@Command(mixinStandardHelpOptions = true, description = "Generate documentation from source files")
public class DocsGeneratorCommand implements Runnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DocsGeneratorCommand.class);

    @ArgGroup(exclusive = false)
    private final Options options = new Options();

    @Parameters(index = "0", description = "The project root directory.")
    private File projectRoot;

    @Parameters(index = "1", description = "The regex pattern for inclusion.")
    private Pattern inclusionPattern;

    @Parameters(index = "2", description = "The output directory.")
    private Path outputDir;

    @Option(names = "--metrics-template", defaultValue = "templates/asciidoc/metrics.adoc.hbs",
            description = "Metrics template location")
    private String metricsTemplate;

    @Option(names = "--spans-template", defaultValue = "templates/asciidoc/spans.adoc.hbs",
            description = "Spans template location")
    private String spansTemplate;

    @Option(names = "--conventions-template", defaultValue = "templates/asciidoc/conventions.adoc.hbs",
            description = "Observation Conventions template location")
    private String conventionsTemplate;

    @Option(names = "--metrics-output", defaultValue = "_metrics.adoc",
            description = "Generated metrics filename. Absolute path or relative path to the output directory.")
    private Path metricsOutput;

    @Option(names = "--spans-output", defaultValue = "_spans.adoc",
            description = "Generated metrics filename. Absolute path or relative path to the output directory.")
    private Path spansOutput;

    @Option(names = "--conventions-output", defaultValue = "_conventions.adoc",
            description = "Generated observation conventions filename. Absolute path or relative path to the output directory.")
    private Path conventionsOutput;

    @Option(names = "--format", defaultValue = "adoc",
            description = "Template format. Choose Markdown (md) or Asciidoc (adoc)")
    private String templateFormat;

    public static void main(String... args) {
        DocsGeneratorCommand command = new DocsGeneratorCommand();
        // Do not call "System.exit" here since exec-maven-plugin's "exec:java" halts the
        // maven run. Then, to properly bubble up the exception, uses "parseArgs" instead
        // of "CommandLine.execute()". This is because "execute" captures an exception
        // thrown while executing the business logic, then convert it to an exit code.
        // This squashes the thrown exception. To avoid it, we need to DIY the command
        // execution.
        // See https://picocli.info/#_diy_command_execution
        execute(command, args);
    }

    private static void execute(Runnable runnable, String[] args) {
        CommandLine cmd = new CommandLine(runnable);
        try {
            ParseResult parseResult = cmd.parseArgs(args);
            // Did user request usage help (--help)?
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(cmd.getOut());
                return;
            }
            // Did user request version help (--version)?
            else if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(cmd.getOut());
                return;
            }
            // invoke the business logic
            runnable.run();
        }
        // invalid user input: print error message and usage help
        catch (ParameterException ex) {
            cmd.getErr().println(ex.getMessage());
            if (!UnmatchedArgumentException.printSuggestions(ex, cmd.getErr())) {
                ex.getCommandLine().usage(cmd.getErr());
            }
            throw ex;
        }
        // exception occurred in business logic
        catch (Exception ex) {
            ex.printStackTrace(cmd.getErr());
            throw ex;
        }
    }

    @Override
    public void run() {
        this.inclusionPattern = Pattern.compile(this.inclusionPattern.pattern().replace("/", File.separator));
        logger.info("Project root: {}", this.projectRoot);
        logger.info("Inclusion pattern: {}", this.inclusionPattern);
        logger.info("Output root: {}", this.outputDir);
        logger.info("Output format: {}", this.templateFormat);
        // when format is specified to markdown update templates and output files
        if (templateFormat.equals("md")) {
            updateFormat();
        }
        if (this.options.metrics) {
            generateMetricsDoc();
        }
        if (this.options.spans) {
            generateSpansDoc();
        }
        if (this.options.conventions) {
            generateConventionsDoc();
        }
    }

    private void updateFormat() {
        // update templates from adoc to markdown
        // replace for default templates
        this.metricsTemplate = this.metricsTemplate.replace("asciidoc", "markdown").replace(".adoc", ".md");
        this.spansTemplate = this.spansTemplate.replace("asciidoc", "markdown").replace(".adoc", ".md");
        this.conventionsTemplate = this.conventionsTemplate.replace("asciidoc", "markdown").replace(".adoc", ".md");

        this.spansOutput = Paths.get(this.spansOutput.toString().replace(".adoc", ".md"));
        this.metricsOutput = Paths.get(this.metricsOutput.toString().replace(".adoc", ".md"));
        this.conventionsOutput = Paths.get(this.conventionsOutput.toString().replace(".adoc", ".md"));
    }

    void generateMetricsDoc() {
        Path output = resolveAndPrepareOutputPath(this.metricsOutput);
        new MetricsDocGenerator(this.projectRoot, this.inclusionPattern, this.metricsTemplate, output).generate();
    }

    void generateSpansDoc() {
        Path output = resolveAndPrepareOutputPath(this.spansOutput);
        new SpansDocGenerator(this.projectRoot, this.inclusionPattern, this.spansTemplate, output).generate();
    }

    void generateConventionsDoc() {
        Path output = resolveAndPrepareOutputPath(this.conventionsOutput);
        new ObservationConventionsDocGenerator(this.projectRoot, this.inclusionPattern, this.conventionsTemplate,
                output)
            .generate();
    }

    private Path resolveAndPrepareOutputPath(Path specified) {
        Path resolved = resolveOutputPath(specified);
        if (resolved.toFile().isDirectory()) {
            throw new IllegalArgumentException(resolved + " is not a file");
        }

        try {
            Files.createDirectories(resolved.getParent());
        }
        catch (IOException ex) {
            throw new RuntimeException("Failed to prepare output directory for " + resolved, ex);
        }

        return resolved;
    }

    private Path resolveOutputPath(Path specified) {
        if (specified.isAbsolute()) {
            return specified;
        }
        return this.outputDir.resolve(specified);
    }

    static class Options {

        @Option(names = "--metrics", description = "Generate metrics documentation", defaultValue = "true")
        private boolean metrics;

        @Option(names = "--spans", description = "Generate spans documentation", defaultValue = "true")
        private boolean spans;

        @Option(names = "--conventions", description = "Generate conventions documentation", defaultValue = "true")
        private boolean conventions;

    }

}
