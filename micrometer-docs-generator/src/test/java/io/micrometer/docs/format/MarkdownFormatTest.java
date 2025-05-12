package io.micrometer.docs.format;

import io.micrometer.docs.conventions.ObservationConventionsDocGenerator;
import io.micrometer.docs.metrics.MetricsDocGenerator;
import io.micrometer.docs.spans.SpansDocGenerator;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class MarkdownFormatTest {

    @Test
    void should_render_metric_doc_in_markdown() throws IOException {
        File root = new File("./src/test/java/io/micrometer/docs/format");
        Path output = Paths.get(".", "build/format/_metrics.md");
        Files.createDirectories(output.getParent());

        new MetricsDocGenerator(root, Pattern.compile(".*"),
            "templates/markdown/metrics.md.hbs", output)
            .generate();
        BDDAssertions.then(new String(Files.readAllBytes(output)))
            .contains("simple counter");
    }

    @Test
    void should_render_span_doc_in_markdown() throws IOException {
        File root = new File("./src/test/java/io/micrometer/docs/format");
        Path output = Paths.get(".", "build/format/_spans.md");
        Files.createDirectories(output.getParent());

        new SpansDocGenerator(root, Pattern.compile(".*"), "templates/markdown/spans.md.hbs", output).generate();
        BDDAssertions.then(new String(Files.readAllBytes(output)))
            .contains("simple span");
    }

    @Test
    void should_render_observation_convention_doc_in_markdown() throws IOException {
        File root = new File("./src/test/java/io/micrometer/docs/format");
        Path output = Paths.get(".", "build/format/_conventions.md");
        Files.createDirectories(output.getParent());

        new ObservationConventionsDocGenerator(root, Pattern.compile(".*"), "templates/markdown/conventions.md.hbs",
            output)
            .generate();

        BDDAssertions.then(new String(Files.readAllBytes(output)))
            .contains("#### GlobalObservationConvention implementations\n" +
                "\n" +
                "| GlobalObservationConvention Class Name | Applicable ObservationContext Class Name |\n" +
                "| ------------------------------------- | ---------------------------------------- |\n" +
                "| `io.micrometer.docs.format.SimpleGlobalConvention` | `SimpleContext` |")
            .contains("#### ObservationConvention implementations\n" +
                "\n" +
                "| ObservationConvention Class Name | Applicable ObservationContext Class Name |\n" +
                "| ------------------------------- | ---------------------------------------- |\n" +
                "| `io.micrometer.docs.format.SimpleConvention` | `SimpleContext` |");
    }

}
