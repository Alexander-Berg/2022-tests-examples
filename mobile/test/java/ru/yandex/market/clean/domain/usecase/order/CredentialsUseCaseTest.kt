package ru.yandex.market.clean.domain.usecase.order

import com.annimon.stream.Optional
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.TokenType
import ru.yandex.market.domain.auth.model.Credentials
import ru.yandex.market.domain.auth.usecase.GetMuidUseCase
import ru.yandex.market.domain.auth.model.MarketUid
import ru.yandex.market.domain.auth.usecase.CredentialsUseCase
import ru.yandex.market.domain.auth.usecase.GetAuthTokenUseCase

class CredentialsUseCaseTest {

    private val testAuthToken = AuthToken(TokenType.OAUTH, TEST_AUTH_TOKEN)
    private val testMuid = MarketUid(TEST_MARKET_UID)

    private val credentialsWithAuthToken = Credentials(
        authToken = testAuthToken,
        muid = null
    )

    private val credentialsWithoutAuthToken = Credentials(
        authToken = null,
        muid = testMuid
    )

    private val getAuthTokenUseCase = mock<GetAuthTokenUseCase>()
    private val getMuidUseCase = mock<GetMuidUseCase>()

    private val credentialsUseCase = CredentialsUseCase(
        getAuthTokenUseCase,
        getMuidUseCase
    )

    @Test
    fun `Get credentials when user is authorized`() {
        setHasAuthorization(true)
        setHasMuid(true)

        val testObserver = credentialsUseCase.getCredentials().test()

        testObserver
            .assertNoErrors()
            .assertValue(credentialsWithAuthToken)
    }

    @Test
    fun `Get credentials when user is not authorized`() {
        setHasAuthorization(false)
        setHasMuid(true)

        val testObserver = credentialsUseCase.getCredentials().test()

        testObserver
            .assertNoErrors()
            .assertValue(credentialsWithoutAuthToken)
    }

    @Test
    fun `Get credentials when user is not authorized and no muid for some reason`() {
        setHasAuthorization(false)
        setHasMuid(false)

        val testObserver = credentialsUseCase.getCredentials().test()

        testObserver
            .assertNoValues()
            .assertError(NoSuchElementException::class.java)
    }

    private fun setHasAuthorization(hasAuthorization: Boolean) {
        if (hasAuthorization) {
            whenever(getAuthTokenUseCase.getCurrentAccountAuthToken())
                .thenReturn(Single.just(Optional.of(testAuthToken)))
        } else {
            whenever(getAuthTokenUseCase.getCurrentAccountAuthToken())
                .thenReturn(Single.just(Optional.empty()))
        }
    }

    private fun setHasMuid(hasMuid: Boolean) {
        if (hasMuid) {
            whenever(getMuidUseCase.getMuid())
                .thenReturn(Single.just(Optional.of(testMuid)))
        } else {
            whenever(getMuidUseCase.getMuid())
                .thenReturn(Single.just(Optional.empty()))
        }
    }

    companion object {
        private const val TEST_AUTH_TOKEN = "123abc123"
        private const val TEST_MARKET_UID = "456rty456"
    }
}