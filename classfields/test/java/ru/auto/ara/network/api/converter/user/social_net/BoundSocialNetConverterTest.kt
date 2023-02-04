package ru.auto.ara.network.api.converter.user.social_net

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.SocialNet
import ru.auto.data.model.network.scala.NWProvider
import ru.auto.data.model.network.scala.NWUserSocialProfile
import ru.auto.data.model.network.scala.converter.UserSocialProfileConverter
import kotlin.test.assertEquals

@RunWith(AllureRunner::class)
class BoundSocialNetConverterTest {

    private fun checkCorrectConversion(
        nwSource: NWUserSocialProfile,
        resultSocialNet: SocialNet,
    ) {
        val converted = UserSocialProfileConverter.fromNetworkNW(nwSource)
        assertEquals(converted?.socialNet, resultSocialNet)
        assertEquals(converted?.added, nwSource.added)
        assertEquals(converted?.socialUserId, nwSource.social_user_id)
    }

    private fun checkNullConversion(
        nwSource: NWUserSocialProfile,
    ) {
        val converted = UserSocialProfileConverter.fromNetworkNW(nwSource)
        assertEquals(converted, null)
    }

    @Test
    fun `should convert yandex`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.YANDEX,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.YANDEX
    )


    @Test
    fun `should convert vk`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.VK,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.VK
    )

    @Test
    fun `should convert twitter`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.TWITTER,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.TWITTER
    )

    @Test
    fun `should convert ok`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.OK,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.OK
    )

    @Test
    fun `should convert google`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.GOOGLE,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.GOOGLE
    )

    @Test
    fun `should convert mailru`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.MAILRU,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.MAILRU
    )

    @Test
    fun `should convert mosru`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.MOSRU,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.MOSRU
    )

    @Test
    fun `should convert gosuslugi`() = checkCorrectConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.GOSUSLUGI,
            social_user_id = "12345"
        ),
        resultSocialNet = SocialNet.GOSUSLUGI
    )

    @Test
    fun `should convert facebook as null`() = checkNullConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.FACEBOOK,
            social_user_id = "12345"
        ),
    )

    @Test
    fun `should convert null provider as null`() = checkNullConversion(
        nwSource = NWUserSocialProfile(
            provider = null,
            social_user_id = "12345"
        ),
    )

    @Test
    fun `should convert null user id as null`() = checkNullConversion(
        nwSource = NWUserSocialProfile(
            provider = NWProvider.GOSUSLUGI,
            social_user_id = null,
        ),
    )
}
