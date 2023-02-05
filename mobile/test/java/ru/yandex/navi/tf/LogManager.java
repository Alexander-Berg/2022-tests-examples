package ru.yandex.navi.tf;

import org.openqa.selenium.logging.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class LogManager {
    private final ArrayList<String> allLogs = new ArrayList<>();
    private int logPos = 0;
    private final MobileUser user;
    private final String logType;

    private static int SEARCH_FIND_FIRST = 0x0001;
    private static int SEARCH_ALL_LOGS = 0x0002;

    LogManager(MobileUser user) {
        this.user = user;

        // Available Log Types:
        // iOS: syslog, crashlog, performance, server, safariConsole, safariNetwork, client
        // Android: logcat, bugreport, server, client
        this.logType = (user.getPlatform() == Platform.iOS ? "syslog" : "logcat");
    }

    void readLogs() {
        getLogs();
        logPos = allLogs.size();
    }

    private void getLogs() {
        List<String> logs = user.getDriver().manage().logs().get(logType).getAll().stream()
                .map(LogEntry::getMessage)
                .collect(Collectors.toList());
        allLogs.addAll(logs);
    }

    String find(String substring) {
        return doFind(substring, SEARCH_FIND_FIRST);
    }

    String searchInAllLogs(String substring) {
        return doFind(substring, SEARCH_ALL_LOGS | SEARCH_FIND_FIRST);
    }

    private String doFind(String substring, int options) {
        getLogs();

        final List<String> result = searchFor(substring, options);
        if (result.isEmpty())
            return null;
        return result.get(0);
    }

    public String getAllLogs() {
        readLogs();
        return String.join("\n", allLogs);
    }

    public List<String> getCrashLog() {
        if (user.getPlatform() == Platform.Android)
            return getAndroidCrashLog();
        else
            return getIosCrashLog();
    }

    private List<String> getAndroidCrashLog() {
        final Pattern pattern = Pattern.compile(
            " F (?!google-breakpad)|NaviFailedAssertionListener|"
                + " E AndroidRuntime: | E ActivityManager: ");

        getLogs();
        return searchFor(pattern, SEARCH_ALL_LOGS);
    }

    private List<String> getIosCrashLog() {
        getLogs();
        return searchFor(Pattern.compile("YandexNavigator.*Assertion failed"), SEARCH_ALL_LOGS);
    }

    public String getExperiments() {
        List<String> experiments = new ArrayList<>();
        List<String> records = searchFor(
            Pattern.compile("internal::AppComponentImpl::onParametersUpdated:"), SEARCH_ALL_LOGS);
        final String key = "[experiment parameter] ";
        for (String record : records) {
            int pos = record.indexOf(key);
            if (pos < 0)
                continue;
            pos += key.length();
            experiments.add(record.substring(pos));
        }
        return String.join("\n", experiments);
    }

    private List<String> searchFor(Pattern pattern, int options) {
        final List<String> result = new ArrayList<>();
        int pos = ((options & SEARCH_ALL_LOGS) != 0 ? 0 : logPos);

        while (pos < allLogs.size()) {
            final String record = allLogs.get(pos); pos++;
            final Matcher matcher = pattern.matcher(record);
            if (matcher.find()) {
                result.add(record.substring(matcher.start()));
                if ((options & SEARCH_FIND_FIRST) != 0)
                    break;
            }
        }

        if ((options & SEARCH_ALL_LOGS) == 0)
            logPos = pos;

        return result;
    }

    private List<String> searchFor(String substring, int options) {
        final List<String> result = new ArrayList<>();
        int pos = ((options & SEARCH_ALL_LOGS) != 0 ? 0 : logPos);

        while (pos < allLogs.size()) {
            final String record = allLogs.get(pos); pos++;
            if (record.contains(substring)) {
                result.add(record);
                if ((options & SEARCH_FIND_FIRST) != 0)
                    break;
            }
        }

        if ((options & SEARCH_ALL_LOGS) == 0)
            logPos = pos;

        return result;
    }
}
