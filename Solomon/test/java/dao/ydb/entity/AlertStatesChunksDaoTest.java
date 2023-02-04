package ru.yandex.solomon.alert.dao.ydb.entity;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.UnsafeByteOperations;
import io.grpc.Status;
import org.junit.Test;

import ru.yandex.devtools.test.annotations.YaIgnore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public abstract class AlertStatesChunksDaoTest {

    protected abstract AlertStatesChunksDao getDao();

    @Test
    public void uploadOneDownloadOne() {
        getDao().createSchemaForTests().join();
        var source = randomBytes(42);
        upload("junk", "test", source);
        var result = download("junk", "test", 1);
        assertArrayEquals(source, result);
    }

    @Test
    public void uploadHugeDownloadHuge() {
        getDao().createSchemaForTests().join();
        var source = randomBytes(ThreadLocalRandom.current().nextInt(5 << 20, 50 << 20));
        var chunks = upload("junk", "huge", source);
        var result = download("junk", "huge", chunks);
        assertArrayEquals(source, result);
    }

    @Test
    public void uploadDifferentFilesIntoOneProject() {
        getDao().createSchemaForTests().join();
        var one = randomBytes(42);
        var two = randomBytes(1 << 20);
        var three = randomBytes(5 << 20);
        var four = randomBytes(10 << 20);

        assertEquals(1, upload("junk", "one", one));
        assertEquals(1, upload("junk", "two", two));
        assertEquals(2, upload("junk", "three", three));
        assertEquals(3, upload("junk", "four", four));

        assertArrayEquals(one, download("junk", "one", 1));
        assertArrayEquals(two, download("junk", "two", 1));
        assertArrayEquals(three, download("junk", "three", 2));
        assertArrayEquals(four, download("junk", "four", 3));
    }

    @Test
    public void chunkCorrupted() {
        getDao().createSchemaForTests().join();
        var source = randomBytes(42);
        upload("junk", "test", source);
        try {
            download("junk", "test", 5);
            fail("expected 5 chunks, but read only one, it's means part of chunks was lost");
        } catch (CompletionException e) {
            var cause = e.getCause();
            var status = Status.fromThrowable(cause);
            assertEquals(Status.Code.DATA_LOSS, status.getCode());
        }
    }

    @Test
    public void crossProjectChunks() {
        getDao().createSchemaForTests().join();
        var one = randomBytes(42);
        var two = randomBytes(10 << 20);

        assertEquals(1, upload("solomon", "state", one));
        assertEquals(3, upload("kikimr", "state", two));

        assertArrayEquals(one, download("solomon", "state", 1));
        assertArrayEquals(two, download("kikimr", "state", 3));
    }

    @Test
    public void deleteFile() {
        getDao().createSchemaForTests().join();
        var one = randomBytes(42);
        var two = randomBytes(10 << 20);
        var three = randomBytes(1 << 20);

        assertEquals(1, upload("solomon", "state", one));
        assertEquals(3, upload("kikimr", "state", two));
        assertEquals(1, upload("kikimr", "three", three));

        getDao().deleteFileChunks("kikimr", "state").join();
        assertArrayEquals(one, download("solomon", "state", 1));
        assertAbsentChunks("kikimr", "state");
        assertArrayEquals(three, download("kikimr", "three", 1));
    }

    @Test
    public void deleteProject() {
        getDao().createSchemaForTests().join();
        var one = randomBytes(42);
        var two = randomBytes(10 << 20);
        var three = randomBytes(1 << 20);

        assertEquals(1, upload("solomon", "state", one));
        assertEquals(3, upload("kikimr", "state", two));
        assertEquals(1, upload("kikimr", "three", three));

        getDao().deleteProject("kikimr").join();
        assertArrayEquals(one, download("solomon", "state", 1));
        assertAbsentChunks("kikimr", "state");
        assertAbsentChunks("kikimr", "three");
    }

    private void assertAbsentChunks(String projectId, String fileId) {
        try {
            assertArrayEquals(new byte[0], download(projectId, fileId, 0));
        } catch (CompletionException e) {
            var cause = e.getCause();
            var status = Status.fromThrowable(cause);
            assertEquals(status.toString(), Status.Code.DATA_LOSS, status.getCode());
        }
    }

    private int upload(String projectId, String fileId, byte[] content) {
        return upload(getDao(), projectId, fileId, content);
    }

    private byte[] download(String projectId, String fileId, int expectedSize) {
        return download(getDao(), projectId, fileId, expectedSize);
    }

    protected int upload(AlertStatesChunksDao dao, String projectId, String fileId, byte[] content) {
        return dao.uploadChunks(projectId, fileId, UnsafeByteOperations.unsafeWrap(content)).join();
    }

    protected byte[] download(AlertStatesChunksDao dao, String projectId, String fileId, int expectedSize) {
        var result = dao.downloadChunks(projectId, fileId, expectedSize).join();
        return result.toByteArray();
    }

    protected byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
