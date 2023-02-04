package ru.yandex.solomon.expression;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.simple.SimpleLogger;

import ru.yandex.misc.io.InputStreamSourceUtils2;
import ru.yandex.solomon.expression.analytics.PreparedProgram;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.compile.DeprOpts;
import ru.yandex.solomon.expression.compile.SelStatement;
import ru.yandex.solomon.expression.version.SelVersion;
import ru.yandex.solomon.util.time.Interval;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class CompilerCanonSupport {
    static class AlertProgram {
        public String project;
        public String id;
        public String program;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class CompiledProgram {
        public Map<String, String> loadRequests;
        public Map<String, String> predefinedVars;
        public List<String> code;
        public String interval;
        public String exceptionClass;
        public String exceptionMessage;
    }

    static final String ALERT_PROGRAMS = "alert_programs.json";
    static final String COMPILED_PROGRAMS = "compiled_programs.json";

    private static final Interval PREPARE_INTERVAL = Interval.after(Instant.parse("2019-11-01T00:00:00Z"), Duration.ofDays(7));

    /**
     * @param path Either local path as /foo/bar/baz or classpath:foo/bar/baz
     */
    static List<AlertProgram> readAlertPrograms(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // If you get resource not found exception
        // 1. ya make -t canon-data
        // 2. Regenerate project
        //
        // https://st.yandex-team.ru/DEVTOOLS-5511
        // https://ml.yandex-team.ru/thread/devtools/169166461003175860/

        InputStream is = InputStreamSourceUtils2.valueOf(path).getInput();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        List<AlertProgram> result = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            AlertProgram alertProgram = mapper.readValue(line, AlertProgram.class);
            result.add(alertProgram);
        }
        is.close();
        return result;
    }

    static List<CompiledProgram> readCanonicalResults(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        InputStream is = InputStreamSourceUtils2.valueOf(path).getInput();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        List<CompiledProgram> result = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            result.add(mapper.readValue(line, CompiledProgram.class));
        }
        is.close();
        return result;
    }

    private static void writeJSON(List<?> objects, OutputStream outputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        PrintStream ps = new PrintStream(outputStream);
        for (Object object : objects) {
            ps.println(mapper.writeValueAsString(object));
        }
    }

    static CompiledProgram compileSource(SelVersion version, String source) {
        CompiledProgram result = new CompiledProgram();
        try {
            PreparedProgram pp = Program.fromSource(version, source)
                .withDeprOpts(DeprOpts.ALERTING)
                .compile()
                .prepare(PREPARE_INTERVAL);
            result.code = pp.getCode().stream().map(SelStatement::format).collect(Collectors.toList());
            result.interval = pp.getInterval().toString();
            result.loadRequests = pp.getAllLoadRequests().entrySet().stream()
                    .map(e -> Map.entry(e.getKey().getName() + ": " + e.getKey().type(), e.getValue().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            result.predefinedVars = pp.getPredefinedVariables().entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue().format()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Throwable t) {
            result.exceptionClass = t.getClass().getSimpleName();
            result.exceptionMessage = t.getMessage();
        }
        return result;
    }

    static List<CompiledProgram> compileSources(List<AlertProgram> sources) {
        List<CompiledProgram> compiled = new ArrayList<>();
        for (AlertProgram alertProgram : sources) {
            compiled.add(compileSource(SelVersion.MAX, alertProgram.program));
        }
        return compiled;
    }

    /**
     * Generate fresh data from same sources
     */
    public static void main(String[] args) throws IOException {
        System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "ru.yandex", "info");
        Locale.setDefault(Locale.ROOT);

        String prefix = "classpath:"; // or /tmp/ for fresh

        List<AlertProgram> alertPrograms = readAlertPrograms(prefix + ALERT_PROGRAMS);

        ZipOutputStream jar = new ZipOutputStream(new FileOutputStream("/tmp/compiler_canon_data.jar"));

        jar.putNextEntry(new ZipEntry(ALERT_PROGRAMS));
        writeJSON(alertPrograms, jar);
        jar.closeEntry();

        List<CompiledProgram> compiled = compileSources(alertPrograms);

        jar.putNextEntry(new ZipEntry(COMPILED_PROGRAMS));
        writeJSON(compiled, jar);
        jar.closeEntry();

        jar.close();
    }
}
