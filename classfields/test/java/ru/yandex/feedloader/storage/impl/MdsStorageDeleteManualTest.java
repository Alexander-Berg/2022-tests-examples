package ru.yandex.feedloader.storage.impl;

import com.amazonaws.services.s3.AmazonS3;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ru.yandex.feedloader.storage.FileStorageService;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test deletion from [[S3StorageService]], run with -Dcom.amazonaws.sdk.disableCertChecking=true
 *
 * @author alesavin
 */
@RunWith(JUnit4.class)
public class MdsStorageDeleteManualTest extends TestCase {

    @Test
    @Ignore
    public void testPutGetDelete() throws IOException {

        final AmazonS3ClientFactory factory =
                new AmazonS3ClientFactory(
                        "https://s3.mds.yandex.net",
                        "yndx",
                        "aaaaa",
                        "xxxx"
                );

        final AmazonS3 client = factory.newInstance();
        final String bucket = "vertis-feeds";

        final FileStorageService service = new MdsStorageService(client, bucket);

        ExecutorService executorService = new ThreadPoolExecutor(50, 50,
                                              0L, TimeUnit.MILLISECONDS,
                                              new LinkedBlockingQueue<Runnable>(100),
                                              new ThreadPoolExecutor.CallerRunsPolicy());


        String filePath = "/Users/alesavin/tmp/mds/production/_list_to_delete3_sorted.txt";
//        String filePath = "/Users/alesavin/tmp/mds/production/test.txt";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
        final BufferedWriter log = new BufferedWriter(new FileWriter("/Users/alesavin/tmp/mds/production/log.txt", true));
        final String[] line = new String[1];
        final AtomicInteger i = new AtomicInteger();
        while ((line[0] = reader.readLine()) != null) {

            executorService.execute(new Runnable() {
                String s = line[0];
                @Override
                public void run() {
                    try {
                        service.delete(s);
                        synchronized (log) {
                            log.write(s + "\n");
                            log.flush();
                        }
                        if (i.incrementAndGet() % 100 == 0)
                            System.out.println("Progress: " + i);
                    } catch (Exception e) {
                        System.err.println("Exception: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

//            System.out.println(line);
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        reader.close();
        log.close();


//        Assert.assertTrue();
    }

}
