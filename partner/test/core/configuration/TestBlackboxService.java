package ru.yandex.partner.core.configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutor;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxException;
import ru.yandex.partner.libs.extservice.blackbox.BlackboxService;
import ru.yandex.partner.libs.extservice.blackbox.BlackboxUserInfo;
import ru.yandex.passport.tvmauth.TvmClient;

public class TestBlackboxService extends BlackboxService {

    private static final Map<Long, BlackboxUserInfo> MOCKED_USER_INFOS = Map.of(
            1015L, new BlackboxUserInfo(1015L, "ru", "123/avatar-id")
    );

    public TestBlackboxService(BlackboxRequestExecutor blackboxRequestExecutor, TvmClient tvmClient) {
        super(blackboxRequestExecutor, tvmClient, "blackbox", "127.0.0.1");
    }

    @Override
    public Map<Long, BlackboxUserInfo> getUserInfosNoCache(List<Long> uids) throws BlackboxException {
        return uids.stream().collect(Collectors.toMap(uid -> uid,
                uid -> MOCKED_USER_INFOS.getOrDefault(uid, new BlackboxUserInfo(uid, "ru", "0/0-0"))));
    }
}
