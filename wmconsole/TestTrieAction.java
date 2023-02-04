package ru.yandex.webmaster3.coordinator.http;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.webmaster3.core.WebmasterException;
import ru.yandex.webmaster3.core.http.Action;
import ru.yandex.webmaster3.core.http.ActionResponse;
import ru.yandex.webmaster3.core.http.ReadAction;
import ru.yandex.webmaster3.storage.util.ydb.exception.WebmasterYdbException;
import ru.yandex.webmaster3.storage.host.AllHostsCacheService;
import ru.yandex.webmaster3.storage.host.dao.HostsYDao;

/**
 * @author avhaliullin
 */
@Slf4j
@ReadAction
@Component("/tmp/testTrie")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TestTrieAction extends Action<SimpleRequest, TestTrieAction.Response> {

    private final AllHostsCacheService allHostsCacheService;
    private final HostsYDao hostsYDao;

    @Override
    public Response process(SimpleRequest request) throws WebmasterException {
        allHostsCacheService.resolveTrie();
        try {
            AtomicLong hostCount = new AtomicLong(0L);
            hostsYDao.forEach(h -> {
                long count = hostCount.incrementAndGet();
                if (count % 100_000 == 0) {
                    log.info("Hosts processed: " + count);
                }

                if (!allHostsCacheService.contains(h.getLeft())) {
                    log.error("No host in trie: " + h.getLeft());
                }
            });
        } catch (WebmasterYdbException e) {
            log.error("Error iterating hosts", e);
        }

        return new Response(0, Collections.emptyList());
    }

    @Value
    public static class Response implements ActionResponse.NormalResponse {
        int count;
        List<Pair<String, String>> hosts;
    }

}
