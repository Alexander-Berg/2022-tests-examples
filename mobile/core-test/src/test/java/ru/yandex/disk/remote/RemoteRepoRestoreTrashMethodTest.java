package ru.yandex.disk.remote;

import org.junit.Test;

public class RemoteRepoRestoreTrashMethodTest extends BaseRemoteRepoMethodTest {

    @Test(expected = ru.yandex.disk.remote.exceptions.ConflictException.class)
    public void shouldThrowConflictExceptionOn409() throws Exception {
        fakeOkHttpInterceptor.addResponse(409);

        remoteRepo.restoreFromTrash("/A", "/A");
    }
}
