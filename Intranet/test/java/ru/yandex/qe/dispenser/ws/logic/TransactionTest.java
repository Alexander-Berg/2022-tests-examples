package ru.yandex.qe.dispenser.ws.logic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.util.ApplicationContextProvider;
import ru.yandex.qe.dispenser.ws.intercept.TransactionFilter;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;
import ru.yandex.qe.dispenser.ws.param.DiExceptionMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"ErrorNotRethrown", "ProhibitedExceptionCaught", "OverlyBroadCatchBlock"})
public final class TransactionTest extends BusinessLogicTestBase {
    private static final Person ROBOT_COMMANDER_DATA = new Person("robot-commander-data", 1120000000102012L, true, false, false, PersonAffiliation.EXTERNAL);

    @Autowired
    private PersonDao personDao;

    @Test
    public void transactionShouldBeCommitedIfNoErrors() {
        skipIfNeeded();

        runInTransaction(() -> personDao.create(ROBOT_COMMANDER_DATA));
        assertTrue(personDao.contains(ROBOT_COMMANDER_DATA.getLogin()));
    }

    @Test
    public void transactionDoesNotChangeDatabaseBeforeCommit() throws InterruptedException {
        skipIfNeeded();

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final CountDownLatch latch3 = new CountDownLatch(1);
        new Thread(() -> {
            runInTransaction(() -> {
                personDao.create(ROBOT_COMMANDER_DATA);
                latch1.countDown();
                latch2.await();
            });
            latch3.countDown();
        }).start();
        latch1.await();
        assertFalse(personDao.contains(ROBOT_COMMANDER_DATA.getLogin()));
        latch2.countDown();
        latch3.await();
        assertTrue(personDao.contains(ROBOT_COMMANDER_DATA.getLogin()));
    }

    @Test
    public void transactionShouldRollbackWhenExceptionInCode() {
        skipIfNeeded();

        try {
            runInTransaction(() -> {
                personDao.create(ROBOT_COMMANDER_DATA);
                new DiExceptionMapper().toResponse(new RuntimeException());
            });
        } catch (RuntimeException ignored) {
        }
        assertFalse(personDao.contains(ROBOT_COMMANDER_DATA.getLogin()));
    }

    @Test
    public void transactionShouldRollbackWhenErrorSomewhere() throws Exception {
        skipIfNeeded();

        try {
            runInTransaction(() -> {
                personDao.create(ROBOT_COMMANDER_DATA);
                throw new Error();
            });
        } catch (Error ignored) {
        }
        assertFalse(personDao.contains(ROBOT_COMMANDER_DATA.getLogin()));
    }

    /**
     * DISPENSER-294: Не открывать транзакцию для создания проектов, если создавать ничего не надо
     */
    @Test
    public void dontOpenTransactionForPersonalProjectsCreationIfNothingToCreate() {
        skipIfNeeded();

        final AtomicInteger counter = new AtomicInteger();
        final Consumer<TransactionTemplate> transactionCounter = tw -> counter.incrementAndGet();
        TransactionWrapper.INSTANCE.addTransactionListener(transactionCounter);
        try {
            int i = 0;

            dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();
            i += 2;
            assertEquals(counter.get(), i);

            dispenser().quotas().changeInService(NIRVANA).shareEntity(SINGLE_USAGE_UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();
            i += isHierarchyEnabled() ? 2 : 1;
            assertEquals(counter.get(), i);

            dispenser().quotas().changeInService(NIRVANA).shareEntity(SINGLE_USAGE_UNIT_ENTITY, LYADZHIN.chooses(VERTICALI)).perform();
            i += 2;
            assertEquals(counter.get(), i);

            updateHierarchy();
            dispenser().quotas().changeInService(NIRVANA).shareEntity(SINGLE_USAGE_UNIT_ENTITY, LYADZHIN.chooses(VERTICALI)).perform();
            i += 1;
            assertEquals(counter.get(), i);
        } finally {
            TransactionWrapper.INSTANCE.removeTransactionListener(transactionCounter);
        }
    }

    /**
     * {@link TransactionFilter#needTransaction}
     * DISPENSER-411: Не открывать транзакцию для ответов из кеша
     */
    @Test
    public void dontOpenTransactionForGetRequests() {
        skipIfNeeded();

        final AtomicInteger counter = new AtomicInteger();
        final Consumer<TransactionTemplate> transactionCounter = tw -> counter.incrementAndGet();
        TransactionWrapper.INSTANCE.addTransactionListener(transactionCounter);
        try {
            dispenser().projects().get().avaliableFor(LYADZHIN.getLogin()).perform();
            assertEquals(0, counter.get());
            dispenser().quotas().get().inService(NIRVANA).forResource(STORAGE).perform();
            assertEquals(0, counter.get());
        } finally {
            TransactionWrapper.INSTANCE.removeTransactionListener(transactionCounter);
        }
    }

    private void skipIfNeeded() {
        Assumptions.assumeTrue(ApplicationContextProvider.containsBean(PlatformTransactionManager.class), "Skipping the test case");
    }
}
