package ru.auto.feature.garage.landing

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.garage.analyst.GarageAnalystSource
import ru.auto.feature.garage.core.ui.BlockType
import ru.auto.feature.garage.model.PartnerPromo
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.prepareParameter

@RunWith(AllureRunner::class)
class GarageLandingFeatureTest {

    private val fixture = kotlinFixtureDefault()
    private fun setupPromo(id: String = "promo_id", promoType: PartnerPromo.PromoType) = prepareParameter(
        promoType.name,
        fixture<PartnerPromo>().copy(
            id = id,
            type = promoType
        )
    )

    @Test
    fun `should open search on add to garage click when user is authorized`() {
        val feature = prepareFeature(isAuthorized = true)
        step("When user clicks on add to garage button") {
            feature.accept(GarageLanding.Msg.OnAddToGarageClicked)
        }
        step("It should open garage search screen") {
            assertThat(feature.latestEffects).containsExactly(GarageLanding.Eff.OpenSearch)
        }
    }

    @Test
    fun `should open search after user authorization on add to garage click when user is not authorized`() {
        val feature = prepareFeature(isAuthorized = false)
        step("When user clicks on add to garage button") {
            feature.accept(GarageLanding.Msg.OnAddToGarageClicked)
        }
        step("It should open login screen") {
            assertThat(feature.latestEffects).containsExactly(GarageLanding.Eff.ObserveUserLogin, GarageLanding.Eff.OpenLogin)
        }
        step("When user authorized") {
            feature.accept(GarageLanding.Msg.UserAuthorized)
        }
        step("It should open search screen") {
            assertThat(feature.latestEffects).containsExactly(GarageLanding.Eff.OpenSearch)
        }
    }

    @Test
    fun `should do nothing after dealer authorization on add to garage click when user not authorized`() {
        val feature = prepareFeature(isAuthorized = false)
        step("When user clicks on add to garage button") {
            feature.accept(GarageLanding.Msg.OnAddToGarageClicked)
        }
        step("It should open login screen") {
            assertThat(feature.latestEffects).containsExactly(GarageLanding.Eff.ObserveUserLogin, GarageLanding.Eff.OpenLogin)
        }
        step("When dealer authorized") {
            feature.accept(GarageLanding.Msg.DealerAuthorized)
        }
        step("It should open search screen") {
            assertThat(feature.latestEffects).isEmpty()
        }
    }

