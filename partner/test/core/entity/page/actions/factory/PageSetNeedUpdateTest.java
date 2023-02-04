package ru.yandex.partner.core.entity.page.actions.factory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.result.MassResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.action.result.ActionsResult;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.page.model.BasePage;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.repository.PageTypedRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.page.PageStateFlag;
import ru.yandex.partner.defaultconfiguration.PartnerLocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class PageSetNeedUpdateTest {
    @Autowired
    DSLContext dsl;
    @Autowired
    ContextPageSetNeedUpdateActionFactory pageActionFactory;
    @Autowired
    ActionPerformer actionPerformer;
    @Autowired
    PageTypedRepository pageTypedRepository;

    @Test
    void testSetNeedUpdate() {
        LocalDateTime runTime = PartnerLocalDateTime.now()
                // DB stores seconds, prevents flap
                .truncatedTo(ChronoUnit.SECONDS);
        Set<Long> ids = pageTypedRepository.getAll(QueryOpts.forClass(ContextPage.class)
                .withProps(Set.of(ContextPage.ID))
        ).stream().map(BasePage::getId).collect(Collectors.toSet());

        ActionsResult<?> result = actionPerformer.doActions(false,
                pageActionFactory.createAction(ids)
        );

        assertThat(result.isCommitted()).isTrue();
        assertThat(result.getResult(ContextPage.class).iterator().next().getSuccessfulCount()).isGreaterThan(0);

        var errorsPerPage = result.getErrors().get(ContextPage.class);
        assertThat(pageTypedRepository.getSafely(ids, ContextPage.class))
                .allSatisfy(page -> {
                    var errors = errorsPerPage.get(page.getId());
                    if (errors != null && !errors.isEmpty()) {
                        assertThat(page.getMultistate()).matches(has(PageStateFlag.NEED_UPDATE).negate());
                        assertThat(page.getUpdateTime())
                                .matches(lastUpdate -> lastUpdate == null || lastUpdate.isBefore(runTime),
                                        "was not updated");
                    } else {
                        assertThat(page.getMultistate()).matches(has(PageStateFlag.NEED_UPDATE));
                        assertThat(page.getUpdateTime()).isAfterOrEqualTo(runTime);
                    }
                });

        assertThat(result.getResult(ContextPage.class).iterator().next().getSuccessfulCount()).isEqualTo(15);
        assertThat(result.getResult(ContextPage.class).iterator().next().getErrorCount()).isEqualTo(0);
        assertThat(result.getErrors().get(ContextPage.class)).hasSize(1);
    }

    @Test
    void testSetNeedUpdateParallelSafe() throws ExecutionException, InterruptedException, TimeoutException {
        LocalDateTime runTime = PartnerLocalDateTime.now()
                // DB stores seconds, prevents flap
                .truncatedTo(ChronoUnit.SECONDS);
        List<Long> ids = pageTypedRepository.getAll(QueryOpts.forClass(ContextPage.class)
                .withProps(Set.of(ContextPage.ID))
        ).stream().map(BasePage::getId).collect(Collectors.toList());

        ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(ids.size()));
        CountDownLatch countDownLatch = new CountDownLatch(ids.size());

        Future<List<ActionsResult<?>>> futureResults = ids.stream()
                .map(id -> pageActionFactory.createAction(List.of(id)))
                .map(action -> (Callable<ActionsResult<?>>) () -> {
                    countDownLatch.countDown();

                    // context is clear
                    assertThat(actionPerformer.getActionContextFacade().hasErrors()).isFalse();
                    assertThat(actionPerformer.getActionContextFacade()
                            .getActionContext(ContextPage.class)
                            .actionLogger().hasRecords()
                    ).isFalse();

                    ActionsResult<?> result = actionPerformer.doActions(false, action);

                    // context is clear
                    // assertThat(actionPerformer.getActionContextFacade().hasErrors()).isFalse();
                    assertThat(actionPerformer.getActionContextFacade()
                            .getActionContext(ContextPage.class)
                            .actionLogger().hasRecords()
                    ).isFalse();

                    return result;
                })
                .map(executor::submit)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Futures::allAsList));

        try {
            var results = futureResults.get(10, TimeUnit.SECONDS);

            var resultPerPage = IntStream.range(0, ids.size())
                    .boxed()
                    .collect(Collectors.toMap(
                            ids::get,
                            results::get
                    ));

            assertThat(pageTypedRepository.getSafely(ids, ContextPage.class))
                    .allSatisfy(page -> {
                        var errors =
                                resultPerPage.get(page.getId()).getErrors().get(ContextPage.class);
                        if (errors != null && !errors.isEmpty()) {
                            assertThat(page.getMultistate()).matches(has(PageStateFlag.NEED_UPDATE).negate());
                            assertThat(page.getUpdateTime())
                                    .matches(lastUpdate -> lastUpdate == null || lastUpdate.isBefore(runTime),
                                            "was not updated");
                        } else {
                            assertThat(page.getMultistate()).matches(has(PageStateFlag.NEED_UPDATE));
                            assertThat(page.getUpdateTime()).isAfterOrEqualTo(runTime);
                        }
                    });

            assertThat(resultPerPage.values().stream()
                    .map(ar -> ar.getResult(ContextPage.class))
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .map(MassResult::getSuccessfulCount)
                    .reduce(0, Integer::sum)
            ).isEqualTo(15);
            assertThat(resultPerPage.values().stream()
                    .map(ar -> ar.getResult(ContextPage.class))
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .map(MassResult::getErrorCount)
                    .reduce(0, Integer::sum)
            ).isEqualTo(0);
            assertThat(resultPerPage.values().stream()
                    .map(ar -> ar.getErrors().get(ContextPage.class))
                    .filter(Objects::nonNull)
                    .map(Map::size)
                    .reduce(0, Integer::sum)
            ).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            futureResults.cancel(true);
        }
        executor.awaitTermination(10L, TimeUnit.SECONDS);
    }
}
