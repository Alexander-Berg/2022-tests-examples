/*
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2019. Yandex, LLC. All rights reserved.
 *
 * Author: Alexander Skvortsov <askvortsov@yandex-team.ru>
 */

package ru.yandex.searchplugin.taxi.configuration.kit

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(JUnit4::class)
class StartupFlowImplTest {
    private val fetcher = mock<StartupFetcher>()
    private val logger = mock<StartupFlowLogger>()
    private var events = arrayListOf<Event>()
    private var oAuthToken: String? = OAUTH_TOKEN
    private var pendingRequests = mutableListOf<PendingRequest>()
    private val actions = Actions()
    private val me = StartupFlowImpl(fetcher, { oAuthToken }, logger, { it() }).apply {
        bindActions(actions)
    }

    @Test
    fun unbindActuallyUnbinds() {
        me.unbindActions(actions)

        me.start()

        assertTrue(events.isEmpty())
    }

    @Test
    fun onStartCancelsPreviousRequest() {
        mockRequestWithToken(OAUTH_TOKEN)
        me.start()
        me.start()
        pendingRequests.firstOrNull()?.requestToken?.let {
            verify(it).cancel()
        }
        assertEquals(2, pendingRequests.count())
    }

    @Test
    fun whenReceivedResponseForCanceledRequestDoesNotInvokeAnyAction() {
        mockRequestWithToken(OAUTH_TOKEN)
        me.start()
        me.start()

        assertEquals(pendingRequests.count(), 2)
        pendingRequests.firstOrNull()?.let {
            it.completion(it.requestToken, mock(), StartupFetcher.Result.Error())
        }

        assertEquals(listOf(Event.PresentLoadingScreen), events)
    }

    @Test
    fun whenOAuthTokenIsNullPerformsRequest() {
        oAuthToken = null

        val token = mock<StartupFetcher.RequestToken>()
        whenever(fetcher.fetchStartupResponse(anyOrNull(), any())).thenReturn(token)

        me.start()

        assertEquals(listOf(Event.PresentLoadingScreen), events)
        verify(fetcher).fetchStartupResponse(isNull(), any())
    }

    @Test
    fun whenOAuthTokenIsNotNullPerformsRequest() {
        val token = mock<StartupFetcher.RequestToken>()
        whenever(fetcher.fetchStartupResponse(any(), any())).thenReturn(token)

        me.start()

        assertEquals(listOf(Event.PresentLoadingScreen), events)
        verify(fetcher).fetchStartupResponse(eq(OAUTH_TOKEN), any())
    }

    @Test
    fun whenRequestSucceededAsksToPresentFeatureScreen() {
        val identity = Identity(
            "test_taxi_token",
            OAUTH_TOKEN
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api"
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.AUTHORIZED,
                params
            )
        )

