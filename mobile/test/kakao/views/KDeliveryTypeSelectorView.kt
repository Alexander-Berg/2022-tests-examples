package ru.yandex.market.test.kakao.views

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import org.hamcrest.Matcher
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.checkout.map.MapDeliveryType
import ru.yandex.market.clean.presentation.feature.checkout.map.view.DeliveryTypeSelectorView

class KDeliveryTypeSelectorView : KBaseCompoundView<KDeliveryTypeSelectorView> {

    constructor(function: ViewBuilder.() -> Unit) : super(DeliveryTypeSelectorView::class, function)

    constructor(
        parent: Matcher<View>,
        function: ViewBuilder.() -> Unit
    ) : super(DeliveryTypeSelectorView::class, parent, function)

    private val pvzButton = KProgressButton(parentMatcher) {
        withId(R.id.pvzButton)
    }

    private val courierButton = KProgressButton(parentMatcher) {
        withId(R.id.courierButton)
    }

    fun checkDeliveryTypeIsEnabled(deliveryType: MapDeliveryType, isEnabled: Boolean) {
        val button = getButtonByType(deliveryType)
        if (isEnabled) {
            button.isEnabled()
        } else {
            button.isDisabled()
        }
    }

    fun checkDeliveryTypeVisibility(deliveryType: MapDeliveryType, isVisible: Boolean) {
        val button = getButtonByType(deliveryType)
        if (isVisible) {
            button.isVisible()
        } else {
            button.isGone()
        }
    }

    fun selectDeliveryType(deliveryType: MapDeliveryType) {
        val activatedButton = getButtonByType(deliveryType)
        activatedButton.click()
    }

    private fun getButtonByType(deliveryType: MapDeliveryType): KProgressButton {
        return when (deliveryType) {
            MapDeliveryType.OUTLET -> pvzButton
            MapDeliveryType.COURIER -> courierButton
        }
    }

}
