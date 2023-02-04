package ru.yandex.webmaster3.regtessiontester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Oleg Bazdyrev on 30/05/2017.
 */
public class RegressionTester {

    /**
     *  Набор тестируемых read-only action'ов
     */
    private static final Set<String> READ_ACTIONS = Sets.newHashSet("/sitemap/list",
            "/sitemap/list/children",
            "/sitemap/info",
            //"/robotstxt/analyze",
            //"/util/serverResponse",
            "/util/validateFilterRegex",
            "/user/host/list",
            "/user/host/suggest",
            "/user/host/verification/info",
            "/user/host/verification/usersVerifiedHost",
            "/user/host/verification/listApplicable",
            "/user/host/settings/read",
            "/user/info",
            "/user/settings/notifications/info",
            "/user/settings/notifications/listHostSettings",
            "/camelcase/info",
            "/messages/list",
            "/messages/getMessage",
            "/messages/unreadCount",
            "/serplinks/info",
            "/sitetree/info",
            "/sitetree/select",
            //"/sitetree/download/pagesArchive",
            "/sitetree/sample/pages",
            "/host/info",
            "/host/statistics",
            "/regions/suggest",
            "/regions/regionInfo",
            "/host/regions/info",
            "/host/convertHostId",
            "/host/convertNewHostIdToOld",
            "/urlchecker/list",
            "/urlchecker/info",
            "/urlchecker2/report",
            "/urlchecker2/requestsList",
            "/checklist/info",
            "/checklist/history",
            "/checklist/extras/brokenMicrodataPageSamples",
            "/checklist/extras/duplicatePageSamples",
            "/checklist/extras/emptyTitlePageSamples",
            "/checklist/extras/emptyDescriptionPageSamples",
            "/checklist/extras/notMobileFriendlyPageSamples",
            "/microdata/byDocUrl",
            "/microdata/byDocFile",
            "/microdata/byDocString",
            "/threats/list",
            "/internal/epic/fail",
            "/old-webmaster/api/hostRegionsInfo",
            "/old-webmaster/api/hostDisplayNameInfo",
            "/temporary/user/beta/status",
            "/temporary/subscription/info",
            "/internal/api/feedbackHostInfo",
            "/internal/api/feedbackHostsAddedByUser",
            "/mirrors/main",
            "/mirrors/requests",
            "/mirrors/generation/info",
            "/searchquery/queryInfo",
            "/searchquery/latest/filter",
            "/searchquery/group/list",
            "/searchquery/group/query/list",
            "/searchquery/group/history",
            "/searchquery/statistics/group/list",
            //"/searchquery/statistics/group/list/download",
            "/searchquery/statistics/group/indicator",
            //"/searchquery/statistics/group/indicator/download",
            "/searchquery/statistics/group/selected",
            //"/searchquery/statistics/group/selected/download",
            "/searchquery/statistics/group/history",
            "/searchquery/statistics/group/piechart",
            "/searchquery/statistics/query/suggest",
            "/searchquery/statistics/query/list",
            //"/searchquery/statistics/query/list/download",
            "/searchquery/statistics/query/selected",
            //"/searchquery/statistics/query/selected/download",
            "/searchquery/statistics/query/indicator",
            //"/searchquery/statistics/query/indicator/download",
            "/searchquery/statistics/query/history",
            "/searchquery/statistics/query/piechart",
            "/searchquery/statistics/dates",
            "/searchquery/statistics/missing",
            "/searchurl/event/samples",
            "/searchurl/event/history",
            "/searchurl/url/samples",
            "/searchurl/url/history",
            "/searchurl/excluded/samples",
            "/searchurl/excluded/statuses",
            "/searchurl/excluded/history",
            "/indexing2/url/history",
            "/indexing2/url/samples",
            "/indexing2/url/samples/codes",
            "/indexing2/event/history",
            "/indexing2/event/samples",
            "/indexing2/event/samples/codes",
            "/search/base/updates",
            "/links/indicators",
            "/links/history",
            "/links/archiveDownloadUrl",
            "/links2/external/history",
            "/links2/external/samples",
            "/links2/external/source/samples",
            "/links2/external/tciAndTld",
            "/links/internal/history",
            "/links/internal/samples",
            "/links/internal/normal/history",
            "/links/internal/normal/samples",
            "/links/internal/redirect/history",
            "/links/internal/redirect/samples",
            "/links/internal/broken/history",
            "/links/internal/broken/samples",
            "/links/internal/notDownloaded/history",
            "/links/internal/notDownloaded/samples",
            "/mobile/requestInfo",
            "/links/internal/broken/siteErrors/history",
            "/links/internal/broken/siteErrors/samples",
            "/links/internal/broken/disallowedByUser/history",
            "/links/internal/broken/disallowedByUser/samples",
            "/links/internal/broken/unsupportedByRobot/history",
            "/links/internal/broken/unsupportedByRobot/samples",
            "/searchquery/regions",
            "/searchquery/regionsParents",
            "/internal/clickhouse/prepare",
            "/internal/clickhouse/switch",
            "/originaltext/limits",
            "/originaltext/list",
            "/addurl/get",
            "/addurl/quota",
            "/dashboard/searchquery/statistics/dates",
            "/dashboard/searchquery/statistics/group/history",
            "/dashboard/searchquery/statistics/query/list",
            "/dashboard/searchurl/event/history",
            "/dashboard/searchurl/event/samples",
            "/dashboard/indexing/tci/history",
            "/dashboard/links2/external/samples",
            "/dashboard/indexing2/url/history",
            "/dashboard/indexing2/event/samples",
            "/searchquery/recommended/list",
            "/searchquery/recommended/region/list",
            "/importanturls/list",
            "/importanturls/history",
            "/importanturls/quota",
            "/turbo/settings/get",
            "/turbo/feed/list",
            "/turbo/feed/isEmpty",
            "/turbo/adfox/parse",
            "/turbo/urls/list",
            "/turbo/errors/supported",
            "/turbo/feed/history",
            "/turbo/offer/get");

