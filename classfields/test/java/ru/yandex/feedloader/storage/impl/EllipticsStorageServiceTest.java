package ru.yandex.feedloader.storage.impl;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.yandex.common.services.fs.EllipticFileStorageService;
import ru.yandex.feedloader.storage.FileStorageService;

import java.io.*;

/**
 * Test for [[RepeatableFileStorageService]]
 *
 * @author alesavin
 */
@RunWith(JUnit4.class)
public class EllipticsStorageServiceTest extends TestCase {

    @Test
    @Ignore
    public void testPutGetDelete() throws IOException {
        EllipticFileStorageService elliptics = new EllipticFileStorageService();
        elliptics.setServiceName("partnerdata-feedloader");
        elliptics.setPath("http://elliptics.test.vertis.yandex.net:80");

        FileStorageService service = new EllipticsStorageService(elliptics);

        final String name = this.getClass().getName() + System.currentTimeMillis();
        final File file = new File(name);
        final OutputStream os = new FileOutputStream(file);
        os.write(name.getBytes("UTF-8"));
        os.close();

        final String url = service.put(name, file);
        Assert.assertNotNull(url);
        Assert.assertEquals(url, name);

        final String url2 = service.getUrl(name);
        Assert.assertTrue(url2.endsWith(name));

        final String name2 = service.getName(url2);
        Assert.assertEquals(name, name2);

        Assert.assertTrue(service.delete(name));
    }

    @After
    public void cleanup() {
        final File directory = new File(".");
        final String prefix = this.getClass().getName();

        File[] files = directory.listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        });
        assert files != null;
        for (File f: files) { assert f.delete(); }
    }

}
