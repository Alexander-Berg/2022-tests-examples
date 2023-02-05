package ru.yandex.disk.publicpage;

import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.test.AndroidTestCase2;

import static org.junit.Assert.assertNotEquals;

@Config(manifest = Config.NONE)
public class PublicLinkTest extends AndroidTestCase2 {

    @Test
    public void testPublicLinkLegacy() {
        final String link = "https://yadi.sk/d/bKIUFadu3Mcdem";
        testPublicLink(link, link);
    }

    @Test
    public void testPublicLink() {
        final String link = "https://disk.yandex.ru/d/bKIUFadu3Mcdem";
        testPublicLink(link, link);
    }

    @Test
    public void testPublicLinkWithSpecialCharactersLegacy() {
        final String link = "https://yadi.sk/d/-7u6PhR_7xZWm";
        testPublicLink(link, link);
    }

    @Test
    public void testPublicLinkWithSpecialCharacters() {
        final String link = "https://disk.yandex.ru/d/-7u6PhR_7xZWm";
        testPublicLink(link, link);
    }

    @Test
    public void testPublicLinkWithDomainLegacy() {
        final String link = "https://public3.dsp.yadi.sk/d/-7u6PhR_7xZWm";
        testPublicLink(link, link);
    }

    @Test
    public void testPublicLinkWithDomain() {
        final String link = "https://disk3.dsp.yandex.ru/d/-7u6PhR_7xZWm";
        testPublicLink(link, link);
    }

    private void testPublicLink(final String originalLink, final String expectedValue) {
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals(null, link.getPath());
        assertEquals(expectedValue, link.getPublicKey());
        assertEquals(expectedValue, link.getFullPublicKey());
        assertEquals(expectedValue, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithTrailingSlashLegacy() {
        final String originalLink = "https://yadi.sk/d/B3Wv-kGI3NhiR8/";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals(null, link.getPath());
        assertEquals("https://yadi.sk/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals("https://yadi.sk/d/B3Wv-kGI3NhiR8", link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithTrailingSlash() {
        final String originalLink = "https://disk.yandex.ru/d/B3Wv-kGI3NhiR8/";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals(null, link.getPath());
        assertEquals("https://disk.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals("https://disk.yandex.ru/d/B3Wv-kGI3NhiR8", link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathLegacy() {
        final String originalLink = "https://yadi.sk/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://yadi.sk/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePath() {
        final String originalLink = "https://disk.yandex.ru/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathCom() {
        final String originalLink = "https://disk.yandex.com/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk.yandex.com/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathUa() {
        final String originalLink = "https://disk.yandex.ua/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk.yandex.ua/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathTr() {
        final String originalLink = "https://disk.yandex.com.tr/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk.yandex.com.tr/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathDsp() {
        final String originalLink = "https://disk.dsp.yandex.ru/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk.dsp.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathDsp2() {
        final String originalLink = "https://disk2.dsp.yandex.ru/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk2.dsp.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathDspDisabled() {
        final String originalLink = "https://disk.dsp.yandex.ru/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, false);
        assertNotEquals("/path/file.jpg", link.getPath());
        assertNotEquals("https://disk.dsp.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFilePathPr() {
        final String originalLink = "https://pr-22372.regtests.dsp.yandex.ru/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://pr-22372.regtests.dsp.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFolderPathLegacy() {
        final String originalLink = "https://yadi.sk/d/B3Wv-kGI3NhiR8/path";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path", link.getPath());
        assertEquals("https://yadi.sk/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithWrongHost() {
        final String originalLink = "https://disk.yandex.unknown/d/B3Wv-kGI3NhiR8/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertNotEquals("/path/file.jpg", link.getPath());
        assertNotEquals("https://disk.yandex.unknown/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithFolderPath() {
        final String originalLink = "https://disk.yandex.ru/d/B3Wv-kGI3NhiR8/path";
        final PublicLink link = new PublicLink(originalLink);
        assertEquals("/path", link.getPath());
        assertEquals("https://disk.yandex.ru/d/B3Wv-kGI3NhiR8", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithDomainAndFilePathLegacy() {
        final String originalLink = "https://public3.dsp.yadi.sk/d/-7u6PhR_7xZWm/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://public3.dsp.yadi.sk/d/-7u6PhR_7xZWm", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithDomainAndFilePath() {
        final String originalLink = "https://disk3.dsp.yandex.ru/d/-7u6PhR_7xZWm/path/file.jpg";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/path/file.jpg", link.getPath());
        assertEquals("https://disk3.dsp.yandex.ru/d/-7u6PhR_7xZWm", link.getPublicKey());
        assertEquals(originalLink, link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertFalse(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithHashLegacy() {
        final String originalLink = "https://public3.dsp.yadi.sk/public/?hash=jsT1zBgnYGHUCnEWc5A82cU%2FP8t%2Ff1xOFkwKNJpQKfE%3D%3A%2F20170824_111214.mp4";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/20170824_111214.mp4", link.getPath());
        assertEquals("jsT1zBgnYGHUCnEWc5A82cU/P8t/f1xOFkwKNJpQKfE=", link.getPublicKey());
        assertEquals("jsT1zBgnYGHUCnEWc5A82cU/P8t/f1xOFkwKNJpQKfE=:/20170824_111214.mp4", link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertTrue(link.isLongHashLink());
    }

    @Test
    public void testPublicLinkWithHash() {
        final String originalLink = "https://disk3.dsp.yandex.ru/public/?hash=jsT1zBgnYGHUCnEWc5A82cU%2FP8t%2Ff1xOFkwKNJpQKfE%3D%3A%2F20170824_111214.mp4";
        final PublicLink link = new PublicLink(originalLink, true);
        assertEquals("/20170824_111214.mp4", link.getPath());
        assertEquals("jsT1zBgnYGHUCnEWc5A82cU/P8t/f1xOFkwKNJpQKfE=", link.getPublicKey());
        assertEquals("jsT1zBgnYGHUCnEWc5A82cU/P8t/f1xOFkwKNJpQKfE=:/20170824_111214.mp4", link.getFullPublicKey());
        assertEquals(originalLink, link.getOriginalLink());
        assertTrue(link.isLongHashLink());
    }
}
