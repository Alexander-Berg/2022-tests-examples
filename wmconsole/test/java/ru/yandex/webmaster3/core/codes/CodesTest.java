package ru.yandex.webmaster3.core.codes;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class  CodesTest {

    @Test
    public void testToStringId() throws Exception {
        for (int i = 1; i < 100; i++) {
            Assert.assertEquals(HttpCodeGroup.UNKNOWN, HttpCodeGroup.get(i));
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
            Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
        }

        for (int i = 100; i < 200; i++) {
            //Assert.assertTrue(HttpCodeGroup.HTTP_GROUP_1XX_ALL.contains(HttpCodeGroup.get(i)));
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
            Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));

        }

        for (int i = 200; i < 300; i++) {
            if (HttpCodeGroup.HTTP_GROUP_2XX.contains(HttpCodeGroup.get(i))) {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_2XX, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.NORMAL, LinkType.get(i));
                Assert.assertFalse(ErrorGroupEnum.get(i).isPresent());
            } else {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            }
        }

        for (int i = 300; i < 400; i++) {
            if (HttpCodeGroup.HTTP_GROUP_3XX.contains(HttpCodeGroup.get(i))) {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_3XX, DownloadedHttpCodeGroup.get(i));
                if (i == 301
                        || i == 302
                        || i == 303
                        || i == 305
                        || i == 307
                        || i == 308)
                {
                    Assert.assertEquals(LinkType.REDIRECT, LinkType.get(i));
                    Assert.assertFalse(ErrorGroupEnum.get(i).isPresent());
                } else {
                    Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                    Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
                }

            } else {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            }
        }

        for (int i = 400; i < 500; i++) {
            if (HttpCodeGroup.HTTP_GROUP_4XX.contains(HttpCodeGroup.get(i))) {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_4XX, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));

                if (i == 400
                        || i == 403
                        || i == 404
                        || i == 410)
                {
                    assertEquals(ErrorGroupEnum.DISALLOWED_BY_USER, ErrorGroupEnum.get(i));
                    Assert.assertNotEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
                } else {
                    Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
                }
            } else {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            }
        }
        for (int i = 500; i < 600; i++) {
            if (HttpCodeGroup.HTTP_GROUP_5XX.contains(HttpCodeGroup.get(i))) {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_5XX, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.get(i).get());
            } else {
                assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            }
        }

        for (int i = 600; i < 1000; i++) {
            Assert.assertEquals(HttpCodeGroup.UNKNOWN, HttpCodeGroup.get(i));
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
            Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
        }

        /*
        Robot codes
         */
        for (int i = 1000; i < 2000; i++) {
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));

            if (i == 1005) {
                Assert.assertEquals(ErrorGroupEnum.UNSUPPORTED_BY_ROBOT, ErrorGroupEnum.getForced(i));
            } else if (HttpCodeGroup.HTTP_GROUP_1003.contains(i)
                    || HttpCodeGroup.HTTP_GROUP_2005.contains(i)
                    || HttpCodeGroup.HTTP_GROUP_2025.contains(i))
            {
                Assert.assertEquals(ErrorGroupEnum.DISALLOWED_BY_USER, ErrorGroupEnum.getForced(i));
                Assert.assertNotEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            } else {
                Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
            }
        }

        for (int i = 2000; i < 3000; i++) {
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_2XX, DownloadedHttpCodeGroup.get(i));

            if (HttpCodeGroup.HTTP_GROUP_2XXX_SPECIAL.contains(i)) {
                Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
                if (i == 2007 || i == 2013)
                {
                    assertEquals(ErrorGroupEnum.UNSUPPORTED_BY_ROBOT, ErrorGroupEnum.get(i));
                } else if (HttpCodeGroup.HTTP_GROUP_1003.contains(i)
                        || HttpCodeGroup.HTTP_GROUP_2005.contains(i)
                        || HttpCodeGroup.HTTP_GROUP_2025.contains(i))
                {
                    Assert.assertEquals(ErrorGroupEnum.DISALLOWED_BY_USER, ErrorGroupEnum.getForced(i));
                } else {
                    Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
                }
            } else {
                Assert.assertEquals(LinkType.NORMAL, LinkType.get(i));

                if (i == 2021) {
                    Assert.assertEquals(ErrorGroupEnum.DISALLOWED_BY_USER, ErrorGroupEnum.getForced(i));
                } else {
                    Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
                }
            }
        }

        for (int i = 3000; i < 4000; i++) {
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
            Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
        }

        for (int i = 4000; i < 10000; i++) {
            Assert.assertEquals(HttpCodeGroup.UNKNOWN, HttpCodeGroup.get(i));
            assertEquals(DownloadedHttpCodeGroup.DOWNLOADED_COULD_NOT_DOWNLOAD, DownloadedHttpCodeGroup.get(i));
            Assert.assertEquals(LinkType.BROKEN, LinkType.get(i));
            Assert.assertEquals(ErrorGroupEnum.SITE_ERROR, ErrorGroupEnum.getForced(i));
        }
    }

    private static <T> void assertEquals(T expected, Optional<T> actual) {
        Assert.assertTrue("Optional is empty, expected: " + expected, actual.isPresent());
        Assert.assertEquals(expected, actual.get());
    }
}
