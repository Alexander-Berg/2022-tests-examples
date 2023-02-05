package ru.yandex.market.clean.presentation.feature.oneclick.store

import com.google.gson.Gson
import com.yandex.payment.sdk.core.data.BankName
import com.yandex.payment.sdk.model.data.PartnerInfo
import com.yandex.payment.sdk.model.data.PaymentOption
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.domain.payment.model.SelectedCardLimitation

@RunWith(Enclosed::class)
class OneClickDataStoreTest {

    @RunWith(Parameterized::class)
    class SelectedCardTest(
        testCaseDescription: String,
        private val savedCardPrefJson: String?,
        private val userCards: List<PaymentOption>?,
        private val limitation: SelectedCardLimitation,
        private val expected: SelectedCard
    ) {

        private val prefs = CommonPreferences(
            mock {
                on { getString("checkoutSavedSelectedCardStateV2", null) } doReturn savedCardPrefJson
            }
        )
        private val dataStore = OneClickDataStore(
            commonPreferences = prefs,
            gson = Gson(),
            oneClickAnalyticsSender = mock()
        )

        @Before
        fun setup() {
            if (userCards != null) {
                dataStore.setPaymentOptions(userCards).test().assertComplete()
            }
        }

        @Test
        fun getCard() {
            assertThat(
                dataStore.getSelectedCard(limitation)
            ).isEqualTo(
                expected
            )
        }

        companion object {

            private val MASTER_CARD = PaymentOption(
                id = "firstCard",
                account = "firstAccount",
                system = "mastercard",
                bankName = BankName.AlfaBank,
                familyInfo = null,
                partnerInfo = null
            )

            private val VISA = PaymentOption(
                id = "secondCard",
                account = "secondAccount",
                system = "visa",
                bankName = BankName.BankOfMoscow,
                familyInfo = null,
                partnerInfo = null
            )

            private val YANDEX_CARD = PaymentOption(
                id = "thirdCard",
                account = "thirdAccount",
                system = "mir",
                bankName = BankName.RaiffeisenBank,
                familyInfo = null,
                partnerInfo = PartnerInfo(isYabankCardOwner = true)
            )

            @Parameterized.Parameters(name = "{index}: {0}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(
                    "Список не загружен с сервера",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.CARD_SELECTED, "not_matter"),
                    null as List<PaymentOption>?,
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.SHOW_ALL_CARDS)
                ),
                arrayOf(
                    "Список загружен с сервера, но пустой",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.CARD_SELECTED, "not_matter"),
                    emptyList<PaymentOption>(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.NEW_CARD)
                ),
                arrayOf(
                    "Карта не выбрана, список не пустой",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.SHOW_ALL_CARDS, "not_matter"),
                    getAllCards(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.CARD_SELECTED, getAllCards().first())
                ),
                arrayOf(
                    "Карта выбрана, но ее нет в списке",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.CARD_SELECTED, "missed_card"),
                    getAllCards(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.CARD_SELECTED, getAllCards().first())
                ),
                arrayOf(
                    "Выбрано добавление новой карты",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.NEW_CARD, "not_matter"),
                    getAllCards(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.NEW_CARD)
                ),
                arrayOf(
                    "Выбнана Я.Карта, но на неё есть лимит, есть другие карты",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.CARD_SELECTED, YANDEX_CARD.id),
                    getAllCards(),
                    SelectedCardLimitation(limitations = listOf(SelectedCardLimitation.Limit.YANDEX_CARD)),
                    SelectedCard(SelectedCard.SelectedCardState.CARD_SELECTED, getAllCards().first())
                ),
                arrayOf(
                    "Выбнана Я.Карта, но на неё есть лимит, других карт нет",
                    getSelectedCardPrefJsonString(SelectedCard.SelectedCardState.CARD_SELECTED, YANDEX_CARD.id),
                    listOf(YANDEX_CARD),
                    SelectedCardLimitation(limitations = listOf(SelectedCardLimitation.Limit.YANDEX_CARD)),
                    SelectedCard(SelectedCard.SelectedCardState.NEW_CARD)
                ),
                arrayOf(
                    "Выбрана первая карта из списка",
                    getSelectedCardPrefJsonString(
                        SelectedCard.SelectedCardState.CARD_SELECTED,
                        getAllCards().first().id
                    ),
                    getAllCards(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.CARD_SELECTED, getAllCards().first())
                ),
                arrayOf(
                    "Выбрана вторая карта из списка",
                    getSelectedCardPrefJsonString(
                        SelectedCard.SelectedCardState.CARD_SELECTED,
                        getAllCards()[1].id
                    ),
                    getAllCards(),
                    SelectedCardLimitation.NO_LIMITS,
                    SelectedCard(SelectedCard.SelectedCardState.CARD_SELECTED, getAllCards()[1])
                )
            )

            private fun getSelectedCardPrefJsonString(state: SelectedCard.SelectedCardState, id: String): String {
                return """
                    {
                        "state": ${state.name},
                        "paymentOptionId": $id
                    }
                """.trimIndent()
            }

            private fun getAllCards(): List<PaymentOption> {
                return listOf(MASTER_CARD, VISA, YANDEX_CARD)
            }
        }
    }
}
