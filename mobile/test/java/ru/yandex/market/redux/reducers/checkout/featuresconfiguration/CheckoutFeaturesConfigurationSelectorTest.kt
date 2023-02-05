package ru.yandex.market.redux.reducers.checkout.featuresconfiguration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.base.redux.selector.useSelector
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.clean.domain.model.checkout.CheckoutFeaturesConfigurationState
import ru.yandex.market.clean.domain.model.checkout.checkoutFeaturesConfigurationStateTestInstance
import ru.yandex.market.redux.selectors.checkout.featuresconfiguration.selectFeaturesConfiguration
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState

class CheckoutFeaturesConfigurationSelectorTest {

    @Test
    fun `CheckoutFeatureConfiguration selector on empty state`() {
        val emptyState = AppState()
        val featureConfiguration = emptyState.useSelector(selectFeaturesConfiguration())

        assertThat(featureConfiguration).isEqualTo(CheckoutFeaturesConfigurationState.EMPTY)
    }

    @Test
    fun `CheckoutFeatureConfiguration selector on non-empty state`() {
        val featuresConfigurationState = checkoutFeaturesConfigurationStateTestInstance()
        val appState = AppState(
            CheckoutState(
                featuresConfigurationState = featuresConfigurationState.asSingleStateObject()
            )
        )
        val selectedFeaturesConfigurationState = appState.useSelector(selectFeaturesConfiguration())

        assertThat(selectedFeaturesConfigurationState).isEqualTo(featuresConfigurationState)
    }
}