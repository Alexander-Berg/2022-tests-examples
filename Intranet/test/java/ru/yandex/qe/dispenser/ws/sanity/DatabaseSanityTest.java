package ru.yandex.qe.dispenser.ws.sanity;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;

import ru.yandex.qe.dispenser.domain.dao.DiJdbcTemplate;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

public class DatabaseSanityTest extends AcceptanceTestBase {
    @Autowired(required = false)
    private DiJdbcTemplate jdbcTemplate;

    @BeforeAll
    public void createInfiniteLoopFunction() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("CREATE OR REPLACE FUNCTION infinte_loop() RETURNS void "
                    + "AS $$ "
                    + "BEGIN "
                    + "  WHILE true LOOP "
                    + "  END LOOP; "
                    + "END; "
                    + "$$ LANGUAGE plpgsql;");
        }
    }

    @BeforeEach
    public void init() {
        Assumptions.assumeFalse(jdbcTemplate == null, "No jdbcTemplate found");
    }

    @Test
    public void queryTimeoutShouldBeSet() {
        //strange way to set execution timeout without @Timeout annotation from JUnit 5.5+
        Assertions.assertTimeout(Duration.ofSeconds(10), () -> {
            Assertions.assertThrows(DataAccessResourceFailureException.class, () -> {
                jdbcTemplate.execute("SELECT * FROM infinte_loop();");
            });
        });
    }

    @AfterAll
    public void deleteInfiniteLoopFunction() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("DROP FUNCTION infinte_loop();");
        }
    }
}