    @Test
    fun `should load and display garage promos when has only one page without promo ids`() {
        val feature = prepareFeature()
        val commonPromo = setupPromo(promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val superPromo = setupPromo(promoType = PartnerPromo.PromoType.SUPER_PROMO)
        val promos = listOf(superPromo, superPromo, commonPromo, commonPromo)

        step("When successful promos response") {
            feature.accept(
                GarageLanding.Msg.PartnerPromoLoaded(
                    partnerPromos = promos,
                    hasMorePromos = false,
                    promoIds = null
                )
            )
        }
        step("It should show promos") {
            assertThat(feature.currentState.partnerPromos)
                .allMatch { promo -> promo.id in promos.map { it.id } }
            assertThat(feature.currentState.hasMorePromos).isFalse
            assertThat(feature.latestEffects).isEmpty()
        }
    }

    @Test
    fun `should load and display garage promos when has several pages without promo ids`() {
        val feature = prepareFeature()
        val commonPromo = setupPromo(promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val superPromo = setupPromo(promoType = PartnerPromo.PromoType.SUPER_PROMO)
        val promos = listOf(superPromo, commonPromo)

        step("When successful promos response") {
            feature.accept(
                GarageLanding.Msg.PartnerPromoLoaded(
                    partnerPromos = promos,
                    hasMorePromos = true,
                    promoIds = null
                )
            )
        }
        step("It should show promos and set has more promos to true") {
            assertThat(feature.currentState.partnerPromos)
                .allMatch { promo -> promo.id in promos.map { it.id } }
            assertThat(feature.currentState.hasMorePromos).isTrue
            assertThat(feature.latestEffects).isEmpty()
        }
    }

    @Test
    fun `should scroll to super promo and open first promo when from deeplink and has equal promo ids`() {
        val source = GarageAnalystSource.DEEPLINK
        val deeplinkPromoId = "test_id"
        val feature = prepareFeature(
            source = source
        )
        val commonPromo = setupPromo(promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val superPromo = setupPromo(id = deeplinkPromoId, promoType = PartnerPromo.PromoType.SUPER_PROMO)
        val promos = listOf(superPromo, superPromo, commonPromo, commonPromo)

        step("When successful promos response") {
            feature.accept(
                GarageLanding.Msg.PartnerPromoLoaded(
                    partnerPromos = promos,
                    hasMorePromos = false,
                    promoIds = listOf(deeplinkPromoId)
                )
            )
        }
        step("It should scroll to super promo and open promo popup") {
            assertThat(feature.currentState.partnerPromos)
                .allMatch { promo -> promo.id in promos.map { it.id } }
            assertThat(feature.latestEffects).containsExactly(
                GarageLanding.Eff.LogShowLandingWithOpenedPromo(source, deeplinkPromoId),
                GarageLanding.Eff.ShowPromoPopup(superPromo),
                GarageLanding.Eff.ScrollToItem(
                    blockType = BlockType.SUPER_PROMO,
                    itemId = null,
                    delayInMillis = 300L
                )
            )
        }
    }

    @Test
    fun `should scroll to common promos and open first promo when from deeplink and has equal promo ids`() {
        val source = GarageAnalystSource.DEEPLINK
        val deeplinkPromoId = "test_id"
        val feature = prepareFeature(
            source = source
        )
        val commonPromo = setupPromo(id = deeplinkPromoId, promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val superPromo = setupPromo(promoType = PartnerPromo.PromoType.SUPER_PROMO)
        val promos = listOf(commonPromo, superPromo)

        step("When successful promos response") {
            feature.accept(
                GarageLanding.Msg.PartnerPromoLoaded(
                    partnerPromos = promos,
                    hasMorePromos = false,
                    promoIds = listOf(deeplinkPromoId)
                )
            )
        }
        step("It should scroll to common promos list and open promo popup") {
            assertThat(feature.currentState.partnerPromos)
                .allMatch { promo -> promo.id in promos.map { it.id } }
            assertThat(feature.latestEffects).containsExactly(
                GarageLanding.Eff.LogShowLandingWithOpenedPromo(source, deeplinkPromoId),
                GarageLanding.Eff.ShowPromoPopup(commonPromo),
                GarageLanding.Eff.ScrollToItem(
                    blockType = null,
                    itemId = "COMMON_PROMOS_ID",
                    delayInMillis = 300L
                )
            )
        }
    }

    @Test
    fun `should load and display garage promos when promo ids from deeplink not equal to landing promos`() {
        val source = GarageAnalystSource.DEEPLINK
        val deeplinkPromoId = "test_id"
        val feature = prepareFeature(
            source = source
        )
        val commonPromo = setupPromo(promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val superPromo = setupPromo(id = "another_promo_id", promoType = PartnerPromo.PromoType.SUPER_PROMO)
        val promos = listOf(superPromo, commonPromo)

        step("When successful promos response") {
            feature.accept(
                GarageLanding.Msg.PartnerPromoLoaded(
                    partnerPromos = promos,
                    hasMorePromos = false,
                    promoIds = listOf(deeplinkPromoId)
                )
            )
        }
        step("It should show promos without scrolling") {
            assertThat(feature.currentState.partnerPromos)
                .allMatch { promo -> promo.id in promos.map { it.id } }
            assertThat(feature.latestEffects).isEmpty()
        }
    }

    @Test
    fun `should display landing without partner promos`() {
        val feature = prepareFeature()

        step("When promos response failed") {
            feature.accept(GarageLanding.Msg.PartnerPromoFailed)
        }
        step("It should show landing without promos") {
            assertThat(feature.currentState.partnerPromos).isEmpty()
            assertThat(feature.currentState.hasMorePromos).isFalse
            assertThat(feature.latestEffects).isEmpty()
        }
    }

    @Test
    fun `should open promo popup when click on promo`() {
        val promoId = "promo_id"
        val commonPromo = setupPromo(id = promoId, promoType = PartnerPromo.PromoType.COMMON_PROMO)
        val partnerPromos = listOf(commonPromo)
        val feature = prepareFeature(partnerPromos = partnerPromos)

        step("When click on promo") {
            feature.accept(GarageLanding.Msg.OnPartnerPromoClicked(promoId))
        }
        step("It should log and open promo popup") {
            assertThat(feature.latestEffects).containsExactly(
                GarageLanding.Eff.ShowPromoPopup(partnerPromos.first()),
                GarageLanding.Eff.LogPromoClicked(promoId)
            )
        }
    }

    @Test
    fun `should open all promos screen when click on show all promos card`() {
        val feature = prepareFeature()

        step("When click on show all promos card") {
            feature.accept(GarageLanding.Msg.OnAllPromosCardClicked)
        }
        step("It should open all promos screen with organic source") {
            assertThat(feature.latestEffects).containsExactly(
                GarageLanding.Eff.OpenAllPromos(
                    promoIds = null,
                    source = GarageAnalystSource.ORGANIC
                )
            )
        }
    }

    @Test
    fun `should open burger menu on navigation click`() {
        val feature = prepareFeature()

        step("When click on button in navigation") {
            feature.accept(GarageLanding.Msg.OnNavigationClicked)
        }
        step("It should open burger menu") {
            assertThat(feature.latestEffects).containsExactly(
                GarageLanding.Eff.OpenBurgerMenuOnClick
            )
        }
    }

    @Test
    fun `should set garage promo button visibility to false when promo not visible`() {
        val feature = prepareFeature()

        step("When scroll landing under garage promo") {
            feature.accept(GarageLanding.Msg.OnGaragePromoButtonVisibleToUser(isVisible = false))
        }
        step("It should set garage promo button visibility to false") {
            assertThat(feature.currentState.isGaragePromoButtonVisibleToUser).isFalse
        }
    }

    @Test
    fun `should set garage promo button visibility to true when promo visible`() {
        val feature = prepareFeature()

        step("When scroll landing for showing garage promo") {
            feature.accept(GarageLanding.Msg.OnGaragePromoButtonVisibleToUser(isVisible = true))
        }
        step("It should set garage promo button visibility to true") {
            assertThat(feature.currentState.isGaragePromoButtonVisibleToUser).isTrue
        }
    }

    companion object {
        private fun prepareFeature(
            isAuthorized: Boolean = false,
            source: GarageAnalystSource = GarageAnalystSource.OTHER,
            partnerPromos: List<PartnerPromo> = emptyList()
        ) = TeaTestFeature(
            initialState = GarageLanding.State(
                isAuthorized = isAuthorized,
                partnerPromos = partnerPromos,
                analystState = GarageLanding.State.AnalystState(
                    isLandingWasShownToUser = false,
                    isFromDeeplink = false,
                    source = source
                )
            ),
            reducer = GarageLanding::reducer
        )
    }
}
