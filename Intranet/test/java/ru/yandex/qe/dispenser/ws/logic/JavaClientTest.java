package ru.yandex.qe.dispenser.ws.logic;

import javax.ws.rs.ForbiddenException;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;

public final class JavaClientTest extends BusinessLogicTestBase {
    @Test
    public void clientPerformExceptionMustContainServerErrorMessage() {
        final String errorMessage = createLocalClient().path("/v1/projects/pizza").get().readEntity(String.class);
        assertThrowsWithMessage(() -> dispenser().projects().get().withKey("pizza").perform(), HttpStatus.SC_NOT_FOUND, errorMessage);
    }

    @Test
    public void rejectProductionRequestsFromDevelopment() {
        Assertions.assertThrows(ForbiddenException.class, () -> {
            final Dispenser dispenser = createWebClientFactory(DispenserConfig.Environment.PRODUCTION).get();
            dispenser.projects().get().perform();
        });
    }
}
