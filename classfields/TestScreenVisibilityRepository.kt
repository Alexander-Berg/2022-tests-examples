package ru.auto.ara.core

import ru.auto.data.model.ScreenVisibility
import ru.auto.data.repository.IScreenVisibilityRepository
import ru.auto.data.util.TEST_IS_APP_RUNNING
import rx.Observable

class TestScreenVisibilityRepository : IScreenVisibilityRepository {

    override fun isAppForeground() = true

    override fun isAppRunning() = TEST_IS_APP_RUNNING

    override fun observeAppForeground(): Observable<Boolean> = Observable.just(true)

    override fun getMessagesScreenVisibility(
        dialogId: String
    ): Observable<ScreenVisibility> = Observable.just(ScreenVisibility.VISIBLE)

    override fun getCurrentVisibleDialogId(): Observable<String?> = Observable.just("")
}
