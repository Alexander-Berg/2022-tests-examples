package ru.yandex.solomon.codec.archive.header;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.solomon.codec.serializer.OwnerField;
import ru.yandex.stockpile.api.EProjectId;

/**
 * @author Stepan Koltsov
 */
public class OwnerFieldTest {

    @Test
    public void merge() {
        Assert.assertEquals(0, OwnerField.mergeOwnerField(0, 0));
        Assert.assertEquals(10, OwnerField.mergeOwnerField(0, 10));
        Assert.assertEquals(10, OwnerField.mergeOwnerField(10, 0));
        Assert.assertEquals(11, OwnerField.mergeOwnerField(10, 11));
        Assert.assertEquals(11, OwnerField.mergeOwnerField(12, 11));
    }

    @Test
    public void forNumberOrUnknown() {
        for (EProjectId projectId : EProjectId.values()) {
            if (projectId == EProjectId.UNRECOGNIZED) {
                continue;
            }
            Assert.assertSame(projectId, OwnerField.forNumberOrUnknown(projectId.getNumber()));
        }
        Assert.assertSame(EProjectId.UNKNOWN, OwnerField.forNumberOrUnknown(-1));
        Assert.assertSame(EProjectId.UNKNOWN, OwnerField.forNumberOrUnknown(9999));
    }

}
