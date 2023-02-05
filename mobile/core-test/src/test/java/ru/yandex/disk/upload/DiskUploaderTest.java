package ru.yandex.disk.upload;

import android.content.ContentResolver;
import android.database.DatabaseUtils;
import android.net.Uri;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskContract.Queue;
import ru.yandex.disk.provider.DiskUploadQueueCursor;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.remote.exceptions.ConnectionException;
import ru.yandex.disk.remote.exceptions.DuplicateFolderException;
import ru.yandex.disk.remote.exceptions.IntermediateFolderNotExistException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;

import java.io.File;

import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.disk.sql.SQLVocabulary.CONTENT;
import static ru.yandex.disk.test.MoreMatchers.anyFile;
import static ru.yandex.disk.upload.DiskUploaderTestHelper.anyUploadListener;

public class DiskUploaderTest extends AndroidTestCase2 {

    DiskUploaderTestHelper helper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        helper = new DiskUploaderTestHelper(mContext);
    }

    @Test
    public void testUploadFileSuccess() throws Exception {
        String fileName = helper.makeFileOnSD("file").getAbsolutePath();
        String destination = "/disk";

        helper.startUploadFiles(true, destination, fileName);

        helper.verifyWebdavUploadFile(fileName, destination);

        helper.assertEmptyQueue();
    }

    @Test
    public void testUploadFileSuccessToDirectory() throws Exception {
        String fileName = helper.makeFileOnSD("file").getAbsolutePath();
        String destination = "/disk/A";

        helper.startUploadFiles(true, destination, fileName);

        helper.verifyMakeFolderNeverCalled();
        helper.verifyWebdavUploadFile(fileName, destination);

        helper.assertEmptyQueue();
    }

    @Test
    public void testUploadDirectorySuccess() throws Exception {
        FileTree tree = new FileTree("A");
        tree.root().content(file("notes.txt"));
        tree.createInFileSystem(helper.getTestRootDirectory());

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        helper.startUploadFiles(srcDirectory);
        helper.commandStarter.executeQueue(true);

        File file = new File(srcDirectory, "notes.txt");
        String destinationDirectory = "/disk" + "/A";

        helper.verifyMakeFolder(destinationDirectory);
        helper.verifyWebdavUploadFile(file.getPath(), destinationDirectory);

        helper.assertEmptyQueue();
    }

    @Test
    public void testWebdavException() throws Exception {
        testWebdavException(new RemoteExecutionException("test"));
    }

    @Test
    public void testServerWebdavException() throws Exception {
        testWebdavException(new RemoteExecutionException("test"));
    }

    private void testWebdavException(RemoteExecutionException e, int expectedUploadState) throws Exception {
        String fileName = helper.makeFileOnSD("file").getAbsolutePath();
        testWebdavException(fileName, e, expectedUploadState);
    }

    private void testWebdavException(String fileName, RemoteExecutionException e, int expectedUploadState) throws Exception {
        String destination = "/disk";

        helper.prepareWebdavThrowException(e);

        helper.startUploadFiles(true, destination, fileName);

        DiskUploadQueueCursor queue = helper.queryQueue();
        queue.moveToFirst();
        assertEquals(expectedUploadState, queue.getTransferState());
        queue.close();
    }

    @Test
    public void testIntermediateFolderNotExistExceptionOnFileUpload() throws Exception {
        FileTree.File file;
        FileTree tree = new FileTree("A");
        tree.root().content(directory("B").content(
                file = file("notes.txt")
        ));
        tree.createInFileSystem(helper.getTestRootDirectory());

        String srcFileName = helper.getTestRootDirectory() + file.getPath();
        WebdavClient webdav = helper.getWebdav();
        doThrow(new ConnectionException("test")).when(webdav).makeFolder(anyString());

        helper.expectRestartUploadTask();

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                DiskUploadQueueCursor queue = helper.queryQueue();
                assertEquals(1, queue.getCount());
                queue.close();
                throw new IntermediateFolderNotExistException("");
            }
        }).when(helper.getWebdav()).uploadFile(anyFile(), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());

        helper.startUploadFiles(true, "/disk/A/B", srcFileName);

        DiskUploadQueueCursor queue = helper.queryQueue();
        DatabaseUtils.dumpCursor(queue);
        assertEquals(3, queue.getCount());

        FileQueueItem notesUploadTask = queue.get(0);
        queue.moveToFirst();
        assertEquals(srcFileName, notesUploadTask.getSrcName());
        assertEquals("/disk/A/B", notesUploadTask.getDestDir());
        assertEquals(Queue.State.IN_QUEUE, notesUploadTask.getTransferState());

        FileQueueItem directoryAUploadTask = queue.get(1);
        assertEquals("A", directoryAUploadTask.getSrcName());
        assertTrue(directoryAUploadTask.isDir());
        assertEquals("/disk", directoryAUploadTask.getDestDir());
        assertEquals(Queue.State.IN_QUEUE, directoryAUploadTask.getTransferState());

        FileQueueItem directoryBUploadTask = queue.get(2);
        assertEquals("B", directoryBUploadTask.getSrcName());
        assertTrue(directoryBUploadTask.isDir());
        assertEquals("/disk/A", directoryBUploadTask.getDestDir());
        assertEquals(Queue.State.IN_QUEUE, directoryBUploadTask.getTransferState());
    }

    @Test
    public void testIntermediateFolderNotExistExceptionOnFileUploadAndFileInRootDirectory() throws Exception {
        String fileName = helper.makeFileOnSD("file").getAbsolutePath();

        WebdavClient webdav = helper.getWebdav();

        doAnswer(invocation -> {
            helper.commandStarter.setExecuteCommands(false);
            throw new IntermediateFolderNotExistException("test");
        }).when(webdav)
                .uploadFile(anyFile(), anyString(), nullable(String.class), anyString(),
                        anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                        anyUploadListener());

        helper.expectRestartUploadTask();

        helper.startUploadFiles(true, "/disk", fileName);

        DiskUploadQueueCursor queue = helper.queryQueue();
        DatabaseUtils.dumpCursor(queue);
        assertEquals(1, queue.getCount());

        {
            FileQueueItem notesUploadTask = queue.get(0);
            assertEquals(fileName, notesUploadTask.getSrcName());
            assertEquals("/disk", notesUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, notesUploadTask.getTransferState());
        }

        helper.resetMocks();

        helper.commandStarter.executeQueue(true);
        helper.commandStarter.setExecuteCommands(false);
        helper.restartUpload();

        helper.verifyMakeFolderNeverCalled();
    }

    @Test
    public void testQueueDirectory() throws Exception {
        FileTree tree = new FileTree("A");
        FileTree.File file;
        tree.root().content(
                directory("B").content(
                        directory("C").content(
                                file = file("notes.txt")
                        )
                )
        );
        tree.createInFileSystem(helper.getTestRootDirectory());

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        String srcFileName = helper.getTestRootDirectory() + file.getPath();
        helper.queueFileToUpload(srcDirectory);

        DiskUploadQueueCursor queue = helper.queryQueue();
        assertEquals(4, queue.getCount());
        {
            FileQueueItem notesUploadTask = queue.get(0);
            assertEquals(srcFileName, notesUploadTask.getSrcName());
            assertFalse(notesUploadTask.isDir());
            assertEquals("/disk/A/B/C", notesUploadTask.getDestDir());
        }
        {
            FileQueueItem directoryAUploadTask = queue.get(1);
            assertEquals("A", directoryAUploadTask.getSrcName());
            assertTrue(directoryAUploadTask.isDir());
            assertEquals("/disk", directoryAUploadTask.getDestDir());
        }
        {
            FileQueueItem directoryBUploadTask = queue.get(2);
            assertEquals("B", directoryBUploadTask.getSrcName());
            assertTrue(directoryBUploadTask.isDir());
            assertEquals("/disk/A", directoryBUploadTask.getDestDir());
        }

        {
            FileQueueItem directoryCUploadTask = queue.get(3);
            assertEquals("C", directoryCUploadTask.getSrcName());
            assertTrue(directoryCUploadTask.isDir());
            assertEquals("/disk/A/B", directoryCUploadTask.getDestDir());

        }
        queue.close();

    }

    @Test
    public void testWebdavExceptionOnDirectory() throws Exception {
        FileTree.File file;
        FileTree tree = new FileTree("A");
        tree.root().content(directory("B").content(
                file = file("notes.txt")
        ));
        tree.createInFileSystem(helper.getTestRootDirectory());

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        String srcFileName = helper.getTestRootDirectory() + file.getPath();
        WebdavClient webdav = helper.getWebdav();
        doThrow(new ConnectionException("test")).when(webdav).makeFolder(anyString());

        helper.startUploadFiles(srcDirectory);

        helper.verifyWebdavUploadFileNeverCalled();

        DiskUploadQueueCursor queue = helper.queryQueue();
        assertEquals(3, queue.getCount());

        DatabaseUtils.dumpCursor(queue);

        {
            FileQueueItem notesUploadTask = queue.get(0);
            assertEquals(srcFileName, notesUploadTask.getSrcName());
            assertEquals("/disk/A/B", notesUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, notesUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryAUploadTask = queue.get(1);
            assertEquals("A", directoryAUploadTask.getSrcName());
            assertTrue(directoryAUploadTask.isDir());
            assertEquals("/disk", directoryAUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryAUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryBUploadTask = queue.get(2);
            assertEquals("B", directoryBUploadTask.getSrcName());
            assertTrue(directoryBUploadTask.isDir());
            assertEquals("/disk/A", directoryBUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryBUploadTask.getTransferState());
        }
    }

    @Test
    public void testIntermediateFolderNotExistExceptionForDirectoryUpload() throws Exception {
        FileTree.File file;
        FileTree tree = new FileTree("A");
        tree.root().content(directory("B").content(
                file = file("notes.txt")
        ));
        tree.createInFileSystem(helper.getTestRootDirectory());

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        String srcFileName = helper.getTestRootDirectory() + file.getPath();
        String destinationRoot = "/disk";

        helper.expectRestartUploadTask();

        doAnswer(invocation -> {
            helper.commandStarter.setExecuteCommands(false);
            DiskUploadQueueCursor queue = helper.queryQueue();
            DatabaseUtils.dumpCursor(queue);
            assertEquals(3, queue.getCount());
            assertEquals(Queue.State.IN_QUEUE, queue.get(0).getTransferState());
            assertEquals(Queue.State.UPLOADED, queue.get(1).getTransferState());
            assertEquals(Queue.State.UPLOADED, queue.get(2).getTransferState());
            queue.close();
            throw new IntermediateFolderNotExistException("");
        }).when(helper.getWebdav()).uploadFile(anyFile(), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());

        //helper.prepareWebdavThrowException(new IntermediateFolderNotExistException(""));

        helper.startUploadFiles(destinationRoot, srcDirectory.getAbsolutePath());

        DiskUploadQueueCursor queue = helper.queryQueue();
        assertEquals(3, queue.getCount());

        DatabaseUtils.dumpCursor(queue);

        {
            FileQueueItem notesUploadTask = queue.get(0);
            assertEquals(srcFileName, notesUploadTask.getSrcName());
            assertEquals("/disk/A/B", notesUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, notesUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryAUploadTask = queue.get(1);
            assertEquals("A", directoryAUploadTask.getSrcName());
            assertTrue(directoryAUploadTask.isDir());
            assertEquals("/disk", directoryAUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryAUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryBUploadTask = queue.get(2);
            assertEquals("B", directoryBUploadTask.getSrcName());
            assertTrue(directoryBUploadTask.isDir());
            assertEquals("/disk/A", directoryBUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryBUploadTask.getTransferState());
        }
    }

    @Test
    public void testIntermediateFolderNotExistExceptionDuringMakeDir() throws Exception {
        FileTree.File file;
        FileTree tree = new FileTree("A");
        tree.root().content(directory("B").content(
                file = file("notes.txt")
        ));
        tree.createInFileSystem(helper.getTestRootDirectory());

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        String srcFileName = helper.getTestRootDirectory() + file.getPath();
        String destinationRoot = "/disk";

        WebdavClient webdav = helper.getWebdav();
        doThrow(new IntermediateFolderNotExistException("test")).when(webdav)
                .makeFolder("/disk/A/B");

        doThrow(new IntermediateFolderNotExistException("test")).when(webdav)
                .uploadFile(anyFile(), anyString(), nullable(String.class), anyString(),
                        anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                        anyUploadListener());

        helper.expectRestartUploadTask(2);

        helper.startUploadFiles(destinationRoot, srcDirectory.getAbsolutePath());

        DiskUploadQueueCursor queue = helper.queryQueue();
        assertEquals(3, queue.getCount());

        DatabaseUtils.dumpCursor(queue);

        {
            FileQueueItem notesUploadTask = queue.get(0);
            assertEquals(srcFileName, notesUploadTask.getSrcName());
            assertEquals("/disk/A/B", notesUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, notesUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryAUploadTask = queue.get(1);
            assertEquals("A", directoryAUploadTask.getSrcName());
            assertTrue(directoryAUploadTask.isDir());
            assertEquals("/disk", directoryAUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryAUploadTask.getTransferState());
        }
        {
            FileQueueItem directoryBUploadTask = queue.get(2);
            assertEquals("B", directoryBUploadTask.getSrcName());
            assertTrue(directoryBUploadTask.isDir());
            assertEquals("/disk/A", directoryBUploadTask.getDestDir());
            assertEquals(Queue.State.IN_QUEUE, directoryBUploadTask.getTransferState());
        }
    }

    private void testWebdavException(RemoteExecutionException e) throws Exception {
        testWebdavException(e, DiskContract.Queue.State.PAUSED);
    }

    @Test
    public void testMakeDirectoryThrowDuplicateFolderException() throws Exception {
        FileTree tree = new FileTree("A");
        tree.root().content(file("notes.txt"));
        tree.createInFileSystem(helper.getTestRootDirectory());

        String destinationDirectory = "/disk" + "/A";
        doThrow(new DuplicateFolderException("test"))
                .when(helper.getWebdav()).makeFolder(destinationDirectory);

        File srcDirectory = new File(helper.getTestRootDirectory(), "A");
        helper.startUploadFiles(srcDirectory);
        helper.commandStarter.executeQueue(true);

        helper.verifyMakeFolder(destinationDirectory);

        File file = new File(srcDirectory, "notes.txt");
        helper.verifyWebdavUploadFile(file.getPath(), destinationDirectory);

        helper.assertEmptyQueue();
    }

    @Test
    public void testSkipIncorrectQueueItems() throws Exception {
        SeclusiveContext context = helper.getContext();
        ContentResolver cr = context.getContentResolver();
        String user = helper.getActiveAccount().getUser();
        Uri queueForUser =
                Uri.parse(CONTENT + getMockContext().getApplicationInfo().packageName + ".minidisk"
                        + "/" + Queue.makeQueuePath(user, null));
        cr.insert(queueForUser, FileQueueItemSerializerHelper.createAddToQueueValues("", "/disk", true));

        doThrow(new IntermediateFolderNotExistException("test"))
                .when(helper.getWebdav()).makeFolder("/disk/");

        helper.restartUpload();

        helper.verifyMakeFolder("/disk/");

        helper.assertEmptyQueue();

    }

}
