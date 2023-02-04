package ru.yandex.qe.dispenser.domain.util;

import org.junit.jupiter.api.Test;

import static ru.yandex.qe.dispenser.domain.util.ValidationUtils.validateEntityKey;
import static ru.yandex.qe.dispenser.domain.util.ValidationUtils.validateProjectKey;
import static ru.yandex.qe.dispenser.domain.util.ValidationUtils.validateReqId;

public class ValidationUtilsTest {
    @Test
    public void clusterApiReqIdShouldPassValidation() {
        validateReqId("rel-cluster-api_-9223372036831550037");
    }

    @Test
    public void nirvanaYtFileKeyShouldPassValidation() {
        validateEntityKey("edcc8349-accc-11e6-8db3-fa163eb3916c");
    }

    @Test
    public void nirvanaWorkflowKeyShouldPassValidation() {
        validateEntityKey("e00cc81c-e4c6-46b2-adf6-8e95d353f736");
    }

    @Test
    public void clusterApiWorkloadKeyShouldPassValidation() {
        validateEntityKey("cluster-api_-9223372036829760135");
    }

    @Test
    public void scraperBatchKeyShouldPassValidation() {
        validateEntityKey("1509017852721-f86cb6ac-404c-4eb9-a4b4-77480fbcf41b");
    }

    @Test
    public void projectKeyWithDashShouldPassValidation() {
        validateProjectKey("external-activities");
    }

    @Test
    public void projectKeyWithUnderscoreShouldPassValidation() {
        validateProjectKey("external_activities");
    }
}
