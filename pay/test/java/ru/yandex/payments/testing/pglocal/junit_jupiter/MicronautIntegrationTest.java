package ru.yandex.payments.testing.pglocal.junit_jupiter;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.testing.pglocal.Database;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static ru.yandex.payments.testing.pglocal.junit_jupiter.MicronautIntegrationTest.DB_NAME;
import static ru.yandex.payments.testing.pglocal.micronaut.PgLocalConfiguration.DB_NAME_PROPERTY;

@MicronautTest
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class MicronautIntegrationTest {
    static final String DB_NAME = "testdb";

    @Value("${local-pg.user}")
    String user;

    @Value("${local-pg.cluster.master.port}")
    int masterPort;

    @Value("${local-pg.cluster.slave.port}")
    int slavePort;

    private static int fetchUsersCount(Database database) {
        return database.fetch("SELECT COUNT(*) FROM users", resultSet -> {
            resultSet.next();
            return resultSet.getInt(1);
        });
    }

    @Test
    @DisplayName("Verify database cluster, containing master and slave, could be started using micronaut integration")
    public void clusterTest() {
        val master = new Database(DB_NAME, masterPort, user);
        assertThat(fetchUsersCount(master))
                .isZero();

        val slave = new Database(DB_NAME, slavePort, user);
        waitAtMost(10, SECONDS)
                .ignoreExceptions()
                .until(
                        () -> fetchUsersCount(slave),
                        usersCount -> usersCount == 0
                );
    }
}
