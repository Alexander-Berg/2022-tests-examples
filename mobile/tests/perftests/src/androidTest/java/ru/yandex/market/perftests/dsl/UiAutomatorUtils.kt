package ru.yandex.market.perftests.dsl

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import ru.yandex.market.perftests.scenario.MarketPerfTest

fun MarketPerfTest.findByText(text: String): UiObject {
    return device.findObject(
        selectorByText(text)
    )
}

fun MarketPerfTest.findById(id: String): UiObject {
    return device.findObject(
        selectorById(id)
    )
}

fun MarketPerfTest.findByDescription(description: String): UiObject {
    return device.findObject(
        selectorByDescription(description)
    )
}

fun MarketPerfTest.waitById(resourceId: String, timeout: Long = 10000L): UiObject2 {
    val condition = Until.findObject(By.res(packageName, resourceId))
    return checkNotNull(device.wait(condition, timeout)) {
        "UI object with resource id $resourceId was not found"
    }
}

fun MarketPerfTest.waitByDescription(description: String, timeout: Long = 10000L): UiObject2 {
    val condition = Until.findObject(By.desc(description))
    return checkNotNull(device.wait(condition, timeout)) {
        "UI object with description id $description was not found"
    }
}

fun MarketPerfTest.waitByText(text: String, timeout: Long = 10000L): UiObject2 {
    val condition = Until.findObject(By.text(text))
    return checkNotNull(device.wait(condition, timeout)) {
        "UI object with description id $text was not found"
    }
}

fun MarketPerfTest.selectorById(id: String): UiSelector {
    return UiSelector().resourceId("${packageName}:id/$id")
}

fun MarketPerfTest.selectorByText(text: String): UiSelector {
    return UiSelector().text(text)
}

fun MarketPerfTest.selectorByClass(clazz: Class<*>): UiSelector {
    return UiSelector().className(clazz)
}

fun MarketPerfTest.selectorByDescription(description: String): UiSelector {
    return UiSelector().description(description)
}

fun MarketPerfTest.getRecyclerById(id: String): UiScrollable {
    return UiScrollable(selectorById(id).scrollable(true))
}

fun MarketPerfTest.getRecyclerByDescription(description: String): UiScrollable {
    return UiScrollable(selectorByDescription(description).scrollable(true))
}

fun MarketPerfTest.findInRecyclerById(recyclerId: String, targetViewId: String): UiObject {
    getRecyclerById(recyclerId).scrollIntoView(selectorById(targetViewId))
    return findById(targetViewId)
}

fun MarketPerfTest.findInRecyclerByIdAndDescription(recyclerId: String, targetViewDescription: String): UiObject {
    getRecyclerById(recyclerId).scrollIntoView(selectorByDescription(targetViewDescription))
    return findByDescription(targetViewDescription)
}

fun MarketPerfTest.findInRecyclerByIdAndText(recyclerId: String, targetViewText: String): UiObject {
    getRecyclerById(recyclerId).scrollIntoView(selectorByText(targetViewText))
    return findByText(targetViewText)
}

fun MarketPerfTest.findInRecyclerByDescription(recyclerDescription: String, targetViewDescription: String): UiObject {
    getRecyclerByDescription(recyclerDescription).scrollIntoView(selectorByDescription(targetViewDescription))
    return findByDescription(targetViewDescription)
}

fun MarketPerfTest.findInRecyclerByDescriptionAndText(recyclerDescription: String, targetViewText: String): UiObject {
    getRecyclerByDescription(recyclerDescription).scrollIntoView(selectorByText(targetViewText))
    return findByText(targetViewText)
}

fun MarketPerfTest.pressHome() {
    device.pressHome()
}