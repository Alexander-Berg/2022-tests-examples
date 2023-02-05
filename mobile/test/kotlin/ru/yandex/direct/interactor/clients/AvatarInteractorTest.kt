// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.interactor.clients

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.repository.clients.Avatar
import ru.yandex.direct.repository.clients.AvatarLocalRepository
import java.net.URL

class AvatarInteractorTest {
    private lateinit var mLocalRepo: AvatarLocalRepository
    private lateinit var mInteractor: AvatarInteractor

    private val mScheduler = TestScheduler()
    private val mUrl1 = URL("https://yandex.ru/1")
    private val mUrl2 = URL("https://yandex.ru/2")
    private val mAvatar = Avatar.empty()

    @Before
    fun setup() {
        mLocalRepo = mock()
        mInteractor = AvatarInteractor(mLocalRepo, mock(), mScheduler, mScheduler)
    }

    @Test
    fun loadAvatar_shouldReturnCachedResult_ifLoadInProgress() {
        val first = mInteractor.loadAvatar(mUrl1)
        val second = mInteractor.loadAvatar(mUrl1)
        assertThat(first).isSameAs(second)
    }

    @Test
    fun loadAvatar_shouldReturnNewResult_ifRequestDifferentUrl() {
        val first = mInteractor.loadAvatar(mUrl1)
        val second = mInteractor.loadAvatar(mUrl2)
        assertThat(first).isNotSameAs(second)
    }

    @Test
    fun loadAvatar_shouldReturnNewResult_ifLoadIsDone() {
        mLocalRepo.stub { on { select(mUrl1) } doReturn mAvatar }
        mLocalRepo.stub { on { containsActualData(mUrl1) } doReturn true }

        val firstObserver = TestObserver.create<Avatar>()
        val first = mInteractor.loadAvatar(mUrl1)
        first.observeOn(mScheduler).subscribe(firstObserver)

        assertThat(mInteractor.inProgress().size == 1).isTrue()
        mScheduler.triggerActions()
        assertThat(mInteractor.inProgress().isEmpty()).isTrue()

        firstObserver.assertValues(mAvatar)

        val secondObserver = TestObserver.create<Avatar>()
        val second = mInteractor.loadAvatar(mUrl1)
        second.observeOn(mScheduler).subscribe(secondObserver)

        assertThat(mInteractor.inProgress().size == 1).isTrue()
        mScheduler.triggerActions()
        assertThat(mInteractor.inProgress().isEmpty()).isTrue()

        secondObserver.assertValues(mAvatar)

        assertThat(first).isNotSameAs(second)
    }

    @Test
    fun loadAvatar_shouldRemoveSingleFromMap_ifError() {
        mLocalRepo.stub { on { select(mUrl1) } doReturn mAvatar }

        val observer = TestObserver.create<Avatar>()
        val single = mInteractor.loadAvatar(mUrl1)
        single.observeOn(mScheduler).subscribe(observer)

        assertThat(mInteractor.inProgress().size == 1).isTrue()
        mScheduler.triggerActions()
        assertThat(mInteractor.inProgress().isEmpty()).isTrue()

        observer.assertTerminated()
        observer.assertNotComplete()
        observer.assertNoValues()
        observer.assertError(NullPointerException::class.java)
    }

    @Test
    fun loadAvatar_allSubscribersShouldReceiveResult_ifSubscribeToSameUrl() {
        mLocalRepo.stub { on { select(mUrl1) } doReturn mAvatar }
        mLocalRepo.stub { on { containsActualData(mUrl1) } doReturn true }
        val first = TestObserver.create<Avatar>()
        val second = TestObserver.create<Avatar>()
        mInteractor.loadAvatar(mUrl1).subscribe(first)
        mInteractor.loadAvatar(mUrl1).subscribe(second)
        mScheduler.triggerActions()
        first.assertValues(mAvatar)
        second.assertValues(mAvatar)
    }
}