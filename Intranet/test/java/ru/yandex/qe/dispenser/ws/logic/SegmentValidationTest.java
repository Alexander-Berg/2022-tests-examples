package ru.yandex.qe.dispenser.ws.logic;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiSegment;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SegmentValidationTest extends BusinessLogicTestBase {

    @Test
    public void segmentsCanBeViewed() {
        final DiListResponse<DiSegment> segments = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform();

        assertFalse(segments.isEmpty());
    }

    @Test
    public void segmentCanBeViewed() {
        final DiSegment segment = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().byKey(DC_SEGMENT_2)
                .get()
                .perform();

        assertEquals(segment.getKey(), DC_SEGMENT_2);
    }

    @Test
    public void segmentCanBeCreated() {
        final Short priority = new Short("666");
        final String newSegmentKey = "test-segment";
        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .create(new DiSegment.Builder(newSegmentKey)
                        .withName("MSK")
                        .withDescription("Another dc")
                        .withPriority(priority)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiSegment> segments = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform();

        assertTrue(segments.stream().anyMatch(segment -> segment.getKey().equals(newSegmentKey) && segment.getPriority().equals(priority)));
    }

    @Test
    public void segmentCanBeCreatedWithoutPriority() {
        final short maxPriority = (short) (dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().get().perform().stream()
                .mapToInt(DiSegment::getPriority)
                .max()
                .orElse(0) + 1);

        final String newSegmentKey = "test-segment";

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .create(new DiSegment.Builder(newSegmentKey)
                        .withName("MSK")
                        .withDescription("Another dc")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiSegment> segments = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform();

        assertTrue(segments.stream().anyMatch(segment -> segment.getKey().equals(newSegmentKey) && segment.getPriority().equals(maxPriority)));
    }

    @Test
    public void segmentCanBeUpdated() {
        final Short priority = new Short("666");
        final String newSegmentKey = "test-segment";

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .create(new DiSegment.Builder(newSegmentKey)
                        .withName("MSK")
                        .withDescription("Another dc")
                        .withPriority(priority)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiSegment first = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform()
                .stream()
                .filter(segment -> segment.getKey().equals(newSegmentKey) && segment.getPriority().equals(priority))
                .findFirst()
                .get();

        final Short anotherPriority = new Short("999");

        final DiSegment second = new DiSegment.Builder(first.getKey())
                .withDescription(first.getDescription())
                .withName(first.getName())
                .inSegmentation(first.getSegmentation())
                .withPriority(anotherPriority)
                .build();

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().byKey(first.getKey())
                .update(second)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiSegment first2 = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform()
                .stream()
                .filter(segment -> segment.getKey().equals(newSegmentKey))
                .findFirst()
                .get();

        assertEquals(first2.getPriority(), anotherPriority);
    }

    @Test
    public void segmentCanBeUpdatedWithoutPriority() {
        final short maxPriority = (short) (dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().get().perform().stream()
                .mapToInt(DiSegment::getPriority)
                .max()
                .orElse(0) + 1);
        final String newSegmentKey = "test-segment";

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .create(new DiSegment.Builder(newSegmentKey)
                        .withName("MSK")
                        .withDescription("Another dc")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiSegment first = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform()
                .stream()
                .filter(segment -> segment.getKey().equals(newSegmentKey) && segment.getPriority().equals(maxPriority))
                .findFirst()
                .get();

        final DiSegment second = new DiSegment.Builder(first.getKey())
                .withDescription(first.getDescription())
                .withName(first.getName())
                .inSegmentation(first.getSegmentation())
                .build();

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().byKey(first.getKey())
                .update(second)
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiSegment first2 = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform()
                .stream()
                .filter(segment -> segment.getKey().equals(newSegmentKey))
                .findFirst()
                .get();

        assertEquals((short) first2.getPriority(), maxPriority);
    }

    @Test
    public void segmentGetByOrder() {

        final String segmentationKey = "testingSegmentation";
        dispenser().segmentations()
                .create(new DiSegmentation.Builder(segmentationKey)
                        .withDescription("testingSegmentation")
                        .withName("testingSegmentation")
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final String newSegmentKey = "test-segment";
        for (short i = 1; i <= 5; i++ ) {
            dispenser()
                    .segmentations().byKey(segmentationKey)
                    .segments()
                    .create(new DiSegment.Builder(newSegmentKey + "-" + i)
                            .withName("segment name " + i)
                            .withDescription("Another dc " + i)
                            .withPriority(i)
                            .build())
                    .performBy(AMOSOV_F);

            updateHierarchy();
        }

        final DiListResponse<DiSegment> segments = dispenser()
                .segmentations().byKey(segmentationKey)
                .segments()
                .get()
                .perform();

        final Iterator<DiSegment> iterator = segments.iterator();
        for (short i = 1; i <= 5; i++) {
            assertEquals(iterator.next().getPriority().shortValue(), i);
        }
    }

    @Test
    public void segmentCanBeRemoved() {
        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().byKey(DC_SEGMENT_1)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiListResponse<DiSegment> segments = dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .get()
                .perform();

        assertFalse(segments.stream().anyMatch(segment -> segment.getKey().equals(DC_SEGMENT_1)));
    }

    @Test
    public void quotaMustBeCreatedOnSegmentAdded() {

        final DiQuotaGetResponse quotasBefore = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform();

        final long dcSegmentCount = quotasBefore.stream()
                .filter(q -> q.getSegmentKeys().contains(DC_SEGMENT_1))
                .count();

        final String newSegmentKey = "test-segment";
        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments()
                .create(new DiSegment.Builder(newSegmentKey)
                        .withName("MSK")
                        .withDescription("Another dc")
                        .withPriority((short) 1)
                        .build())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse quotasAfter = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform();

        assertTrue(quotasAfter.stream()
                .anyMatch(q -> q.getSegmentKeys().contains(newSegmentKey))
        );

        assertEquals(quotasAfter.size(), quotasBefore.size() + dcSegmentCount);
    }

    @Test
    public void quotaMustBeRemovedOnSegmentRemoved() {

        final DiQuotaGetResponse quotasBefore = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform();

        dispenser()
                .segmentations().byKey(DC_SEGMENTATION)
                .segments().byKey(DC_SEGMENT_2)
                .delete()
                .performBy(AMOSOV_F);

        updateHierarchy();

        final DiQuotaGetResponse quotasAfter = dispenser().quotas()
                .get()
                .ofProject(YANDEX)
                .inService(YP)
                .forResource(SEGMENT_CPU)
                .perform();

        final long dcSegmentCount = quotasBefore.stream()
                .filter(q -> q.getSegmentKeys().contains(DC_SEGMENT_1))
                .count();

        assertFalse(quotasAfter.stream()
                .anyMatch(q -> q.getSegmentKeys().contains(DC_SEGMENT_2))
        );

        assertEquals(quotasAfter.size(), quotasBefore.size() - dcSegmentCount);
    }

}
