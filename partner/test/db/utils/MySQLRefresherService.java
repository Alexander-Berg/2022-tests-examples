package ru.yandex.partner.test.db.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;

import ru.yandex.partner.dbschema.partner.Tables;
import ru.yandex.partner.dbschema.partner.tables.AllPages;
import ru.yandex.partner.dbschema.partner.tables.AllPagesView;
import ru.yandex.partner.test.db.QueryLogService;
import ru.yandex.partner.test.utils.TestUtils;

public class MySQLRefresherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLRefresherService.class);
    private static final Set<String> CROSS_TRIGGERED = Set.of(
            "pages",
            "context_on_site_campaign",
            "internal_context_on_site_campaign",
            "mobile_app_settings",
            "internal_mobile_app"
    );
    private final DSLContext dslContext;
    private final QueryLogService queryLogService;

    public MySQLRefresherService(DSLContext dslContext, QueryLogService queryLogService) {
        this.dslContext = dslContext;
        this.queryLogService = queryLogService;
    }

    public void refresh(String schemaName) throws IOException, URISyntaxException {
        List<String> affectedTableNames = getAffectedTableNames();
        if (!affectedTableNames.isEmpty()) {
            partialRefresh(schemaName, affectedTableNames);
        }
    }

    public void forceRefresh(String schemaName, boolean withData)
            throws IOException, URISyntaxException {
        fullRefresh(schemaName, withData);
    }

    private void fullRefresh(String schemaName, boolean withData) throws IOException,
            URISyntaxException {
        dslContext.execute("SET sql_mode='NO_AUTO_VALUE_ON_ZERO'");
        dslContext.execute("DROP SCHEMA " + schemaName);
        dslContext.execute("CREATE SCHEMA " + schemaName);
        dslContext.execute("use " + schemaName);

        List<String> sortedResourcePaths = getSortedResourcePaths(withData);

        dslContext.execute("set foreign_key_checks=0");

        for (String resourcePath : sortedResourcePaths) {
            initSqlFromResource(resourcePath);
        }

        dslContext.dropTable(AllPages.ALL_PAGES).execute();
        dslContext.execute("RENAME TABLE " + AllPagesView.ALL_PAGES_VIEW.getName() + " to " + AllPages.ALL_PAGES.getName());

        dslContext.execute("set foreign_key_checks=1");
    }

    private void partialRefresh(String schemaName,
                                List<String> affectedTableNames) throws IOException, URISyntaxException {
        dslContext.execute("SET sql_mode='NO_AUTO_VALUE_ON_ZERO'");
        dslContext.execute("set foreign_key_checks=0");

        List<String> sortedResourcePaths = getSortedResourcePaths(true);

        affectedTableNames.forEach(tableName -> {
                    dslContext.execute("delete from " + tableName);
                    dslContext.execute("alter table " + tableName + " auto_increment = 1");
                }
        );
        for (String resourcePath : sortedResourcePaths) {
            String tableName = getTableNameFromResourcePath(resourcePath, schemaName);
            if (!(resourcePath.endsWith(".data.sql") && affectedTableNames.contains(tableName))) {
                continue;
            }

            initSqlFromResource(resourcePath);
        }
        dslContext.execute("set foreign_key_checks=1");
    }

    @PostConstruct
    private void init() {
        try {
            forceRefresh("partner", true);
        } catch (IOException | URISyntaxException e) {
            throw new BeanCreationException("Failed to configure DSLContext", e);
        }
    }

    // assume path format: schema/table_name.(data|schema|view).sql
    private String getTableNameFromResourcePath(String resourcePath, String schema) {
        return resourcePath
                // первая точка будет сразу после имени таблицы https://stackoverflow.com/a/776135/14986254
                .substring(0, resourcePath.indexOf("."))
                .substring((schema + "/").length());
    }

    private void initSqlFromResource(String resourcePath) throws IOException {
        LOGGER.debug("Execute resource: {}", resourcePath);

        try (InputStream is = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            String fileContent = IOUtils.toString(is);
            String statementDelimiter = ";";
            var delimiterPattern = Pattern.compile("delimiter (.*)", Pattern.CASE_INSENSITIVE);
            String[] strings = fileContent.split("\n");
            StringBuilder currentStatement = new StringBuilder();
            for (String string : strings) {
                var delimiterMatched = delimiterPattern.matcher(string);
                if (delimiterMatched.matches()) {
                    if (!currentStatement.isEmpty()) {
                        dslContext.execute(currentStatement.toString());
                    }
                    statementDelimiter = delimiterMatched.group(1);
                    currentStatement = new StringBuilder();
                    continue;
                }

                if (string.endsWith(statementDelimiter)) {
                    currentStatement.append(string, 0, string.length() - statementDelimiter.length());
                    dslContext.execute(currentStatement.toString());
                    currentStatement = new StringBuilder();
                } else {
                    currentStatement.append(string);
                    currentStatement.append("\n");
                }
            }
            if (!currentStatement.isEmpty()) {
                dslContext.execute(currentStatement.toString());
            }
        }
    }

    private List<String> getSortedResourcePaths(boolean withData) throws IOException, URISyntaxException {
        return getResourcePaths().stream()
                .filter(path -> withData || weight(path) < 3)
                .sorted(
                        Comparator
                                .comparingInt(this::weight)
                                .thenComparing(String::compareTo))
                .collect(Collectors.toList());
    }

    private int weight(String path) {
        return path.endsWith("schema.sql") ? 0
                : path.endsWith("view.sql") ? 1
                : path.endsWith("trigger.sql") ? 2
                : 3;
    }

    private List<String> getAffectedTableNames() {
        var affectedTables = queryLogService.getLog().stream()
                .filter(sql -> !TestUtils.isReadOnlyQuery(sql))
                .map(TestUtils::extractTableName)
                .collect(Collectors.toList());

        if (affectedTables.stream().anyMatch(CROSS_TRIGGERED::contains)) {
            affectedTables.addAll(CROSS_TRIGGERED);
        }

        return affectedTables;
    }

    private List<String> getResourcePaths() throws IOException, URISyntaxException {
        CodeSource src = Tables.class.getProtectionDomain().getCodeSource();
        if (src != null) {
            URL srcLocation = src.getLocation();
            LOGGER.info("Source location: {}", srcLocation);
            return srcLocation.getPath().endsWith(".jar")
                    ? fromJarURL(srcLocation)
                    : fromFileURL();
        } else {
            throw new IOException("Cannot find resources");
        }
    }

    private List<String> fromJarURL(URL jar) throws IOException {
        // в аркадии ищет в jar файле
        // https://stackoverflow.com/questions/1429172/how-to-list-the-files-inside-a-jar-file
        LOGGER.info("Search in JAR file");
        ZipInputStream zip = new ZipInputStream(jar.openStream());
        var list = new ArrayList<String>();
        while (true) {
            ZipEntry e = zip.getNextEntry();
            if (e == null) {
                break;
            }
            String name = e.getName();
            LOGGER.debug("Find: {}", name);
            if (name.startsWith("partner/") && !name.endsWith("/")) {
                LOGGER.debug("Added: {}", name);
                list.add(name);
            }
        }
        return list;
    }

    private List<String> fromFileURL() throws URISyntaxException, IOException {
        LOGGER.info("Search in Directory");
        return Files.walk(Path.of(getClass().getResource("/partner").toURI()))
                .map(Path::toFile)
                .filter(File::isFile)
                .map(File::getAbsolutePath)
                .map(filename -> filename.substring(filename.lastIndexOf("partner" + File.separator)))
                .collect(Collectors.toList());
    }
}
