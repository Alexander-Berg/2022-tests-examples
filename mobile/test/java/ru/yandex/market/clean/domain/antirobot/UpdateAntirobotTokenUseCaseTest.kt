package ru.yandex.market.clean.domain.antirobot

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.antirobot.AntirobotPersistentRepository
import ru.yandex.market.clean.data.repository.antirobot.AntirobotRemoteRepository
import ru.yandex.market.clean.data.repository.antirobot.SafetyNetAttestationRepository
import ru.yandex.market.clean.domain.usecase.health.facades.AntirobotHealthFacade
import ru.yandex.market.domain.auth.model.Uuid
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase
import ru.yandex.market.mockResult

class UpdateAntirobotTokenUseCaseTest {

    private val getUuidUseCase = mock<GetUuidUseCase>()
    private val safetyNetAttestationRepository = mock<SafetyNetAttestationRepository>()
    private val antirobotPersistentRepository = mock<AntirobotPersistentRepository>()
    private val antirobotRemoteRepository = mock<AntirobotRemoteRepository>()
    private val antirobotHealthFacade = mock<AntirobotHealthFacade>()

    private val useCase = UpdateAntirobotTokenUseCase(
        getUuidUseCase = getUuidUseCase,
        safetyNetAttestationRepository = safetyNetAttestationRepository,
        antirobotPersistentRepository = antirobotPersistentRepository,
        antirobotRemoteRepository = antirobotRemoteRepository,
        antirobotHealthFacade = antirobotHealthFacade,
    )

    @Before
    fun setUp() {
        getUuidUseCase.getUuid().mockResult(uuidSingle())
    }

    @Test
    fun `Requests all nonce and attestation token, load and save jws token to preferences`() {
        whenever(antirobotRemoteRepository.getNonce(UUID.value))
            .thenReturn(nonceSingle())

        whenever(safetyNetAttestationRepository.getAttestationToken(NONCE))
            .thenReturn(attestationTokenSingle())

        whenever(antirobotRemoteRepository.getJwsToken(NONCE, ATTESTATION_TOKEN))
            .thenReturn(actualJwsTokenSingle())

        whenever(antirobotPersistentRepository.saveJwsToken(JWS_TOKEN))
            .thenReturn(Completable.complete())

        val subscriber = useCase.execute().test()

        verify(antirobotRemoteRepository).getNonce(UUID.value)
        verify(safetyNetAttestationRepository).getAttestationToken(NONCE)
        verify(antirobotRemoteRepository).getJwsToken(NONCE, ATTESTATION_TOKEN)
        verify(antirobotPersistentRepository).saveJwsToken(JWS_TOKEN)
        verify(antirobotHealthFacade).sendJwsUpdateSuccessEvent(JWS_TOKEN)

        subscriber
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `Completes with error when failed to get nonce`() {
        whenever(antirobotRemoteRepository.getNonce(UUID.value))
            .thenReturn(errorSingle())

        val subscriber = useCase.execute().test()

        verify(antirobotRemoteRepository).getNonce(UUID.value)
        verify(safetyNetAttestationRepository, never()).getAttestationToken(NONCE)
        verify(antirobotRemoteRepository, never()).getJwsToken(NONCE, ATTESTATION_TOKEN)
        verify(antirobotPersistentRepository, never()).saveJwsToken(JWS_TOKEN)
        verify(antirobotHealthFacade, never()).sendJwsUpdateSuccessEvent(JWS_TOKEN)

        subscriber
            .assertError(Exception::class.java)
    }

    @Test
    fun `Completes with error when failed to get attestation token`() {
        whenever(antirobotRemoteRepository.getNonce(UUID.value))
            .thenReturn(nonceSingle())

        whenever(safetyNetAttestationRepository.getAttestationToken(NONCE))
            .thenReturn(errorSingle())

        val subscriber = useCase.execute().test()

        verify(antirobotRemoteRepository).getNonce(UUID.value)
        verify(safetyNetAttestationRepository).getAttestationToken(NONCE)
        verify(antirobotRemoteRepository, never()).getJwsToken(NONCE, ATTESTATION_TOKEN)
        verify(antirobotPersistentRepository, never()).saveJwsToken(JWS_TOKEN)
        verify(antirobotHealthFacade, never()).sendJwsUpdateSuccessEvent(JWS_TOKEN)

        subscriber
            .assertError(Exception::class.java)
    }

    @Test
    fun `Completes with error when failed to get jws token`() {
        whenever(antirobotRemoteRepository.getNonce(UUID.value))
            .thenReturn(nonceSingle())

        whenever(safetyNetAttestationRepository.getAttestationToken(NONCE))
            .thenReturn(attestationTokenSingle())

        whenever(antirobotRemoteRepository.getJwsToken(NONCE, ATTESTATION_TOKEN))
            .thenReturn(errorSingle())

        val subscriber = useCase.execute().test()

        verify(antirobotRemoteRepository).getNonce(UUID.value)
        verify(safetyNetAttestationRepository).getAttestationToken(NONCE)
        verify(antirobotRemoteRepository).getJwsToken(NONCE, ATTESTATION_TOKEN)
        verify(antirobotPersistentRepository, never()).saveJwsToken(JWS_TOKEN)
        verify(antirobotHealthFacade, never()).sendJwsUpdateSuccessEvent(JWS_TOKEN)

        subscriber
            .assertError(Exception::class.java)
    }

    private fun uuidSingle() = Single.just(UUID)

    private fun actualJwsTokenSingle() = Single.just(JWS_TOKEN)

    private fun nonceSingle() = Single.just(NONCE)

    private fun attestationTokenSingle() = Single.just(ATTESTATION_TOKEN)

    private fun <T> errorSingle() = Single.error<T>(Exception("Unexpected exception"))

    companion object {
        private val UUID = Uuid("some-uuid")
        private const val JWS_TOKEN = "some-jws-token"
        private const val NONCE = "some-nonce"
        private const val ATTESTATION_TOKEN = "some-attestation-token"
    }
}