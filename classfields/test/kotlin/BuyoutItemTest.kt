package ru.auto.feature.burger

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.burger.presintation.Burger
import ru.auto.feature.burger.ui.BurgerCardVM
import ru.auto.feature.burger.ui.BurgerMenuItem
import ru.auto.feature.burger.ui.BurgerTextVM
import ru.auto.feature.burger.ui.BurgerVMFactory
import ru.auto.test.tea.TeaTestFeature

@RunWith(AllureRunner::class)
class BuyoutItemTest {

    @Test
    fun `should not show buyout item when buyout not available`() {
        val feature = prepareFeature()
        assertThat(feature.currentState.items.buyoutItem).isNull()
    }

    @Test
    fun `should have buyout item when buyout is available`() {
        val feature = prepareFeature()

        step("when buyout item is available") {
            feature.accept(Burger.Msg.OnBuyoutAvailable)
        }

        step("should have it in state to build vm") {
            assertThat(feature.currentState.items.buyoutItem).isNotNull
        }
    }

    @Test
    fun `should open buyout webview when buyout item clicked`() {
        val feature = prepareFeature()

        val buyoutMenuItem = BurgerMenuItem.Buyout
        step("when buyout item clicked") {
            feature.accept(Burger.Msg.OnItemClicked(BurgerMenuItem.Buyout))
        }

        step("should open webview and log") {
            assertThat(feature.latestEffects).containsExactly(
                Burger.Eff.OpenMenuItem(buyoutMenuItem),
                Burger.Eff.ReportMenuItemOpened(buyoutMenuItem)
            )
        }
    }

    @Test
    fun `should not build buyout item vm when it is not available`() {
        val vmFactory = BurgerVMFactory()

        val items = vmFactory.buildVM(Burger.initialState()).items
        assertThat(items).extracting { (it as? BurgerCardVM)?.item }.doesNotContain(BurgerMenuItem.Buyout)
    }

    @Test
    fun `should build buyout item vm as last card item`() {
        val vmFactory = BurgerVMFactory()

        val state = Burger.initialState()
        val items = vmFactory.buildVM(state.copy(items = state.items.copy(buyoutItem = BurgerMenuItem.Buyout))).items
        val lastCardItem = items.last { it is BurgerCardVM } as? BurgerCardVM
        assertThat(lastCardItem?.item).isEqualTo(BurgerMenuItem.Buyout)
    }

    @Test
    fun `should not merge card and text vms`() {
        val vmFactory = BurgerVMFactory()

        val state = Burger.initialState()
        val items = vmFactory.buildVM(state.copy(items = state.items.copy(buyoutItem = BurgerMenuItem.Buyout))).items
        val lastCardItem = items.last { it is BurgerCardVM } as BurgerCardVM
        val lastCardItemPosition = items.lastIndexOf(lastCardItem) + 1
        assertThat(items.take(lastCardItemPosition)).allMatch { it !is BurgerTextVM }
        assertThat(items.takeLast(items.size - lastCardItemPosition)).allMatch { it !is BurgerCardVM }
    }

    companion object {
        private fun prepareFeature() = TeaTestFeature(
            initialState = Burger.initialState(),
            reducer = Burger::reducer,
        )
    }


}
