package ru.yandex.disk.remote

import org.mockito.kotlin.whenever
import org.junit.Test
import org.robolectric.annotation.Config
import rx.observers.TestSubscriber

@Config(manifest = Config.NONE)
class RemoteRepoGetSettingsFromServerMethodTest: BaseRemoteRepoMethodTest() {
    @Test
    fun `should not fail on server error`() {
        fakeOkHttpInterceptor.addResponse(500)
        whenever(mockWebdavClient.settings).thenReturn(SettingsFromServer("", "","",""))

        val subscriber = TestSubscriber<Any>()
        remoteRepo.getSettingsFromServer().subscribe(subscriber)
        subscriber.assertNoErrors()
    }
}