    private static final String ACTION_REQUEST_MARK = "ActionRouter Request: ";
    private static final Pattern ACTION_REQUEST_PATTERN = Pattern.compile("\\[([0-9T\\-:.]+)] INFO  \\[([a-zA-Z0-9\\-]+)] \\[] " +
            "ActionRouter Request: action=([^\\s]+) parameters=(.+)");

    private static final ObjectMapper OM = new ObjectMapper();
    private static final ObjectMapper OM_PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final int MIN_INTERVAL_MS = 100;
    private static final int MAXIMUM_RUNS_PER_ACTION = 20;

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option inputOpt = new Option("i", "input", true, "Input log file for analyzing");
        inputOpt.setRequired(true);
        options.addOption(inputOpt);
        Option hostsOpt = new Option("h", "host", true, "Host to analyze. May (and should) be used several times");
        hostsOpt.setRequired(true);
        options.addOption(hostsOpt);
        options.addOption(null, "min-interval", true, "Minimum interval between requests, milliseconds");
        options.addOption(null, "max-runs-per-action", true, "Maximum runs with different parameters per action");
        options.addOption("ia", "ignore-action", true, "Ignore action <arg>. May be used several times");
        options.addOption("ll", "limit-lines", true, "Limit input usage to <arg> lines (default is 1 million)");

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("log-analyzer", options);
            System.exit(1);
            return;
        }

        String fileName = commandLine.getOptionValue("input");
        long minIntervalMs = Long.parseLong(commandLine.getOptionValue("min-interval", String.valueOf(MIN_INTERVAL_MS)));
        String[] hosts = commandLine.getOptionValues("host");
        int maxRunsPerAction = Integer.parseInt(commandLine.getOptionValue("max-runs-per-action", String.valueOf(MAXIMUM_RUNS_PER_ACTION)));
        String[] ignoreActions = commandLine.getOptionValues("ignore-action");
        Set<String> actionsToIgnore = ignoreActions == null ? Collections.emptySet() : Sets.newHashSet(ignoreActions);
        long limitLines = Long.parseLong(commandLine.getOptionValue("limit-lines", "1000000"));

        ExecutorService threadPool = Executors.newFixedThreadPool(hosts.length);
        Map<String, List<ActionRun>> runsByName = new TreeMap<>();
        final long[] prevRunMs = {0L};
        final int[] lineNumber = {1};

        Files.lines(Paths.get(fileName), Charsets.UTF_8).limit(limitLines).forEach(line -> {
            lineNumber[0]++;
            if (lineNumber[0] % 100000 == 0) {
                System.out.println("Line " + lineNumber[0]);
            }
            if (!line.contains(ACTION_REQUEST_MARK))
                return;
            Matcher matcher = ACTION_REQUEST_PATTERN.matcher(line);
            if (!matcher.matches()) {
                return;
            }
            String action = matcher.group(3);
            String paramString = matcher.group(4);
            if (!READ_ACTIONS.contains(action) || actionsToIgnore.contains(action)) {
                return;
            }
            // run action on each host
            try {
                List<ActionRun> runs = runsByName.computeIfAbsent(action, s -> new ArrayList<>());
                if (runs.size() >= maxRunsPerAction) {
                    return;
                }
                // search for same action run
                JsonNode parameters = OM.readTree(paramString);
                ActionRun actionRun = new ActionRun(action, parameters);
                if (runs.contains(actionRun)) {
                    return;
                }
                runs.add(actionRun);

                Thread.sleep(Math.max(0, minIntervalMs - (System.currentTimeMillis() - prevRunMs[0])));
                prevRunMs[0] = System.currentTimeMillis();

                System.out.println("Line " + lineNumber[0] + ". Running action: " + action);
                // run actions
                List<Future<ActionRunResult>> results = threadPool.invokeAll(Arrays.stream(hosts)
                        .map(host -> new ActionRunTask(host, action, parameters)).collect(Collectors.toSet()));
                for (Future<ActionRunResult> result : results) {
                    ActionRunResult actionRunResult = result.get();
                    actionRun.getResultsByHost().put(actionRunResult.getHost(), actionRunResult);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

        });
        threadPool.shutdown();
        System.out.println("OUTPUT");
        System.out.println("Total actions: " + runsByName.size());
        System.out.println("Total actions runned: " + runsByName.values().stream().mapToInt(List::size).sum());
        System.out.println("=================================================");
        // print different results
        runsByName.forEach((action, results) -> {
            for (ActionRun actionRun : results) {
                if (!actionRun.hasDifferentResults())
                    continue;
                System.out.println("Action: " + action);
                try {
                    System.out.println("Parameters: " + OM_PRETTY.writeValueAsString(actionRun.getParams()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Result: ");
                actionRun.getResultsByHost().forEach((host, actionRunResult) -> {
                    System.out.println("Host: " + host);
                    System.out.println(actionRunResult.toString());
                    System.out.println();
                });
                System.out.println("=================================================");
            }
        });
        System.exit(0);
    }
}
