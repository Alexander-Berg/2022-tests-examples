package ru.yandex.qe.dispenser.standalone;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.domain.juggler.ActiveCheckKwargs;
import ru.yandex.qe.dispenser.domain.juggler.ActiveCheckType;
import ru.yandex.qe.dispenser.domain.juggler.AddCheckResult;
import ru.yandex.qe.dispenser.domain.juggler.Check;
import ru.yandex.qe.dispenser.domain.juggler.CheckData;
import ru.yandex.qe.dispenser.domain.juggler.JugglerApi;
import ru.yandex.qe.dispenser.domain.juggler.JugglerNotification;
import ru.yandex.qe.dispenser.domain.juggler.notifications.OnChangeKwargs;
import ru.yandex.qe.dispenser.domain.juggler.notifications.Template;
import ru.yandex.qe.dispenser.ws.QuotaCheckService;

import static java.util.stream.Collectors.toSet;

@ParametersAreNonnullByDefault
public class MockJugglerApi implements JugglerApi {
    private final Collection<Check> checks = new ArrayList<>();

    public MockJugglerApi() {
        checks.add(new Check("dispenser-dev.yandex-team.ru",
                "yandex_yt-cpu",
                "dispenser_quotas",
                Collections.singleton("project_yandex"),
                ActiveCheckType.HTTPS,
                new ActiveCheckKwargs(
                        "/api/v1/check-quotas?projectKey=yandex&quotaSpecKey=/nirvana/yt-cpu&valueType=PERCENT&value=90&compareType=GREATER",
                        Collections.singleton(QuotaCheckService.STATUS_OK),
                        Collections.emptyList(),
                        true
                ),
                Collections.singleton(
                        new JugglerNotification(Template.ON_STATUS_CHANGE,
                                new OnChangeKwargs(Collections.singletonList("denblo"), Collections.singletonList("telegram"), Collections.emptySet()), "")
                ),
                "",
                Collections.emptyMap()
        ));
    }

    @Override
    public AddCheckResult addOrUpdate(final Check check, final int execFlag) {
        checks.removeIf(c -> c.getHost().equals(check.getHost()) && c.getService().equals(check.getService()));
        checks.add(check);
        return new AddCheckResult(true, "OK");
    }

    @Override
    public Map<String, Map<String, CheckData>> getServiceChecks(@Nullable final String host,
                                                                @Nullable final String service,
                                                                final int includeNotifications,
                                                                @Nullable final String tags, final int execFlag) {
        final Table<String, String, CheckData> result = HashBasedTable.create();
        final Set<String> parsedTags = parseTags(tags);

        for (final Check check : checks) {
            if ((host == null || host.equals(check.getHost())) && (service == null || service.equals(check.getService()))
                    && check.getTags().containsAll(parsedTags)) {
                final CheckData checkData = new CheckData(check.getNamespace(), check.getTags(), check.getActive(), check.getActiveKwargs(), check.getNotifications(), check.getDescription(), check.getMeta());
                result.put(check.getHost(), check.getService(), checkData);
            }
        }


        return result.rowMap();
    }

    @Override
    public void removeCheck(@Nullable final String host, @Nullable final String serviceName, @Nullable final String tags,
                            final int execFlag) {
        final Set<String> parsedTags = parseTags(tags);

        checks.removeIf(check -> (host == null || host.equals(check.getHost())) &&
                (serviceName == null || serviceName.equals(check.getService())) &&
                check.getTags().containsAll(parsedTags));
    }

    @NotNull
    private Set<String> parseTags(@Nullable final String tags) {
        final Set<String> parsedTags;
        final String trimmedTags = StringUtils.trimToNull(tags);
        if (trimmedTags != null) {
            parsedTags = Arrays.stream(trimmedTags.split(" +[.] +"))
                    .map(String::trim)
                    .collect(toSet());
        } else {
            parsedTags = Collections.emptySet();
        }
        return parsedTags;
    }
}
