package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.remote.exceptions.DuplicateFolderException;
import ru.yandex.disk.remote.exceptions.IntermediateFolderNotExistException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;

import java.io.IOException;

public class MakeFolderMethodTest extends WebdavClientMethodTestCase {

    @Override
    protected void prepareGoodResponse() throws Exception {
        prepareToReturnResponse(201);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void invokeMethod() throws RemoteExecutionException, IOException {
        client.makeFolder("/disk/Папка А");
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("MKCOL", "/disk/%D0%9F%D0%B0%D0%BF%D0%BA%D0%B0%20%D0%90", null, request);
    }

    @Test
    public void testDuplicateFolderException() throws Exception {
        prepareToReturnContent(405, "folder already exists");
        try {
            invokeMethod();
            fail("expect DuplicateFolderException");
        } catch (final DuplicateFolderException e) {
            //ok
        }
    }

    @Test
    public void testIntermediateFolderNotExistsException() throws Exception {
        prepareToReturnContent(409, "folder already exists");
        try {
            invokeMethod();
            fail("expect IntermediateFolderNotExistException");
        } catch (final IntermediateFolderNotExistException e) {
            //ok
        }
    }

}