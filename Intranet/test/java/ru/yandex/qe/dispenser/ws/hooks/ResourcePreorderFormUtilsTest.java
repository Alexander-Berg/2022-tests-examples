package ru.yandex.qe.dispenser.ws.hooks;

import java.util.List;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.dao.property.PropertyDao;
import ru.yandex.qe.dispenser.domain.property.Property;
import ru.yandex.qe.dispenser.domain.property.PropertyManager;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

/**
 * @author Nikita Minin <spasitel@yandex-team.ru>
 */
public class ResourcePreorderFormUtilsTest extends AcceptanceTestBase {

    @Autowired
    private PropertyManager propertyManager;

    @Autowired
    private PropertyDao propertyDao;

    @BeforeEach
    public void init() {
        propertyDao.clear();
        propertyDao.create(new Property(0, "resource_preorder_form", "ignored_resources", Property.Type.STRING.createValue("seg,ip4,io_ssd,io_hdd,gpu,net")));
        updateHierarchy();
    }

    @Test
    public void valueOfGood() {
        Assertions.assertNotNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN-seg:default-cpu:10-mem:40-hdd:1-ssd:0,00000001-ip4:0-io_ssd:1000-io_hdd:0-gpu:0-gpu_q:0", false));
        Assertions.assertNotNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN-seg:default-cpu:10-mem:40-hdd:1", false));
        Assertions.assertNotNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN-seg:default-cpu:10-mem:40-hdd:1-ssd:1", false));
        Assertions.assertNotNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN-seg:default-cpu:10-mem:40.1-hdd:1.56-ssd:1.88", false));
        Assertions.assertNotNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN-seg:default-cpu:10-mem:40,1-hdd:1,56-ssd:1,88", false));
    }

    @Test
    public void valueOfMultiLineGood() {
        @NotNull final List<ResourcePreorderFormUtils.QuotaBySegment> quotaBySegments = ResourcePreorderFormUtils.getQuotaBySegments("YP",
                "loc:MAN-seg:default-cpu:0-mem:0-hdd:0-ssd:0.0000001-ip4:0-io_ssd:0-io_hdd:0-gpu:0-gpu_q:0" +
                        "\r\nloc:MAN-seg:default-cpu:0-mem:0.0000001-hdd:0-ssd:0-ip4:0-io_ssd:0-io_hdd:0-gpu:0-gpu_q:0", false);
        Assertions.assertNotNull(quotaBySegments);
        Assertions.assertEquals(2, quotaBySegments.size());
        Assertions.assertEquals(1, quotaBySegments.get(0).quotaByResource.size());
        Assertions.assertEquals(1, quotaBySegments.get(1).quotaByResource.size());

    }

    @Test
    public void valueOfEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "", false));
        });
    }

    @Test
    public void valueOfNoLoc() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "seg:default-cpu:10-mem:40-hdd:1", false));
        });
    }


    @Test
    public void valueOfBadSeq() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertNull(ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "loc:MAN--seg:default-cpu:10-mem:40-hdd:1-ssd:1", false));
        });
    }

    @Test
    public void ignoredResourcesCanBeSetThroughProperty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "foo:foo-bar:bar-loc:SAS", false);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "bar:bar-bar:bar-loc:SAS", false);
        });

        propertyManager.setProperty("resource_preorder_form", "ignored_resources", Property.Type.STRING.createValue("foo,bar"));

        updateHierarchy();

        ResourcePreorderFormUtils.QuotaBySegment.valueOf("YP", "foo:foo-bar:bar-loc:SAS", false);
    }

    @Test
    public void newResourceModeShouldWorkNormally() {
        List<ResourcePreorderFormUtils.QuotaBySegment> quotaBySegments = ResourcePreorderFormUtils.getQuotaBySegments("YP", "loc:MAN-seg:default-cpu:1-mem:1-hdd:1-ssd:1-ip4:0-io_ssd:1-io_hdd:1-gpu:0-gpu_q:1", true);
        Assertions.assertEquals(1, quotaBySegments.size());
        ResourcePreorderFormUtils.QuotaBySegment quotaBySegment = Iterables.getOnlyElement(quotaBySegments);
        Assertions.assertEquals("MAN", quotaBySegment.loc);
        Assertions.assertEquals("default", quotaBySegment.segment);
        Assertions.assertEquals(DiAmount.of(1000, DiUnit.PERMILLE_CORES), quotaBySegment.quotaByResource.get("cpu_segmented"));
        Assertions.assertEquals(DiAmount.of(1_073_741_824L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ram_segmented"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("hdd_segmented"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ssd_segmented"));
        Assertions.assertEquals(DiAmount.of(1, DiUnit.COUNT), quotaBySegment.quotaByResource.get("gpu_segmented"));
        Assertions.assertEquals(DiAmount.of(1_048_576, DiUnit.BINARY_BPS), quotaBySegment.quotaByResource.get("io_ssd"));
        Assertions.assertEquals(DiAmount.of(1_048_576, DiUnit.BINARY_BPS), quotaBySegment.quotaByResource.get("io_hdd"));

        quotaBySegments = ResourcePreorderFormUtils.getQuotaBySegments("Qloud", "loc:MAN-seg:default-cpu:1-mem:1-hdd:1-ssd:1-ip4:0-io_ssd:1-io_hdd:1-gpu:0-gpu_q:1", true);
        Assertions.assertEquals(1, quotaBySegments.size());
        quotaBySegment = Iterables.getOnlyElement(quotaBySegments);
        Assertions.assertEquals("MAN", quotaBySegment.loc);
        Assertions.assertEquals("default", quotaBySegment.segment);
        Assertions.assertEquals(DiAmount.of(1000, DiUnit.PERMILLE_CORES), quotaBySegment.quotaByResource.get("cpu_segmented"));
        Assertions.assertEquals(DiAmount.of(1_073_741_824L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ram_segmented"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("hdd_segmented"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ssd"));

        quotaBySegments = ResourcePreorderFormUtils.getQuotaBySegments("RTC (GenCfg)", "loc:MAN-seg:default-cpu:1-mem:1-hdd:1-ssd:1-ip4:0-io_ssd:1-io_hdd:1-gpu:0-gpu_q:1", true);
        Assertions.assertEquals(1, quotaBySegments.size());
        quotaBySegment = Iterables.getOnlyElement(quotaBySegments);
        Assertions.assertEquals("MAN", quotaBySegment.loc);
        Assertions.assertEquals("default", quotaBySegment.segment);
        Assertions.assertEquals(DiAmount.of(1000, DiUnit.PERMILLE_CORES), quotaBySegment.quotaByResource.get("cpu"));
        Assertions.assertEquals(DiAmount.of(1_073_741_824L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ram"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("hdd"));
        Assertions.assertEquals(DiAmount.of(1_099_511_627_776L, DiUnit.BYTE), quotaBySegment.quotaByResource.get("ssd"));
        Assertions.assertEquals(DiAmount.of(1, DiUnit.COUNT), quotaBySegment.quotaByResource.get("gpu"));
    }
}
