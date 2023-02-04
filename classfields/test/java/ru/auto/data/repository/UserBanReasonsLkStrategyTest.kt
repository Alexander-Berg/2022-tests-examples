package ru.auto.data.repository

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.data.model.user.DomainBan
import ru.auto.data.model.user.DomainBanReason
import ru.auto.data.model.user.UserBans
import ru.auto.feature.user.interactor.UserBanReasonsLkStrategy
import kotlin.test.assertEquals


@RunWith(AllureParametrizedRunner::class)
class UserBanReasonsLkStrategyTest(private val params: Parameters) {

    private val strategy = UserBanReasonsLkStrategy()

    @Test
    fun `check observe user sends event on start`() {
        val actual = strategy.getReasons(params.bans)
        val expected = params.expected
        assertEquals(params.expected, actual, "reasons '$actual' should be the same as '$expected'")
    }


    data class Parameters(
        val name: String,
        val bans: UserBans,
        val expected: List<String>
    )

    companion object {
        private val ignoredReasonIfOnlyOne = DomainBanReason(
            "USER_RESELLER",
            banText = "user reseller"
        )
        private val ignoredReasonIfOnlyOneLowerCase = DomainBanReason(
            "user_reseller",
            banText = "user reseller lower case"
        )
        private val reasonModel = DomainBanReason(
            "WRONG_MODEL",
            banText = "wrong model"
        )
        private val reasonModelDuplicate = DomainBanReason(
            "WRONG_MODEL2",
            banText = "wrong model"
        )
        private val reasonConfiguration = DomainBanReason(
            "WRONG_CONFIGURATION",
            banText = "configuration"
        )

        private val reasonWrongAd = DomainBanReason(
            "WRONG_AD_PARAMETERS",
            banText = "wrong ad"
        )

        private val reasonLowPriceEmpty = DomainBanReason(
            "LOW_PRICE",
            banText = ""
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{index} - {0}")
        fun data(): Collection<Array<out Any>> = listOf(
            arrayOf(
                Parameters(
                    name = "check show all reasons",
                    bans = UserBans(
                        bans = mapOf(
                            "CAR" to DomainBan(
                                enrichedReasons = listOf(
                                    reasonModel,
                                    reasonConfiguration,
                                    reasonWrongAd,
                                    ignoredReasonIfOnlyOne
                                )
                            )
                        )
                    ),
                    expected = listOf(
                        reasonModel,
                        reasonConfiguration,
                        reasonWrongAd,
                        ignoredReasonIfOnlyOne
                    ).map { it.banText }
                )
            ),
            arrayOf(
                Parameters(
                    name = "check don't show bans for ignored domains",
                    bans = UserBans(
                        bans = mapOf(
                            "REVIEWS" to DomainBan(enrichedReasons = listOf(reasonModel)),
                            "MESSAGES" to DomainBan(enrichedReasons = listOf(reasonConfiguration, reasonWrongAd)),
                            "AUTOPARTS" to DomainBan(enrichedReasons = listOf(reasonWrongAd)),
                            "REVIEWS_COMMENTS" to DomainBan(enrichedReasons = listOf(reasonConfiguration, reasonModel))
                        )
                    ),
                    expected = listOf()
                )
            ),
            arrayOf(
                Parameters(
                    name = "check filter ignored domains",
                    bans = UserBans(
                        bans = mapOf(
                            "REVIEWS" to DomainBan(enrichedReasons = listOf(reasonModel)),
                            "REVIEWS_COMMENTS" to DomainBan(enrichedReasons = listOf(reasonConfiguration, reasonModel)),
                            "MOTORCYCLE" to DomainBan(enrichedReasons = listOf(reasonConfiguration)),
                            "AUTO" to DomainBan(enrichedReasons = listOf(reasonWrongAd))

                        )
                    ),
                    expected = listOf(reasonConfiguration.banText, reasonWrongAd.banText)
                )
            ),
            arrayOf(
                Parameters(
                    name = "check don't show bans if only USER_RESELLER reason",
                    bans = UserBans(
                        bans = mapOf(
                            "MOTORCYCLE" to DomainBan(
                                enrichedReasons = listOf(
                                    ignoredReasonIfOnlyOne, ignoredReasonIfOnlyOneLowerCase
                                )
                            ),
                            "AUTO" to DomainBan(
                                enrichedReasons = listOf(
                                    ignoredReasonIfOnlyOne
                                )
                            )
                        )
                    ),
                    expected = listOf()
                )
            ),
            arrayOf(
                Parameters(
                    name = "check duplicates shown as one reason",
                    bans = UserBans(
                        bans = mapOf(
                            "MOTORCYCLE" to DomainBan(
                                enrichedReasons = listOf(
                                    reasonModel, reasonModelDuplicate, reasonLowPriceEmpty
                                )
                            ),
                            "AUTO" to DomainBan(
                                enrichedReasons = listOf(
                                    reasonModel, reasonModelDuplicate
                                )
                            )
                        )
                    ),
                    expected = listOf(reasonModel.banText)
                )
            )
        )
    }

}
