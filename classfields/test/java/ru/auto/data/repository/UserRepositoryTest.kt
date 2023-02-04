package ru.auto.data.repository

import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.ara.network.api.error.nodeapi.AuthFailedException
import ru.auto.data.model.User
import ru.auto.data.model.network.scala.NWAutoruUserProfile
import ru.auto.data.model.network.scala.NWUser
import ru.auto.data.model.network.scala.NWUserEmail
import ru.auto.data.model.network.scala.NWUserPhone
import ru.auto.data.model.network.scala.NWUserProfile
import ru.auto.data.model.network.scala.NWUserResponse
import ru.auto.data.model.network.scala.converter.UserConverter
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.prefs.IReactivePrefsDelegate
import ru.auto.feature.user.repository.UserRepository
import rx.Completable
import rx.Single
import rx.observers.TestSubscriber
import kotlin.math.pow


@RunWith(AllureParametrizedRunner::class)
class UserRepositoryTest(private val params: Parameters) : RxTest() {

    @Test
    fun `check observe user sends event on start`() {
        val userRepository = UserRepository(
            scalaApi = { getScalaApi(params.userInNetwork) },
            prefs = getPrefs(params.userInPrefs),
            logError = {}
        )
        val subscriber = TestSubscriber<User>()
        userRepository.observeUser().subscribe(subscriber)

        val expected = getUser(params.userInPrefs)
        subscriber.assertValue(expected)
    }

    @Test
    fun `check observe user sends event and prefs updated after update`() {
        val prefsUserInfo = getUser(params.userInPrefs)
        val prefs = getPrefs(params.userInPrefs)
        val userRepository = UserRepository(
            scalaApi = { getScalaApi(params.userInNetwork) },
            prefs = prefs,
            logError = {}
        )

        val subscriber = TestSubscriber<User>()
        userRepository.observeUser().subscribe(subscriber)

        val expectedUser = getUser(params.updatedUser)
        val updateSubscriber = TestSubscriber<Any>()
        userRepository.updateUser(expectedUser).subscribe(updateSubscriber)
        updateSubscriber.assertCompleted()

        val expected = linkedSetOf(prefsUserInfo, expectedUser)
        subscriber.assertValues(*expected.toTypedArray())

        val expectedJson = expectedUser.toJson()
        verify(prefs, times(1)).saveString(any(), any())
        verify(prefs).saveString("cache_user", expectedJson)
    }

    @Test
    fun `check fetch user requests user from network, saves it to prefs and then triggers observe user with new value`() {
        val prefsUserInfo = getUser(params.userInPrefs)

        val api = getScalaApi(params.userInNetwork)
        val prefs = getPrefs(params.userInPrefs)
        val userRepository = UserRepository(
            scalaApi = { api },
            prefs = prefs,
            logError = {}
        )

        val subscriber = TestSubscriber<User>()
        userRepository.observeUser().subscribe(subscriber)

        val expectedUser = getUser(params.userInNetwork)
        val fetchSubscriber = TestSubscriber<Any>()
        userRepository.fetchUser().subscribe(fetchSubscriber)
        fetchSubscriber.assertCompleted()

        verify(api, times(1)).getCurrentUser()

        val expected = linkedSetOf(prefsUserInfo, expectedUser)
        subscriber.assertValues(*expected.toTypedArray())

        val expectedPrefsJson = expectedUser.toJson()
        verify(prefs, times(1)).saveString(any(), any())
        verify(prefs).saveString("cache_user", expectedPrefsJson)
    }

