package com.yandex.mail.testopithecus

import com.yandex.mail.testopithecus.pages.FolderListPage
import com.yandex.xplat.testopithecus.DefaultFolderName
import com.yandex.xplat.testopithecus.FolderName
import java.io.File

fun whileMax(condition: () -> Boolean, maxTimes: Int = 15, callback: () -> Unit) {
    var i = 0

    while (condition.invoke() && i < maxTimes) {
        callback.invoke()
        i++
    }
}

fun formatIfTabName(folderName: FolderName): FolderName {
    return when (folderName) {
        DefaultFolderName.mailingLists -> FolderListPage.SUBSCRIPTIONS
        DefaultFolderName.socialNetworks -> FolderListPage.SOCIALMEDIA
        else -> folderName
    }
}

fun isTestAlreadyPassed(testName: String): Boolean {
    val fileTestPassed = File("/sdcard/allure-results/test-passed.txt")
    if (fileTestPassed.exists()) {
        fileTestPassed.bufferedReader().readLines().forEach {
            if (it == testName) {
                return true
            }
        }
    }
    return false
}

fun isTestAlreadyFailed(testName: String): Boolean {
    val fileTestFailed = File("/sdcard/allure-results/test-failed.txt")
    if (fileTestFailed.exists()) {
        fileTestFailed.bufferedReader().readLines().forEach {
            if (it == testName) {
                return true
            }
        }
    }
    return false
}
