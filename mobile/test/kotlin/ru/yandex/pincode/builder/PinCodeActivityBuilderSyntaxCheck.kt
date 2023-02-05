package ru.yandex.pincode.builder

import org.junit.Test

class PinCodeActivityBuilderSyntaxCheck {
    @Test
    fun createPinCodeSyntax_shouldCompileWithoutErrors() {
        PinCodeActivityBuilder().createNewPinCode()
            .setTitle(0)
            .setAppIcon(0)
            .setLinkColor(0)
            .setPrimaryTextColor(0)
            .setToolbarVisible(true)
            .setSecondaryTextColor(0)
            .setRequestCode(0)
    }

    @Test
    fun validatePinCode_shouldCompileWithoutErrors() {
        PinCodeActivityBuilder().validatePinCode("1234")
            .setTitle(0)
            .setAppIcon(0)
            .setLinkColor(0)
            .setPrimaryTextColor(0)
            .setToolbarVisible(true)
            .setSecondaryTextColor(0)
            .setRequestCode(0)
    }
}
