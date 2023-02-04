package ru.yandex.feedloader.storage.impl;

import com.amazonaws.services.s3.AmazonS3;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.yandex.feedloader.storage.FileStorageService;

import java.io.*;
import java.net.URL;

/**
 * Test for [[S3StorageService]], run with -Dcom.amazonaws.sdk.disableCertChecking=true
 *
 * @author alesavin
 */
@RunWith(JUnit4.class)
public class MdsStorageServiceTest extends TestCase {


    /*   @Test
    public void testPutGetDelete0() throws IOException {

        final AmazonS3ClientFactory factory =
                new AmazonS3ClientFactory(
                        "https://s3.mdst.yandex.net",
                        "yndx",
                        "yE5Wa4bP8GFc6OpLC296",
                        "etS16lPTUJqPYBgbtakRHNgBtQzlSn+UtjvCRMJV"
                );

        final AmazonS3 client = factory.newInstance();
        final String bucket = "vertis-feeds";

        FileStorageService service = new MdsStorageService(client, bucket);

        final String name = "61.txt";
        final File file = new File(name);
        final OutputStream os = new FileOutputStream(file);
        os.write("62.165.62.106; 62.165.62.106; 61 70.168.73.50; 70.168.73.50; 61 199.106.235.0; 199.106.235.127; 61 199.106.235.1; 199.106.235.1; 61 216.34.38.66; 216.34.38.66; 61\n".getBytes("UTF-8"));
        os.close();

        final String url = service.put(name, file);
        Assert.assertNotNull(url);
        System.out.println("URL: "  + url);
    }*/

    @Test
//    @Ignore
    public void testPutGetDelete() throws IOException {

        final AmazonS3ClientFactory factory =
                new AmazonS3ClientFactory(
                        "http://s3.mdst.yandex.net",
                        "yndx",
                        "yE5Wa4bP8GFc6OpLC296",
                        "etS16lPTUJqPYBgbtakRHNgBtQzlSn+UtjvCRMJV"
                );

        final AmazonS3 client = factory.newInstance();
        final String bucket = "vertis-feeds";
//        client.createBucket(bucket);

        FileStorageService service = new MdsStorageService(client, bucket);

        final String name = this.getClass().getName() + System.currentTimeMillis();
        final File file = new File(name);
        final OutputStream os = new FileOutputStream(file);
        os.write(name.getBytes("UTF-8"));
        os.close();

        final String url = service.put(name, file);
        Assert.assertNotNull(url);
        Assert.assertTrue(url.endsWith(name));

        final String url2 = service.getUrl(name);
        Assert.assertEquals(url, url2);

        final String name2 = service.getName(url);
        Assert.assertEquals(name, name2);

        final String contentEncoding = new URL(url)
                .openConnection()
                .getHeaderField("Content-Encoding");

        Assert.assertEquals(contentEncoding, "gzip");

        Assert.assertTrue(service.delete(name));
    }

    @After
    public void cleanup() {
        final File directory = new File(".");
        final String prefix = this.getClass().getName();

        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        });
        assert files != null;
        for (File f : files) {
            assert f.delete();
        }
    }
}
