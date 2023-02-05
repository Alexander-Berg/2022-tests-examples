package com.yandex.mail.testopithecus

import com.yandex.xplat.testopithecus.common.ReportIntegration
import io.qameta.allure.kotlin.Allure

class DefaultReportIntegration : ReportIntegration {
    override fun addTestpalmId(id: Int) {
        Allure.tms("$id", "https://testpalm.yandex-team.ru/testcase/mobilemail-$id")
    }

    override fun addFeatureName(feature: String) {
        Allure.feature(feature)
    }
}
