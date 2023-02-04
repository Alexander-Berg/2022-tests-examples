package ru.yandex.qe.dispenser.ws.logic;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SegmentationValidationTest extends BusinessLogicTestBase {

    @Test
    public void segmentationsCanBeViewed() {
        final DiListResponse<DiSegmentation> segmentations = dispenser().segmentations().get()
                .perform();

        assertFalse(segmentations.isEmpty());
    }

    @Test
    public void segmentationCanBeViewed() {
        final DiSegmentation segmentations = dispenser().segmentations()
                .byKey(DC_SEGMENTATION)
                .get()
                .perform();

        assertEquals(DC_SEGMENTATION, segmentations.getKey());
    }

    @Test
    public void dispenserAdminCanCreateSegmentation() {
        dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segment")
                        .withName("name")
                        .withDescription("description")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        dispenser().segmentations()
                .byKey("test-segment")
                .get()
                .perform();
    }

    @Test
    public void dispenserAdminCanRemoveSegmentation() {
        dispenser()
                .segmentations()
                .byKey(DC_SEGMENTATION)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        assertThrows(Throwable.class, () -> {
            dispenser().segmentations()
                    .byKey(DC_SEGMENTATION)
                    .get()
                    .perform();
        });
    }

    @Test
    public void segmentationMustHavePriority() {
        dispenser()
                .segmentations()
                .create(new DiSegmentation.Builder("test-segmentation")
                        .withName("name")
                        .withDescription("description")
                        .withPriority(10)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final int priority = dispenser().segmentations()
                .byKey("test-segmentation")
                .get()
                .perform().getPriority();

        assertEquals(10, priority);
    }

    @Test
    public void priorityMustBeRequiredField() {
        assertThrowsWithMessage(() -> {
            createAuthorizedLocalClient(AMOSOV_F)
                    .path("/v1/segmentations")
                    .post(ImmutableMap.of(
                            "key", "test-segmentation",
                            "name", "name",
                            "description", "description"
                    ), DiSegmentation.class);
        }, "Priority is required");
    }
}
