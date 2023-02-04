package ru.yandex.webmaster3.storage.delurl;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.util.HtmlUtils;

/**
 * @author aherman
 */
public class DelurlServiceTest {
    @Test
    public void getMetaTagPredicate() throws Exception {
        List<HtmlUtils.MetaTag> metaTags = Lists.newArrayList(
                new HtmlUtils.MetaTag(null, null, null),
                new HtmlUtils.MetaTag("test", null, null),
                new HtmlUtils.MetaTag(null, "test", null),
                new HtmlUtils.MetaTag("test", "test", null),
                new HtmlUtils.MetaTag("robots", "all", null),
                new HtmlUtils.MetaTag("robots", "noindex", null),
                new HtmlUtils.MetaTag("robots", "nofollow", null),
                new HtmlUtils.MetaTag("robots", "none", null),
                new HtmlUtils.MetaTag("robots", "noindex,nofollow", null),
                new HtmlUtils.MetaTag("robots", "noindex, nofollow", null),
                new HtmlUtils.MetaTag("robots", " noindex, nofollow , test", null),
                new HtmlUtils.MetaTag("ROBOTS", " NOINDEX, NOFOLLOW , TEST", null),
                new HtmlUtils.MetaTag("yandex", "all", null),
                new HtmlUtils.MetaTag("yandex", "noindex", null),
                new HtmlUtils.MetaTag("yandex", "nofollow", null),
                new HtmlUtils.MetaTag("yandex", "none", null),
                new HtmlUtils.MetaTag("yandex", "noindex,nofollow", null),
                new HtmlUtils.MetaTag("yandex", "noindex, nofollow", null),
                new HtmlUtils.MetaTag("yandex", " noindex, nofollow , test", null),
                new HtmlUtils.MetaTag("YANDEX", " NOINDEX, NOFOLLOW , TEST", null)
        );
        List<HtmlUtils.MetaTag> filteredMetaTags =
                metaTags.stream().filter(DelUrlService.NOINDEX_METATAG_PREDICATE).collect(Collectors.toList());

        List<HtmlUtils.MetaTag> expected = Lists.newArrayList(
                new HtmlUtils.MetaTag("robots", "noindex", null),
                new HtmlUtils.MetaTag("robots", "none", null),
                new HtmlUtils.MetaTag("robots", "noindex,nofollow", null),
                new HtmlUtils.MetaTag("robots", "noindex, nofollow", null),
                new HtmlUtils.MetaTag("robots", " noindex, nofollow , test", null),
                new HtmlUtils.MetaTag("ROBOTS", " NOINDEX, NOFOLLOW , TEST", null),
                new HtmlUtils.MetaTag("yandex", "noindex", null),
                new HtmlUtils.MetaTag("yandex", "none", null),
                new HtmlUtils.MetaTag("yandex", "noindex,nofollow", null),
                new HtmlUtils.MetaTag("yandex", "noindex, nofollow", null),
                new HtmlUtils.MetaTag("yandex", " noindex, nofollow , test", null),
                new HtmlUtils.MetaTag("YANDEX", " NOINDEX, NOFOLLOW , TEST", null)
        );
        Assert.assertEquals(expected, filteredMetaTags);

        Assert.assertEquals(true, DelUrlService.NOINDEX_METATAG_PREDICATE.test(new HtmlUtils.MetaTag("robots", "follow, noindex", null)));
    }
}