        performCase(result, listOf(Event.PresentLoadingScreen, Event.PresentFeatureScreen(identity, params)))
    }

    @Test
    fun whenRequestSucceededWithoutParamsAsksToPresentUnknownError() {
        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.AUTHORIZED
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.PresentError(StartupErrorResponse(statusCode = 200)))
        )
    }

    @Test
    fun whenPhoneConfirmationIsRequiredAsksToConfirmPhoneNumber() {
        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.PHONE_CONFIRMATION_REQUIRED
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.ConfirmPhoneNumber)
        )
    }

    @Test
    fun whenPhoneConfirmationIsRequireAsksToPresentFeatureScreenWithLateLoginAllowed() {
        oAuthToken = null

        val identity = Identity(
            "test_taxi_token",
            null
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api",
            lateLoginAllowed = true
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.PHONE_CONFIRMATION_REQUIRED,
                params
            )
        )

        performCase(
            result,
            listOf(Event.PresentLoadingScreen, Event.PresentFeatureScreen(identity, params)),
            oAuthToken = null
        )
    }

    @Test
    fun whenPhoneConfirmationIsRequireAsksToConfirmPhoneNumberWithLateLoginAllowedAndForcedAuthorization() {
        oAuthToken = null

        val identity = Identity(
            "test_taxi_token",
            null
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api",
            lateLoginAllowed = true
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.PHONE_CONFIRMATION_REQUIRED,
                params
            )
        )

        performCase(
            result,
            listOf(Event.PresentLoadingScreen, Event.ConfirmPhoneNumber),
            oAuthToken = null,
            forceAuthorization = true
        )
    }

    @Test
    fun whenReceivedUnauthorizedStatusAsksToRefreshToken() {
        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.RefreshOAuthToken)
        )
    }

    @Test
    fun whenReceivedUnauthorizedErrorAsksToRefreshToken() {
        performCase(
            StartupFetcher.Result.Error(
                StartupErrorResponse(
                    statusCode = 401,
                    StartupErrorResponseJson(StartupFlowImpl.UNAUTHORIZED_ERROR_CODE, "")
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.RefreshOAuthToken)
        )
    }

    @Test
    fun whenReceivedOtherErrorAsksToPresentHumanReadableError() {
        val text = "test_error_text"
        val errorCode = "other_error"
        performCase(
            StartupFetcher.Result.Error(
                StartupErrorResponse(
                    statusCode = 500,
                    StartupErrorResponseJson(message = text, code = errorCode)
                )
            ),
            listOf(
                Event.PresentLoadingScreen,
                Event.PresentError(
                    StartupErrorResponse(
                        statusCode = 500,
                        StartupErrorResponseJson(message = text, code = errorCode)
                    )
                )
            )
        )
    }

    @Test
    fun whenRequestSucceededUsesItOnNextStart() {
        val identity = Identity(
            "test_taxi_token",
            OAUTH_TOKEN
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api"
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.AUTHORIZED,
                params
            )
        )

        mockRequestWithToken(identity.oAuthToken)
        me.start()
        fulfillPendingRequests(listOf(result))
        events.clear()

        clearInvocations(fetcher)
        me.start()

        verifyNoMoreInteractions(fetcher)
        assertEquals(listOf(Event.PresentFeatureScreen(identity, params)), events)
    }

    @Test
    fun whenRequestSucceededAsksToPresentAuthScreenWithNullOauthToken() {
        oAuthToken = null
        val identity = Identity(
            "test_taxi_token",
            null
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api"
        )

        performCase(
            StartupFetcher.Result.Success(
                identity.taxiUserId,
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED,
                    params
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.PresentAuthScreen),
            null
        )
    }

    @Test
    fun whenRequestSucceededWithParamsAsksToRefreshOAuthToken() {
        val identity = Identity(
            "test_taxi_token",
            OAUTH_TOKEN
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api"
        )

        performCase(
            StartupFetcher.Result.Success(
                identity.taxiUserId,
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED,
                    params
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.RefreshOAuthToken)
        )
    }

    @Test
    fun whenRequestSucceededAsksToPresentFeatureScreenWithNullOauthTokenWithLateLoginAllowed() {
        oAuthToken = null
        val identity = Identity(
            "test_taxi_token",
            null
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api",
            lateLoginAllowed = true
        )

        performCase(
            StartupFetcher.Result.Success(
                identity.taxiUserId,
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED,
                    params
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.PresentFeatureScreen(identity, params)),
            null
        )
    }

    @Test
    fun whenRequestSucceededWithoutParamsAsksToPresentAuthErrorWithNullOauthToken() {
        oAuthToken = null
        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.AuthError),
            null
        )
    }

    @Test
    fun whenReceivedOtherErrorAsksToPresentHumanReadableErrorWithNullOauthToken() {
        oAuthToken = null
        val text = "test_error_text"
        val errorCode = "other_error"
        performCase(
            StartupFetcher.Result.Error(
                StartupErrorResponse(
                    statusCode = 500,
                    StartupErrorResponseJson(code = errorCode, message = text)
                )
            ),
            listOf(
                Event.PresentLoadingScreen,
                Event.PresentError(
                    StartupErrorResponse(
                        statusCode = 500,
                        StartupErrorResponseJson(code = errorCode, message = text)
                    )
                )
            ),
            null
        )
    }

    @Test
    fun whenRequestSucceededUsesItOnNextStartWithNullOauthTokenWithLateLoginAllowed() {
        oAuthToken = null
        val identity = Identity(
            "test_taxi_token",
            null
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api",
            lateLoginAllowed = true
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.UNAUTHORIZED,
                params
            )
        )

        mockRequestWithToken(identity.oAuthToken)
        me.start()
        fulfillPendingRequests(listOf(result))
        events.clear()

        clearInvocations(fetcher)
        me.start()

        verifyNoMoreInteractions(fetcher)
        assertEquals(listOf(Event.PresentFeatureScreen(identity, params)), events)
    }

    @Test
    fun whenOAuthTokenChangedAfterRequestSucceedPerformsRequestAgain() {
        val identity = Identity(
            "test_taxi_token",
            OAUTH_TOKEN
        )
        val params = StartupResponseJson.ParametersResponseJson(
            StartupResponseJson.ParametersResponseJson.EatsResponseJson(
                courierMaxDistanceToFocus = 123,
                service = "test_service",
                url = "some_url"
            ),
            trackingApi = "test_api"
        )
        val result = StartupFetcher.Result.Success(
            identity.taxiUserId,
            StartupResponseJson(
                StartupResponseJson.AuthorizationStatus.AUTHORIZED,
                params
            )
        )

        mockRequestWithToken(identity.oAuthToken)
        me.start()
        fulfillPendingRequests(listOf(result))
        events.clear()

        val otherToken = "other_token"
        val newIdentity = Identity(
            identity.taxiUserId,
            otherToken
        )
        oAuthToken = otherToken

        performCase(
            result,
            listOf(Event.PresentLoadingScreen, Event.PresentFeatureScreen(newIdentity, params)),
            otherToken
        )
    }

    @Test
    fun whenOAuthTokenDidNotChangeAfterRefreshAndResponseIsUnauthorizedAsksToPresentAuthError() {
        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(
                    StartupResponseJson.AuthorizationStatus.UNAUTHORIZED
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.RefreshOAuthToken)
        )

        events.clear()

        performCase(
            StartupFetcher.Result.Success(
                "some_id",
                StartupResponseJson(StartupResponseJson.AuthorizationStatus.UNAUTHORIZED)
            ),
            listOf(Event.PresentLoadingScreen, Event.AuthError)
        )
    }

    @Test
    fun whenOAuthTokenDidNotChangeAfterRefreshAndReceivedUnauthorizedErrorAsksToPresentAuthError() {
        performCase(
            StartupFetcher.Result.Error(
                StartupErrorResponse(
                    statusCode = 401,
                    StartupErrorResponseJson(StartupFlowImpl.UNAUTHORIZED_ERROR_CODE, "")
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.RefreshOAuthToken)
        )

        events.clear()

        performCase(
            StartupFetcher.Result.Error(
                StartupErrorResponse(
                    statusCode = 401,
                    StartupErrorResponseJson(StartupFlowImpl.UNAUTHORIZED_ERROR_CODE, "")
                )
            ),
            listOf(Event.PresentLoadingScreen, Event.AuthError)
        )
    }

    private fun performCase(
        requestResult: StartupFetcher.Result,
        expectedEvents: List<Event>,
        oAuthToken: String? = OAUTH_TOKEN,
        showLoadingScreen: Boolean = true,
        forceAuthorization: Boolean = false
    ) {
        mockRequestWithToken(oAuthToken)
        me.start(showLoadingScreen, forceAuthorization)
        fulfillPendingRequests(listOf(requestResult))
        assertEquals(expectedEvents, events)
    }

    private fun mockRequestWithToken(oAuthToken: String?) {
        whenever(fetcher.fetchStartupResponse(eq(oAuthToken), any())).doAnswer {
            @Suppress("UNCHECKED_CAST")
            val completion = it.arguments[1]
                as ((StartupFetcher.RequestToken, StartupRequestBody, StartupFetcher.Result) -> Unit)
            val requestToken = mock<StartupFetcher.RequestToken>()
            pendingRequests.add(PendingRequest(requestToken, completion))
            requestToken
        }
    }

    private fun fulfillPendingRequests(results: List<StartupFetcher.Result>) {
        assertEquals(pendingRequests.count(), results.count())
        pendingRequests.zip(results).forEach {
            it.first.completion(it.first.requestToken, mock(), it.second)
        }
        pendingRequests.clear()
    }

    private fun addEvent(event: Event) {
        events.add(event)
    }

    private sealed class Event {
        data class PresentFeatureScreen(
            val identity: Identity,
            val params: StartupResponseJson.ParametersResponseJson
        ) : Event()

        data class PresentError(
            val errorResponse: StartupErrorResponse? = null,
            val throwable: Throwable? = null
        ) : Event()

        object PresentAuthScreen : Event()
        object RefreshOAuthToken : Event()
        object ConfirmPhoneNumber : Event()
        object PresentLoadingScreen : Event()
        object AuthError : Event()
    }

    private class PendingRequest(
        val requestToken: StartupFetcher.RequestToken,
        val completion: (StartupFetcher.RequestToken, StartupRequestBody, StartupFetcher.Result) -> Unit
    )

    private inner class Actions : StartupFlow.Actions {
        override fun presentFeatureScreen(
            identity: Identity,
            params: StartupResponseJson.ParametersResponseJson
        ) = addEvent(Event.PresentFeatureScreen(identity, params))

        override fun presentLoadingScreen() = addEvent(Event.PresentLoadingScreen)
        override fun presentWelcomeScreen() = addEvent(Event.PresentAuthScreen)
        override fun presentError(errorResponse: StartupErrorResponse?, throwable: Throwable?) =
            addEvent(Event.PresentError(errorResponse, throwable))
        override fun presentAuthError() = addEvent(Event.AuthError)
        override fun refreshOAuthToken() = addEvent(Event.RefreshOAuthToken)
        override fun confirmPhoneNumber() = addEvent(Event.ConfirmPhoneNumber)
    }

    private companion object {
        const val OAUTH_TOKEN = "initial_test_token"
    }
}
