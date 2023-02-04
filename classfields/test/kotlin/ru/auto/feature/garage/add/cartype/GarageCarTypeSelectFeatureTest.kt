package ru.auto.feature.garage.add.cartype

import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.feature.garage.add.cartype.ui.GarageCarTypeSelectVmFactory
import ru.auto.feature.garage.add.cartype.ui.GarageCarTypeSelectVmFactory.Companion.isGarageAddDisclaimerItem
import ru.auto.test.tea.TeaTestFeature
import ru.auto.testextension.kotlinFixtureDefault
import ru.auto.testextension.prepareParameter

@RunWith(AllureRunner::class)
class GarageCarTypeSelectFeatureTest {

    private val fixture = kotlinFixtureDefault()
    private val vmFactory = prepareParameter("vm factory", fixture<GarageCarTypeSelectVmFactory>())

    @Test
    fun `should not display banner if has cards in garage`() {
        val feature = prepareFeature()
        step("When has cards in garage") {
            feature.accept(GarageCarTypeSelect.Msg.GarageCardsInfoReceived(hasGarageCards = true))
        }
        step("It should not display disclaimer") {
            assertThat(feature.currentState.hasGarageCards).isTrue
            val vm = vmFactory.buildVm(feature.currentState)
            assertThat(vm).isNotEmpty
            assertThat(vm.none { it.isGarageAddDisclaimerItem() }).isTrue
        }
    }

    companion object {
        private fun prepareFeature() = TeaTestFeature(
            initialState = GarageCarTypeSelect.initialState(),
            reducer = GarageCarTypeSelectReducer::reduce,
        )
    }
}
