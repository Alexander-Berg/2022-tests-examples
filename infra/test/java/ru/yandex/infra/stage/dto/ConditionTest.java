package ru.yandex.infra.stage.dto;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

class ConditionTest {

    @Test
    void equalsIgnoreTimestamp() {
        assertThat(new Condition(Condition.Status.TRUE, "reason", "msg", Instant.now()),
                equalTo(new Condition(Condition.Status.TRUE, "reason", "msg", Instant.EPOCH)));

        assertThat(new Condition(Condition.Status.TRUE, "reason", "msg", Instant.EPOCH),
                not(equalTo(new Condition(Condition.Status.FALSE, "reason", "msg", Instant.EPOCH))));
        assertThat(new Condition(Condition.Status.TRUE, "reason", "msg", Instant.EPOCH),
                not(equalTo(new Condition(Condition.Status.TRUE, "reason2", "msg", Instant.EPOCH))));
        assertThat(new Condition(Condition.Status.TRUE, "reason", "msg", Instant.EPOCH),
                not(equalTo(new Condition(Condition.Status.TRUE, "reason", "msg2", Instant.EPOCH))));
    }

}