    @Test
    fun `check fetch user returns unauthorized when api returns AuthFailedException`() {
        val prefsUserInfo = getUser(params.userInPrefs)

        val api = getScalaApi(authorizedError = true)
        val prefs = getPrefs(params.userInPrefs)
        val userRepository = UserRepository(
            scalaApi = { api },
            prefs = prefs,
            logError = {}
        )

        val subscriber = TestSubscriber<User>()
        userRepository.observeUser().subscribe(subscriber)

        val expectedUser = User.Unauthorized
        val fetchSubscriber = TestSubscriber<Any>()
        userRepository.fetchUser().subscribe(fetchSubscriber)
        fetchSubscriber.assertValue(expectedUser)

        verify(api, times(1)).getCurrentUser()

        val expected = linkedSetOf(prefsUserInfo, expectedUser)
        subscriber.assertValues(*expected.toTypedArray())

        val expectedPrefsJson = expectedUser.toJson()
        verify(prefs, times(1)).saveString(any(), anyOrNull())
        verify(prefs).saveString("cache_user", expectedPrefsJson)
    }

    @Test
    fun `check fetch user throws error when api returns RuntimeException`() {
        val prefsUserInfo = getUser(params.userInPrefs)

        val api = getScalaApi(authorizedError = false)
        val prefs = getPrefs(params.userInPrefs)
        val userRepository = UserRepository(
            scalaApi = { api },
            prefs = prefs,
            logError = {}
        )

        val subscriber = TestSubscriber<User>()
        userRepository.observeUser().subscribe(subscriber)

        val fetchSubscriber = TestSubscriber<Any>()
        userRepository.fetchUser().subscribe(fetchSubscriber)
        fetchSubscriber.assertError(RUNTIME_EXCEPTION)

        verify(api, times(1)).getCurrentUser()

        subscriber.assertValues(prefsUserInfo)

        verify(prefs, times(0)).saveString(any(), any())
    }

    private fun getScalaApi(user: NWUser? = null, authorizedError: Boolean? = null): ScalaApi = mock<ScalaApi>().apply {
        val result = when (authorizedError) {
            true -> Single.error(AuthFailedException())
            false -> Single.error(RUNTIME_EXCEPTION)
            else -> Single.just(NWUserResponse(user))
        }
        whenever(getCurrentUser()).thenReturn(result)
    }

    private fun getPrefs(userState: NWUser?): IReactivePrefsDelegate = mock<IReactivePrefsDelegate>().apply {
        val user = getUser(userState)
        val json = Gson().toJson(user)
        whenever(getString("cache_user")).thenReturn(Single.just(json))
        whenever(saveString(eq("cache_user"), anyOrNull())).thenReturn(Completable.complete())
    }

    private fun User.toJson(): String? =
        when (this) {
            is User.Authorized -> Gson().toJson(this)
            is User.Unauthorized -> null
        }


    data class Parameters(
        val userInPrefs: NWUser?,
        val userInNetwork: NWUser?,
        val updatedUser: NWUser?
    )


    companion object {
        private val USER = NWUser(
            id = "ivashka99",
            profile = NWUserProfile(
                autoru = NWAutoruUserProfile(
                    first_name = "Ivanov Ivan Ivanovich",
                    driving_year = 99
                )
            ),
            emails = listOf(NWUserEmail("a@a.a")),
            phones = listOf(NWUserPhone("+79999999"))
        )

        private val USER2 = NWUser(
            id = "artyomka",
            profile = NWUserProfile(
                autoru = NWAutoruUserProfile(
                    first_name = "Artyom"
                )
            )
        )

        private val USER3 = NWUser(
            id = "ilya",
            profile = NWUserProfile(
                autoru = NWAutoruUserProfile(
                    first_name = "Ilya"
                )
            )
        )

        private val RUNTIME_EXCEPTION = RuntimeException("404")

        private val users = listOf(USER, USER2, USER3, USER, null, null)

        @JvmStatic
        @Parameterized.Parameters(name = "{index} - {0}")
        fun data(): Collection<Array<Any>> = (0 until users.size.toDouble().pow(3.0).toInt())
            .map { idx ->
                val first = idx % 3
                val second = (idx / users.size) % 3
                val third = (idx / users.size / users.size) % 3
                Parameters(
                    userInPrefs = users[first],
                    userInNetwork = users[second],
                    updatedUser = users[third]
                )
            }.map { arrayOf<Any>(it) }

        private fun getUser(user: NWUser?): User = user?.let(UserConverter::fromNetwork) ?: User.Unauthorized

    }
}
