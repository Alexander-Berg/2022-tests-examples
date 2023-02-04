package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.regex.Pattern.matches;
import static java.util.stream.Collectors.toList;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static ru.yandex.vertis.utils.RerunUtils.hasRerunBuildFeature;

public class SaveFailedAdapter extends AgentLifeCycleAdapter {

    private static final String TEST_CASES = "/testsuite/testcase";

    private AgentRunningBuild runningBuild;
    private ArtifactsWatcher artifactsWatcher;
    private Path buildWorkingDir;


    public SaveFailedAdapter(@NotNull final EventDispatcher<AgentLifeCycleListener> events,
                             @NotNull final ArtifactsWatcher artifactsWatcher) {
        events.addListener(this);
        this.artifactsWatcher = artifactsWatcher;
    }

    @Override
    public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
        super.beforeRunnerStart(runner);
        buildWorkingDir = runner.getWorkingDirectory().toPath();
    }

    @Override
    public void beforeBuildFinish(@NotNull final AgentRunningBuild build,
                                  @NotNull final BuildFinishedStatus buildStatus) {
        super.beforeBuildFinish(build, buildStatus);
        this.runningBuild = build;
        if (!hasRerunBuildFeature(build)) {
            return;
        }

        try {
            saveFailed();
        } catch (Exception e) {
            getLogger().message("Couldn't save failed tests...");
        }
    }

    private void saveFailed() throws Exception {
        Path failed = runningBuild.getBuildTempDirectory().toPath().resolve(RerunConstant.FAILED);
        String mavenCommand = "";

        getLogger().message("Saving failed tests to " + failed);

        if (!Files.exists(failed)) {
            Files.createFile(failed);
        }

        if (Files.exists(buildWorkingDir)) {
            List<Path> resultsList = Files.walk(buildWorkingDir)
                    .filter(r -> matches("TEST-.*\\.xml", r.getFileName().toString()))
                    .collect(toList());
            mavenCommand = testToRerun(resultsList);
        }
        Files.write(failed, mavenCommand.getBytes());
        artifactsWatcher.addNewArtifactsPath(failed.toString() + "=>" + RerunConstant.TC_HIDDEN_DIR);
    }

    public static String testToRerun(List<Path> paths) {
        StringBuilder command = new StringBuilder();
        TreeSet<String> passed = new TreeSet<>();
        TreeSet<String> failed = new TreeSet<>();
        for (Path path : paths) {
            try (StringReader stream = new StringReader(readLineByLine(path))) {
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodes = (NodeList) xPath.compile(TEST_CASES)
                        .evaluate(new InputSource(stream), NODESET);

                for (int i = 0; i < nodes.getLength(); i++) {
                    String testName = createTestName(nodes.item(i));
                    if (rerunCaseCondition(xPath, nodes.item(i))) {
                        failed.add(testName);
                    } else {
                        passed.add(testName);
                    }
                }
            } catch (Exception e) {
                return "";
            }
        }
        failed.removeAll(passed);
        failed.forEach(f -> command.append(f).append(","));
        return command.toString();
    }

    private static String readLineByLine(Path filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    private static String createTestName(Node testCase) {
        return String.format("%s#%s",
                testCase.getAttributes().getNamedItem("classname").getNodeValue(),
                testCase.getAttributes().getNamedItem("name").getNodeValue());
    }

    private static boolean rerunCaseCondition(XPath xPath, Node testCase) {
        try {
            return (xPath.evaluate("failure", testCase, NODE) != null) ||
                    (xPath.evaluate("error", testCase, NODE) != null);
        } catch (XPathExpressionException e) {
            return false;
        }
    }

    private BuildProgressLogger getLogger() {
        return runningBuild.getBuildLogger();
    }
}
