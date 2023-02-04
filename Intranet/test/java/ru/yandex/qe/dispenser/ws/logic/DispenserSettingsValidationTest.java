package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiDispenserSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DispenserSettingsValidationTest extends BusinessLogicTestBase {

    @Test
    public void getDispenserAdmins() {
        final DiDispenserSettings dispenser = dispenser().dispenserSettings().get().perform();
        assertEquals(dispenser.getAdmins(), Collections.singletonList(AMOSOV_F.getLogin()));
    }

    @Test
    public void updateDispenserAdmin() {

        final List<String> updatedAdmins = Arrays.asList(LOTREK.getLogin(), TERRY.getLogin());

        final DiDispenserSettings dispenserSettings = dispenser()
                .dispenserSettings()
                .update()
                .withAdmins(updatedAdmins)
                .performBy(AMOSOV_F);

        assertTrue(dispenserSettings.getAdmins().contains(LOTREK.getLogin()));

        updateHierarchy();

        final DiDispenserSettings updatedDispenser = dispenser().dispenserSettings().get().perform();

        assertEquals(updatedAdmins, updatedDispenser.getAdmins());
    }
}
