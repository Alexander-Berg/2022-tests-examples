package ru.yandex.webmaster.common.contentpreview;

import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.util.collections.Cf;

/**
 * @author aherman
 */
public class ContentPreviewSettingsServiceTest {
    @Test
    public void testCombineByHostOne() throws Exception {
        ContentPreviewSettings cps = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        Iterator<ContentPreviewSettings> one = Cf.list(cps).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(one);
        Assert.assertTrue(result.hasNext());
        Assert.assertEquals(1, result.next().size());
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostTwoSame() throws Exception {
        ContentPreviewSettings cps1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2 = new ContentPreviewSettings(1, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);
        Iterator<ContentPreviewSettings> two = Cf.list(cps1, cps2).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(2, next.size());
        Assert.assertEquals(cps1, next.get(0));
        Assert.assertEquals(cps2, next.get(1));

        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostTwoDifferent() throws Exception {
        ContentPreviewSettings cps1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2 = new ContentPreviewSettings(2, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);
        Iterator<ContentPreviewSettings> two = Cf.list(cps1, cps2).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(1, next.size());
        Assert.assertEquals(cps1, next.get(0));

        Assert.assertTrue(result.hasNext());
        next = result.next();
        Assert.assertEquals(1, next.size());
        Assert.assertEquals(cps2, next.get(0));

        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostThree_3() throws Exception {
        ContentPreviewSettings cps1_1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps1_2 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps1_3 = new ContentPreviewSettings(1, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);

        Iterator<ContentPreviewSettings> two = Cf.list(cps1_1, cps1_2, cps1_3).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(3, next.size());
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostThree_1_2() throws Exception {
        ContentPreviewSettings cps1_1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2_1 = new ContentPreviewSettings(2, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2_2 = new ContentPreviewSettings(2, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);

        Iterator<ContentPreviewSettings> two = Cf.list(cps1_1, cps2_1, cps2_2).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(1, next.size());

        Assert.assertTrue(result.hasNext());
        next = result.next();
        Assert.assertEquals(2, next.size());

        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostThree_2_1() throws Exception {
        ContentPreviewSettings cps1_1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps1_2 = new ContentPreviewSettings(1, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2_1 = new ContentPreviewSettings(2, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);

        Iterator<ContentPreviewSettings> two = Cf.list(cps1_1, cps1_2, cps2_1).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(2, next.size());

        Assert.assertTrue(result.hasNext());
        next = result.next();
        Assert.assertEquals(1, next.size());

        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void testCombineByHostThree_1_1_1() throws Exception {
        ContentPreviewSettings cps1_1 = new ContentPreviewSettings(1, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps2_1 = new ContentPreviewSettings(2, ContentPreviewType.MOBILE,
                ContentPreviewState.DISABLED);
        ContentPreviewSettings cps3_1 = new ContentPreviewSettings(3, ContentPreviewType.DESKTOP,
                ContentPreviewState.DISABLED);

        Iterator<ContentPreviewSettings> two = Cf.list(cps1_1, cps2_1, cps3_1).iterator();
        Iterator<List<ContentPreviewSettings>> result = ContentPreviewSettingsService.combineByHost(two);

        Assert.assertTrue(result.hasNext());
        List<ContentPreviewSettings> next = result.next();
        Assert.assertEquals(1, next.size());

        Assert.assertTrue(result.hasNext());
        next = result.next();
        Assert.assertEquals(1, next.size());

        Assert.assertTrue(result.hasNext());
        next = result.next();
        Assert.assertEquals(1, next.size());

        Assert.assertFalse(result.hasNext());
    }

}
