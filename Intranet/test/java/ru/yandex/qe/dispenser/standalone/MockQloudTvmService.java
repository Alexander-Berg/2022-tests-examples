package ru.yandex.qe.dispenser.standalone;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ru.yandex.qe.hitman.tvm.qloud.QloudTvmService;
import ru.yandex.qe.hitman.tvm.qloud.TvmServiceTicketInfo;
import ru.yandex.qe.hitman.tvm.qloud.TvmUserTicketInfo;

public class MockQloudTvmService implements QloudTvmService {

    private final Map<String, Integer> serviceTicketToSource;
    private final Set<String> allowedTargetClientIds;

    public MockQloudTvmService(final Map<String, Integer> serviceTicketToSource, final Set<String> allowedTargetClientIds) {
        this.serviceTicketToSource = serviceTicketToSource;
        this.allowedTargetClientIds = allowedTargetClientIds;
    }

    @Override
    public String getTicket(final String targetClientId) {
        if (allowedTargetClientIds.contains(targetClientId)) {
            return "FAKE-SERVICE-TICKET";
        } else {
            throw new IllegalArgumentException(
                    "Attempting to request ticket for disallowed destination " + targetClientId);
        }
    }

    @Override
    public boolean isValidTarget(final String clientId) {
        return allowedTargetClientIds.contains(clientId);
    }

    @Override
    public Optional<TvmServiceTicketInfo> validateServiceTicket(final String serviceTicket) {
        return Optional.ofNullable(serviceTicketToSource.get(serviceTicket))
                .map(s -> new TvmServiceTicketInfo(s, 1, Collections.emptyList(), "", ""));
    }

    @Override
    public Optional<TvmUserTicketInfo> validateUserTicket(final String userTicket) {
        return Optional.empty();
    }
}
