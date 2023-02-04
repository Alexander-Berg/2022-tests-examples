package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.DiSegmentation;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceSegmentation;
import ru.yandex.qe.dispenser.domain.Segmentation;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.segmentation.ResourceSegmentationDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceSegmentationValidationTest extends BusinessLogicTestBase {

    private static final String SEGMENT_CPU_NO_QUOTAS = "SEGMENT_CPU_NO_QUOTAS";

    @Autowired
    private ResourceSegmentationDao resourceSegmentationDao;
    @Autowired
    private ResourceDao resourceDao;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        final Service yp = Hierarchy.get().getServiceReader().read(YP);
        final Segmentation dcSegmentation = Hierarchy.get().getSegmentationReader().read(new Segmentation.Key(DC_SEGMENTATION));
        final Segmentation anotherSegmentation = Hierarchy.get().getSegmentationReader().read(new Segmentation.Key(SEGMENT_SEGMENTATION));

        final Resource segmentCpu = resourceDao.create(new Resource.Builder(SEGMENT_CPU_NO_QUOTAS, yp)
                .name("Segment CPU")
                .type(DiResourceType.PROCESSOR)
                .mode(DiQuotingMode.DEFAULT)
                .build());

        updateHierarchy();

        resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                segmentCpu, dcSegmentation
        ).build());

        resourceSegmentationDao.create(new ResourceSegmentation.Builder(
                segmentCpu, anotherSegmentation
        ).build());

        updateHierarchy();
    }

    @Test
    public void resourceSegmentaionCanBeViewed() {
        final List<DiSegmentation> segmentations = dispenser().service(YP)
                .resource(SEGMENT_CPU_NO_QUOTAS)
                .segmentations()
                .get()
                .perform();

        assertFalse(segmentations.isEmpty());
    }

    @Test
    public void resourceSegmentationCanBeCleared() {

        dispenser().service(YP)
                .resource(SEGMENT_CPU_NO_QUOTAS)
                .segmentations()
                .update(Collections.emptyList())
                .performBy(AMOSOV_F);

        updateHierarchy();

        final List<DiSegmentation> segmentations = dispenser().service(YP)
                .resource(SEGMENT_CPU_NO_QUOTAS)
                .segmentations()
                .get()
                .perform();

        assertTrue(segmentations.isEmpty());
    }

    @Test
    public void resourceSegmentationCanBeUpdated() {
        dispenser().service(YP)
                .resource(SEGMENT_CPU_NO_QUOTAS)
                .segmentations()
                .update(Arrays.asList(
                        new DiSegmentation.Builder(SEGMENT_SEGMENTATION)
                                .build()
                ))
                .performBy(AMOSOV_F);

        updateHierarchy();

        final List<DiSegmentation> segmentations = dispenser().service(YP)
                .resource(SEGMENT_CPU_NO_QUOTAS)
                .segmentations()
                .get()
                .perform();

        assertEquals(1, segmentations.size());
    }

    @Test
    public void resourceSegmentationCantBeUpdatedForResourcesWithQuotaSpecs() {
        assertThrowsWithMessage(() -> {
            dispenser().service(YP)
                    .resource(SEGMENT_CPU)
                    .segmentations()
                    .update(Arrays.asList(
                            new DiSegmentation.Builder(DC_SEGMENTATION)
                                    .build()
                    ))
                    .performBy(AMOSOV_F);
        }, "Can't modify segmentations for resource with created quota spec");
    }
}
