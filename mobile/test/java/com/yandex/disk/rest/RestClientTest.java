/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.yandex.disk.rest.exceptions.CancelledDownloadException;
import com.yandex.disk.rest.exceptions.CancelledUploadingException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.WrongMethodException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.ApiVersion;
import com.yandex.disk.rest.json.DiskInfo;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Operation;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.yandex.disk.rest.util.Hash;
import com.yandex.disk.rest.util.Log;
import com.yandex.disk.rest.util.ResourcePath;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class RestClientTest {

    private static final String TAG = "RestClientTest";

    private RestClient client;

    @Before
    public void setUp() throws Exception {
        Log.setLevel(Level.INFO);

        Log.info(TAG, "pwd: " + new File(".").getAbsolutePath());

        final String user = System.getenv("restapi_test_user_name");
        final String token = System.getenv("restapi_test_user_token");

        assertThat(user, notNullValue());
        assertThat(token, notNullValue());

        this.client = createRestClient(user, token);
    }

    private static RestClient createRestClient(String user, String token) {
        Credentials credentials = new Credentials(user, token);

        final OkHttpClient okHttpClientBuilderWithLogging = OkHttpClientFactory.makeClient()
                .addInterceptor(new LoggingInterceptor(true))
                .build();
        return new RestClient(credentials, okHttpClientBuilderWithLogging);
    }

    @Ignore
    @Test
    public void testApiVersion() throws Exception {
        ApiVersion apiVersion = client.getApiVersion();
        Log.info(TAG, "apiVersion: " + apiVersion);
        assertThat(apiVersion.getBuild(), not(isEmptyOrNullString()));
        assertTrue("2.33.16".equalsIgnoreCase(apiVersion.getBuild()));
        assertTrue("v1".equalsIgnoreCase(apiVersion.getApiVersion()));
    }

    @Test
    public void testDiskInfo() throws Exception {
        DiskInfo diskInfo = client.getDiskInfo();
        Log.info(TAG, "diskInfo: " + diskInfo);
        assertThat(diskInfo.getTotalSpace(), greaterThan(0L));
        assertThat(diskInfo.getTrashSize(), greaterThanOrEqualTo(0L));
        assertThat(diskInfo.getUsedSpace(), greaterThanOrEqualTo(0L));
        assertThat(diskInfo.getSystemFolders(), isA(Map.class));
        assertThat(diskInfo.getSystemFolders(), hasKey("applications"));
        assertThat(diskInfo.getSystemFolders(), hasKey("downloads"));
    }

    @Test
    public void testListResources() throws Exception {
        int limit = 50;
        Resource resource = client.getResources(new ResourcesArgs.Builder()
                .setPath("/")
                .setLimit(limit)
                .setOffset(2)
                .build());
        assertTrue("dir".equals(resource.getType()));
        assertEquals(resource.getPath(), new ResourcePath("disk", "/"));
        Log.info(TAG, "self: " + resource);

        ResourceList items = resource.getResourceList();
        assertFalse(items == null);
        for (Resource item : items.getItems()) {
            Log.info(TAG, "item: " + item);
        }
        assertThat(items.getItems(), hasSize(9));
        assertThat(items.getItems().get(0).getName(), not(isEmptyOrNullString()));
    }

    @Test
    public void testListResources2() throws Exception {
        int limit = 50;
        int offset = 0;
        ResourceList items;
        do {
            Resource resource = client.getResources(new ResourcesArgs.Builder()
                    .setPath("/")
                    .setLimit(limit)
                    .setOffset(offset)
                    .build());
            assertTrue("dir".equals(resource.getType()));
            assertEquals(resource.getPath(), new ResourcePath("disk", "/"));
            Log.info(TAG, "self: " + resource);
            items = resource.getResourceList();
            assertFalse(items == null);
            for (Resource item : items.getItems()) {
                Log.info(TAG, "item: " + item);
            }
            offset += limit;
            Log.info(TAG, "offset: " + offset);
        } while (items.getItems().size() >= limit);
    }

    @Test
    public void testListResourcesHandler() throws Exception {
        ResourcesHandler parsingHandler = new ResourcesHandler() {
            final List<Resource> items = new ArrayList<>();

            @Override
            public void handleSelf(Resource item) {
                Log.info(TAG, "self: " + item);
            }

            @Override
            public void handleItem(Resource item) {
                items.add(item);
                Log.info(TAG, "item: " + item);
            }

            @Override
            public void onFinished(int itemsOnPage) {
                assertThat(items, hasSize(itemsOnPage));
                assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
            }
        };
        client.getResources(new ResourcesArgs.Builder()
                .setPath("/")
                .setParsingHandler(parsingHandler)
                .build());
    }

    @Ignore("Broken handle: /v1/disk/resources/files")
    @Test
    public void testFlatListResources() throws Exception {
        int limit = 50;
        ResourceList resourceList = client.getFlatResourceList(new ResourcesArgs.Builder()
                .setMediaType("video")
                .setLimit(limit)
                .setOffset(0)
                .build());
        Log.info(TAG, "resourceList: " + resourceList);
        assertEquals(resourceList.getPath(), null);

        List<Resource> items = resourceList.getItems();
        assertFalse(items == null);
        for (Resource item : items) {
            Log.info(TAG, "item: " + item);
            assertTrue(item.getMimeType().contains("video"));
        }
        assertThat(items, hasSize(1));
        assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
    }

    @Ignore("Broken handle: /v1/disk/resources/files")
    @Test
    public void testFlatListResourcesHandler() throws Exception {
        ResourcesHandler parsingHandler = new ResourcesHandler() {
            final List<Resource> items = new ArrayList<>();

            @Override
            public void handleSelf(Resource item) {
                throw new AssertionError();
            }

            @Override
            public void handleItem(Resource item) {
                items.add(item);
                Log.info(TAG, "item: " + item);
            }

            @Override
            public void onFinished(int itemsOnPage) {
                assertThat(items, hasSize(itemsOnPage));
                assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
            }
        };
        client.getFlatResourceList(new ResourcesArgs.Builder()
                .setMediaType("audio")
                .setParsingHandler(parsingHandler)
                .build());
    }

    @Ignore("Broken handle: /v1/disk/resources/last-uploaded")
    @Test
    public void testGetLastUploadedResources() throws Exception {
        int limit = 50;
        ResourceList resourceList = client.getLastUploadedResources(new ResourcesArgs.Builder()
                .setMediaType("video")
                .setLimit(limit)
                .setOffset(0)
                .build());
        Log.info(TAG, "resourceList: " + resourceList);
        assertEquals(resourceList.getPath(), null);

        List<Resource> items = resourceList.getItems();
        assertFalse(items == null);
        for (Resource item : items) {
            Log.info(TAG, "item: " + item);
            assertTrue(item.getMimeType().contains("video"));
        }
        assertThat(items, hasSize(1));
        assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
    }

    @Ignore("Broken handle: /v1/disk/resources/last-uploaded")
    @Test
    public void testUploadedListResourcesHandler() throws Exception {
        ResourcesHandler parsingHandler = new ResourcesHandler() {
            final List<Resource> items = new ArrayList<>();

            @Override
            public void handleSelf(Resource item) {
                throw new AssertionError();
            }

            @Override
            public void handleItem(Resource item) {
                items.add(item);
                Log.info(TAG, "item: " + item);
            }

            @Override
            public void onFinished(int itemsOnPage) {
                assertThat(items, hasSize(itemsOnPage));
                assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
            }
        };
        client.getLastUploadedResources(new ResourcesArgs.Builder()
                .setMediaType("audio")
                .setParsingHandler(parsingHandler)
                .build());
    }

    @Test
    public void testPatchResource() throws Exception {
        Map<String, Object> fooBar = new LinkedHashMap<>();
        fooBar.put("foo", 1);
        fooBar.put("bar", 2);
        Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
        properties.put("custom_properties", fooBar);

        Moshi moshi = new Moshi.Builder().build();

        JsonAdapter<Map> adapter = moshi.adapter(Map.class);
        RequestBody body = RequestBody
                .create(MediaType.parse("application/json"), adapter.toJson(properties));

        Resource resource = client.patchResource(new ResourcesArgs.Builder()
                .setPath("/0-test")
                .setBody(body)
                .build());
        assertTrue("dir".equals(resource.getType()));
        assertEquals(resource.getPath(), new ResourcePath("disk", "/0-test"));
        Log.info(TAG, "self: " + resource);
    }

    @Test
    public void testListTrash() throws Exception {
        final List<Resource> items = new ArrayList<>();
        ResourcesHandler parsingHandler = new ResourcesHandler() {
            @Override
            public void handleItem(Resource item) {
                Log.info(TAG, "item: " + item);
                items.add(item);
//                if (new ResourcePath("trash", "/.TheUnarchiverTemp0").equals(item.getPath())) {
//                    assertEquals(item.getOriginPath(), new ResourcePath("disk", "/apple/.TheUnarchiverTemp0"));
//                }
            }

            @Override
            public void onFinished(int itemsOnPage) {
                assertThat(items, hasSize(itemsOnPage));
                if (itemsOnPage > 0) {
                    assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
                }
            }
        };
        client.getTrashResources(new ResourcesArgs.Builder()
                .setPath("/")
                .setParsingHandler(parsingHandler)
                .build());
    }

    private void checkResult(Link link)
            throws IOException, InterruptedException, WrongMethodException, HttpCodeException {
        switch (link.getHttpStatus()) {
            case done:
                break;
            case inProgress:
                Operation operation = client.waitProgress(link, new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            if (++i > 100) {
                                throw new AssertionError();
                            }
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                assertThat(operation.getStatus(), not(isEmptyOrNullString()));
                assertTrue(operation.isSuccess());
                break;
            case error:
            default:
                throw new AssertionError();
        }
    }

    @Test
    public void testDeleteFromTrash() throws Exception {
        String name = "/drop-trash-test";
        String path = "/0-test" + name;

        try {
            client.delete(path, true);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        Link link = client.makeFolder(path);
        assertTrue(link.getHref() != null);
        assertTrue(link.getHref().equals(client.getUrl() + "/v1/disk/resources?path="
                + URLEncoder.encode("disk:" + path, "UTF-8")));

        String sub = path + "/sub1";
        Link link2 = client.makeFolder(sub);
        assertTrue(link2.getHref() != null);
        assertTrue(link2.getHref().equals(client.getUrl() + "/v1/disk/resources?path="
                + URLEncoder.encode("disk:" + sub, "UTF-8")));

        checkResult(client.delete(path, false));
        checkResult(client.deleteFromTrash(name));
    }

    @Test
    public void testRestoreFromTrash() throws Exception {
        String name = "/restore-trash-test";
        String path = "/0-test" + name;

        try {
            client.delete(path, true);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        Link link = client.makeFolder(path);
        assertTrue(link.getHref() != null);
        assertTrue(link.getHref().equals(client.getUrl() + "/v1/disk/resources?path="
                + URLEncoder.encode("disk:" + path, "UTF-8")));

        checkResult(client.delete(path, false));
        checkResult(client.restoreFromTrash(name, null, null));
    }

    @Test
    public void testDownloadFile() throws Exception {
        String path = "/download-test.jpg";
        File local = new File("/tmp/" + path);
        local.delete();
        assertFalse(local.exists());
        client.downloadFile(path, local, new ProgressListener() {
            @Override
            public void updateProgress(long loaded, long total) {
                Log.info(TAG, "updateProgress: " + loaded + " / " + total);
            }

            @Override
            public boolean hasCancelled() {
                return false;
            }
        });
        Log.info(TAG, "length: " + local.length());
        assertTrue(local.length() == 2031252);
        assertTrue(local.delete());
    }

    @Test
    public void testDownloadFileResume() throws Exception {
        String path = "/download-test.jpg";
        final File local = new File("/tmp/" + path);
        local.delete();
        assertFalse(local.exists());

        final int lastPass = 2;
        for (int i = 0; i <= lastPass; i++) {
            final int pass = i;
            try {
                client.downloadFile(path, new DownloadListener() {
                    boolean doCancel = false;

                    @Override
                    public OutputStream getOutputStream(boolean append) throws IOException {
                        return new FileOutputStream(local, append);
                    }

                    @Override
                    public long getLocalLength() {
                        return local.length();
                    }

                    @Override
                    public void updateProgress(long loaded, long total) {
                        Log.info(TAG, "updateProgress: pass=" + pass + ": " + loaded + " / " + total);
                        if (pass == 0 && loaded >= 10240) {
                            doCancel = true;
                        }
                        if (pass == 1 && loaded >= 102400) {
                            doCancel = true;
                        }
                    }

                    @Override
                    public boolean hasCancelled() {
                        if (doCancel) {
                            Log.info(TAG, "cancelled");
                        }
                        return doCancel;
                    }
                });
            } catch (CancelledDownloadException ex) {
                Log.info(TAG, "CancelledDownloadException");
            } catch (IOException ex) {
                if (pass >= lastPass) {
                    throw ex;
                }
            }
        }

        Log.info(TAG, "length: " + local.length());
        assertTrue(local.length() == 2031252);
        assertTrue(local.delete());
    }

    @Test
    public void testHash() throws Exception {
        File file = new File("../testResources/test-upload-001.bin");
        Hash hash = Hash.getHash(file);
        assertTrue(hash.getSize() == file.length());
        assertTrue("11968e619814b8f7f0367241d6ee1c2d".equalsIgnoreCase(hash.getMd5()));
        assertTrue("18339f4b55f3771b5486595686d0d43ff63da17edd0b30edb7e95f69abce5fad".equalsIgnoreCase(hash.getSha256()));
    }

    @Ignore("Broken handle: /v1/disk/resources/upload")
    @Test
    public void testSaveFromUrl() throws Exception {
        String url = "http://yastatic.net/morda-logo/i/apple-touch-icon/ru-76x76.png";
        String path = "/0-test/save-from-url-test.png";

        try {
            client.delete(path, false);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }

        Link saveLink = client.saveFromUrl(url, path);
        Operation operation = client.getOperation(saveLink);
        Log.info(TAG, "operation: " + operation);
        assertThat(operation.getStatus(), not(isEmptyOrNullString()));
    }

    @Test(expected = ServerIOException.class)
    public void testUploadFileOverwriteFailed() throws Exception {
        String path = "/download-test.jpg";
        client.getUploadLink(path, false);
    }

    @Test
    public void testUploadFileResume() throws Exception {
        String name = "test-upload-002.bin";
        String serverPath = "/0-test/" + name;
        String localPath = "/tmp/" + name;

        Runtime.getRuntime()
                .exec("/usr/bin/env dd if=/dev/urandom of=" + localPath + " bs=1048576 count=1")
                .waitFor();

        File local = new File(localPath);
        assertTrue(local.exists());
        assertTrue(local.length() == 1048576);

        Link link = client.getUploadLink(serverPath, true);
        final int lastPass = 2;
        for (int i = 0; i <= lastPass; i++) {
            final int pass = i;
            try {
                client.uploadFile(link, true, local, new ProgressListener() {
                    boolean doCancel = false;

                    @Override
                    public void updateProgress(long loaded, long total) {
                        Log.info(TAG, "updateProgress: pass=" + pass + ": " + loaded + " / " + total);
                        if (pass == 0 && loaded >= 10240) {
                            doCancel = true;
                        }
                        if (pass == 1 && loaded >= 102400) {
                            doCancel = true;
                        }
                    }

                    @Override
                    public boolean hasCancelled() {
                        if (doCancel) {
                            Log.info(TAG, "cancelled");
                        }
                        return doCancel;
                    }
                });
            } catch (CancelledUploadingException ex) {
                Log.info(TAG, "CancelledUploadingException");
            } catch (IOException ex) {
                if (pass >= lastPass) {
                    throw ex;
                }
            }
        }
    }

    @Test
    public void testMakeFolder() throws Exception {
        String path = "/0-test/make-folder-test";

        try {
            client.delete(path, false);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }

        Link link = client.makeFolder(path);
        Log.info(TAG, "link: " + link);
        Operation operation = client.getOperation(link);
        Log.info(TAG, "operation: " + operation);
        assertTrue(operation.getStatus() == null);
    }

    @Test
    public void testCopyFolder() throws Exception {
        String from = "/0-test/copy-folder-test-src";
        String to = "/0-test/copy-folder-test-dst";

        try {
            client.makeFolder(from);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        try {
            client.delete(to, false);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }

        Link link = client.copy(from, to, false);
        Log.info(TAG, "link: " + link);
        Operation operation = client.getOperation(link);
        Log.info(TAG, "operation: " + operation);
        assertTrue(operation.getStatus() == null);
    }

    @Test
    public void testMoveFolder() throws Exception {
        String from = "/0-test/move-folder-test-src";
        String to = "/0-test/move-folder-test-dst";

        try {
            client.makeFolder(from);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        try {
            client.delete(to, false);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }

        Link link = client.move(from, to, false);
        Log.info(TAG, "link: " + link);
        Operation operation = client.getOperation(link);
        Log.info(TAG, "operation: " + operation);
        assertTrue(operation.getStatus() == null);
    }

    @Test
    public void testPublishAndUnpublish() throws Exception {
        String path = "/0-test/publish-test";

        try {
            client.delete(path, false);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        client.makeFolder(path);

        client.publish(path);
        client.getResources(new ResourcesArgs.Builder()
                .setPath(path)
                .setParsingHandler(new ResourcesHandler() {
                    @Override
                    public void handleItem(Resource item) {
                        assertThat(item.getPublicKey(), not(isEmptyOrNullString()));
                        assertThat(item.getPublicUrl(), not(isEmptyOrNullString()));
                    }
                })
                .build());

        client.unpublish(path);
        client.getResources(new ResourcesArgs.Builder()
                .setPath(path)
                .setParsingHandler(new ResourcesHandler() {
                    @Override
                    public void handleItem(Resource item) {
                        assertThat(item.getPublicKey(), isEmptyOrNullString());
                        assertThat(item.getPublicUrl(), isEmptyOrNullString());
                    }
                })
                .build());
    }

    @Test
    public void testListPublicResources() throws Exception {
        String path = "/0-test/list-public-resources-test";

        try {
            client.delete(path, true);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        try {
            client.makeFolder(path);
        } catch (ServerIOException ex) {
            ex.printStackTrace();
        }
        for (int i = 1; i < 6; i++) {
            try {
                client.makeFolder(path + "/folder-" + i);
            } catch (ServerIOException ex) {
                ex.printStackTrace();
            }
        }

        Link link = client.publish(path);
        Log.info(TAG, "link: " + link);

        try {
            final String[] publicKey = new String[1];
            client.getResources(new ResourcesArgs.Builder()
                    .setPath(path)
                    .setParsingHandler(new ResourcesHandler() {
                        @Override
                        public void handleSelf(Resource item) {
                            publicKey[0] = item.getPublicKey();
                        }
                    })
                    .build());
            Log.info(TAG, "publicKey: " + publicKey[0]);

            client.listPublicResources(new ResourcesArgs.Builder()
                    .setPublicKey(publicKey[0])
                    .setParsingHandler(new ResourcesHandler() {
                        final List<Resource> items = new ArrayList<>();

                        @Override
                        public void handleItem(Resource item) {
                            items.add(item);
                            Log.info(TAG, "item: " + item);
                        }

                        @Override
                        public void onFinished(int itemsOnPage) {
                            assertThat(items, hasSize(itemsOnPage));
                            assertThat(items.get(0).getName(), not(isEmptyOrNullString()));
                        }
                    })
                    .build());

        } finally {
            try {
                client.unpublish(path);
            } catch (ServerIOException ex) {
                ex.printStackTrace();
            }
            client.delete(path, true);
        }
    }

    @Ignore("Broken handle: /v1/disk/public/resources/save-to-disk/")
    @Test
    public void testDownloadAndSavePublicResource() throws Exception {
        String path = "/download-test.jpg";
        File local = new File("/tmp/" + path);
        local.delete();
        assertFalse(local.exists());

        Link link = client.publish(path);
        Log.info(TAG, "link: " + link);
        try {
            final String[] publicKey = new String[1];
            client.getResources(new ResourcesArgs.Builder()
                    .setPath(path)
                    .setParsingHandler(new ResourcesHandler() {
                        @Override
                        public void handleSelf(Resource item) {
                            publicKey[0] = item.getPublicKey();
                        }
                    })
                    .build());

            Link savedLink = client.savePublicResource(publicKey[0], null, null);
            Log.info(TAG, "savedLink: " + savedLink);

            client.downloadPublicResource(publicKey[0], "", local, new ProgressListener() {
                @Override
                public void updateProgress(long loaded, long total) {
                    Log.info(TAG, "updateProgress: " + loaded + " / " + total);
                }

                @Override
                public boolean hasCancelled() {
                    return false;
                }
            });
            Log.info(TAG, "length: " + local.length());
            assertTrue(local.length() == 2031252);
            assertTrue(local.delete());

        } finally {
            client.unpublish(path);
        }
    }

    @Test(expected = HttpCodeException.class)
    public void testErrorHandler() throws Exception {
        client.getOperation("-");
    }

    @Test(expected = HttpCodeException.class)
    public void testBadAuth() throws Exception {
        client = createRestClient("some_user", "some_expired_token");
        client.getDiskInfo();
    }
}