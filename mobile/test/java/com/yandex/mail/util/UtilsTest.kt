@file:Suppress("detekt-kotlin:TooManyFunctions")

package com.yandex.mail.util

import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.ArraySet
import com.yandex.mail.provider.Constants
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.runners.IntegrationTestRunner.app
import com.yandex.mail.shadows.MailShadowDownloadManager
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.AccountsTools
import com.yandex.mail.tools.RobolectricTools
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.util.Arrays

@RunWith(IntegrationTestRunner::class)
class UtilsTest {

    @Test
    fun `removeNonAscii should not change ascii`() {
        val text = "asfZA123 ;=?+<>"
        assertThat(Utils.removeNonAscii(text)).isEqualTo(text)
    }

    @Test
    fun `urlSafe should remove non ascii`() {
        val text = "asfZA123 ;=?+<>"
        val nonAsciiText = text + "фывРОРЛ₽°°ў‘©™"
        assertThat(Utils.removeNonAscii(nonAsciiText)).isEqualTo(text)
    }

    @Test
    @Throws(Exception::class)
    fun `is network available returns proper value`() {
        RobolectricTools.turnNetworkOn()
        assertThat(Utils.isNetworkAvailable(app())).isTrue()

        RobolectricTools.turnNetworkOff()
        assertThat(Utils.isNetworkAvailable(app())).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun `restoreAccount should work`() {
        assertThat(Utils.restoreAccount(app())).isEqualTo(Constants.NO_UID)

        AccountsTools.insertAccount(Accounts.teamLoginData, true)
        AccountsTools.insertAccount(Accounts.testLoginData, false)
        val selectedUid = Utils.restoreAccount(app())
        assertThat(selectedUid).isEqualTo(Accounts.teamLoginData.uid)
    }

    @Test
    @Throws(Exception::class)
    fun `selectAccount should work`() {
        val acc1 = Accounts.testLoginData
        val acc2 = Accounts.teamLoginData
        AccountsTools.insertAccount(acc1, false)
        AccountsTools.insertAccount(acc2, true)
        Utils.selectAccount(app(), acc1.uid)
        assertThat(Utils.restoreAccount(app())).isEqualTo(acc1.uid)
    }

    @Test
    fun `equals returns true for nulls`() {
        assertThat(Utils.equals(null, null)).isTrue()
    }

    @Test
    fun `equals returns false for null and non null`() {
        assertThat(Utils.equals(null, Any())).isFalse()
    }

    @Test
    fun `equals returns false for non null and null`() {
        assertThat(Utils.equals(Any(), null)).isFalse()
    }

    @Test
    fun `equals returns false for two different objects`() {
        assertThat(Utils.equals(Any(), Any())).isFalse()
    }

    @Test
    fun `equals returns true for same references`() {
        val ref = Any()
        assertThat(Utils.equals(ref, ref)).isTrue()
    }

    @Test
    fun `equals returns true for different refs to equal objects`() {
        val list1 = ArrayList<String>(1)
        list1.add("")

        val list2 = ArrayList<String>(1)
        list2.add("")

        assertThat(Utils.equals(list1, list2)).isTrue()
    }

    @Test
    fun `equalsIgnoreCase return true for nulls`() {
        assertThat(Utils.equalsIgnoreCase(null, null)).isTrue()
    }

    @Test
    fun `equalsIgnoreCase returns false for null and non null 1`() {
        assertThat(Utils.equalsIgnoreCase(null, "")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for non null and null 1`() {
        assertThat(Utils.equalsIgnoreCase("", null)).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for null and non null 2`() {
        assertThat(Utils.equalsIgnoreCase(null, "a")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for non null and null 2`() {
        assertThat(Utils.equalsIgnoreCase("a", null)).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for two different objects 1`() {
        assertThat(Utils.equalsIgnoreCase("a", "")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for two different objects 2`() {
        assertThat(Utils.equalsIgnoreCase("", "a")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for two different objects 3`() {
        assertThat(Utils.equalsIgnoreCase("a", "b")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns false for two different objects 4`() {
        assertThat(Utils.equalsIgnoreCase("b", "a")).isFalse()
    }

    @Test
    fun `equalsIgnoreCase returns true for empty strings`() {
        assertThat(Utils.equalsIgnoreCase("", "")).isTrue()
    }

    @Test
    fun `equalsIgnoreCase returns true for different cases`() {
        assertThat(Utils.equalsIgnoreCase("a", "A")).isTrue()
    }

    @Test
    fun `equalsIgnoreCase returns true for same cases`() {
        assertThat(Utils.equalsIgnoreCase("a", "a")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns true for same cases`() {
        val strings = Arrays.asList("abc", "def", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "abc")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns true for different cases 1`() {
        val strings = Arrays.asList("abc", "def", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "Abc")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns true for different cases 2`() {
        val strings = Arrays.asList("abC", "def", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "abc")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns true for different cases 3`() {
        val strings = Arrays.asList("abc", "deF", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "DEf")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns false for null string`() {
        val strings = Arrays.asList("aBc", "def", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, null)).isFalse()
    }

    @Test
    fun `containsIgnoreCase returns false for empty string`() {
        val strings = Arrays.asList("abc", "def", "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "")).isFalse()
    }

    @Test
    fun `containsIgnoreCase returns true for null elements`() {
        val strings = Arrays.asList<String>("abc", null, "ghi")
        assertThat(Utils.containsIgnoreCase(strings, "ghi")).isTrue()
    }

    @Test
    fun `containsIgnoreCase returns false for empty list 1`() {
        val strings = ArrayList<String>()
        assertThat(Utils.containsIgnoreCase(strings, "")).isFalse()
    }

    @Test
    fun `containsIgnoreCase returns false for empty list 2`() {
        val strings = ArrayList<String>()
        assertThat(Utils.containsIgnoreCase(strings, null)).isFalse()
    }

    @Test
    fun `containsIgnoreCase returns false for empty list 3`() {
        val strings = ArrayList<String>()
        assertThat(Utils.containsIgnoreCase(strings, "abc")).isFalse()
    }

    @Test
    fun `closeSafelyParcelableFileDescriptor should not crash on null`() {
        Utils.closeSafely(null as ParcelFileDescriptor?)
    }

    @Test
    @Throws(IOException::class)
    fun `closeSafelyParcelableFileDescriptor should close descriptor`() {
        val parcelFileDescriptor = mock(ParcelFileDescriptor::class.java)

        Utils.closeSafely(parcelFileDescriptor)
        verify(parcelFileDescriptor).close()
    }

    @Test
    @Throws(IOException::class)
    fun `closeSafelyParcelableFileDescriptor should catch exception`() {
        val parcelFileDescriptor = mock(ParcelFileDescriptor::class.java)
        doThrow(IOException()).`when`(parcelFileDescriptor).close()

        Utils.closeSafely(parcelFileDescriptor)
        verify(parcelFileDescriptor).close()
    }

    @Test
    fun `generateAvailableFilename should return passed filename if there is no file with same name`() {
        val filename = Utils.generateAvailableFilename(app().cacheDir, "testFile.txt")
        assertThat(filename).isEqualTo("testFile.txt")
    }

    @Test
    fun `generateAvailableFilename should return changed filename if there is file with same name with extension`() {
        File(app().cacheDir, "testFile.txt").createNewFile()
        val filename = Utils.generateAvailableFilename(IntegrationTestRunner.app().cacheDir, "testFile.txt")
        assertThat(filename)
            .isNotEqualTo("testFile.txt")
            .startsWith("testFile-")
            .endsWith(".txt")
    }

    @Test
    fun `generateAvailableFilename should return changed filename if there is file with same name without extension`() {
        File(app().cacheDir, "testFile").createNewFile()
        val filename = Utils.generateAvailableFilename(IntegrationTestRunner.app().cacheDir, "testFile")
        assertThat(filename)
            .isNotEqualTo("testFile")
            .startsWith("testFile-")
    }

    @Test
    fun `generateAvailableFilename should shorten filename if name is too big`() {
        val shortenedPrefix = "a".repeat(Utils.MAXIMUM_FILENAME_LENGTH)
        val prefix = shortenedPrefix + "b"
        val displayName = "$prefix.txt"
        File(app().cacheDir, displayName).createNewFile()
        val filename = Utils.generateAvailableFilename(IntegrationTestRunner.app().cacheDir, displayName)
        assertThat(filename)
            .isNotEqualTo(displayName)
            .doesNotStartWith(prefix)
            .startsWith(shortenedPrefix)
    }

    @Test
    fun `getShortenedFilenameWithExtension should shorten filename without removing extension`() {
        val shortenedPrefix = "a".repeat(Utils.MAXIMUM_FILENAME_LENGTH)
        val prefix = shortenedPrefix + "b"
        val extension = ".txt"
        val displayName = prefix + extension
        assertThat(Utils.getShortenedFilenameWithExtension(displayName)).isEqualTo(shortenedPrefix + extension)
    }

    @Test
    fun `getShortenedFilenameWithExtension should shorten filename without extension`() {
        val shortenedFilename = "a".repeat(Utils.MAXIMUM_FILENAME_LENGTH)
        val filename = shortenedFilename + "b"
        assertThat(Utils.getShortenedFilenameWithExtension(filename)).isEqualTo(shortenedFilename)
    }

    @Test
    fun `getShortenedFilenameWithExtension should do nothing if filename too small and without extension`() {
        assertThat(Utils.getShortenedFilenameWithExtension("abc")).isEqualTo("abc")
    }

    @Test
    fun `getShortenedFilenameWithExtension should do nothing if file name too small`() {
        assertThat(Utils.getShortenedFilenameWithExtension("file.txt")).isEqualTo("file.txt")
    }

    @Test
    fun `getFilenameWithoutExtension for file with extension`() {
        assertThat(Utils.getFilenameWithoutExtension("file name.txt")).isEqualTo("file name")
    }

    @Test
    fun `getFilenameWithoutExtension for file with double extension`() {
        assertThat(Utils.getFilenameWithoutExtension("file name.txt.txt")).isEqualTo("file name.txt")
    }

    @Test
    fun `getFilenameWithoutExtension for file without extension`() {
        assertThat(Utils.getFilenameWithoutExtension("file name")).isEqualTo("file name")
    }

    @Test
    fun `getFilenameWithoutExtension for file only with extension`() {
        assertThat(Utils.getFilenameWithoutExtension(".txt")).isEqualTo("")
    }

    @Test
    fun `getFilenameWithoutExtension for empty filename`() {
        assertThat(Utils.getFilenameWithoutExtension("")).isEqualTo("")
    }

    @Test
    fun `getFilenameWithoutExtension for null`() {
        assertThat(Utils.getFilenameWithoutExtension(null)).isEqualTo(null)
    }

    @Test
    fun `join on null array returns empty string`() {
        assertThat(Utils.join(";", null)).isEqualTo("")
    }

    @Test
    fun `split works properly 1`() {
        assertThat(Utils.split(2, listOf(1, 2, 3, 4, 5, 6))).isEqualTo(
            listOf(
                listOf(1, 2),
                listOf(3, 4),
                listOf(5, 6)
            )
        )
    }

    @Test
    fun `split works properly 2`() {
        assertThat(Utils.split(3, listOf(1, 2, 3, 4, 5, 6, 7))).isEqualTo(
            listOf(
                listOf(1, 2, 3),
                listOf(4, 5, 6),
                listOf(7)
            )
        )
    }

    @Test
    fun `parseSingleAddressLine works properly`() {
        val pair = Utils.parseSingleAddressLine("name@yandex.ru")
        assertThat(pair.first).isEqualTo("name@yandex.ru")
        assertThat(pair.second).isNull()
    }

    @Test
    fun `collectionToArray works properly`() {
        assertThat(Utils.collectionToArray(listOf(1L, 2L, 3L, 4L)))
            .isEqualTo(longArrayOf(1L, 2L, 3L, 4L))
    }

    @Test
    fun `intCollectionToArray works properly`() {
        assertThat(Utils.intCollectionToArray(listOf(1, 2, 3, 4)))
            .isEqualTo(arrayOf(1, 2, 3, 4))
    }

    @Test
    fun `arrayToList on ints works properly`() {
        assertThat(Utils.arrayToList(1, 2, 3, 4, 5))
            .isEqualTo(listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun `arrayToList on longs works properly`() {
        assertThat(Utils.arrayToList(1L, 2L, 3L, 4L, 5L))
            .isEqualTo(listOf(1L, 2L, 3L, 4L, 5L))
    }

    @Test
    fun `arrayToSet works properly`() {
        assertThat(Utils.arrayToSet(1L, 2L, 3L, 4L, 5L))
            .isEqualTo(setOf(1L, 2L, 3L, 4L, 5L))
    }

    @Test
    fun `arrayToArraySet works properly`() {
        val arraySet = ArraySet<Long>()
        arraySet.add(1L)
        arraySet.add(2L)
        arraySet.add(3L)
        arraySet.add(4L)
        arraySet.add(5L)
        assertThat(Utils.arrayToArraySet(1L, 2L, 3L, 4L, 5L))
            .isEqualTo(arraySet)
    }

    @Test
    fun `unionAll works properly`() {
        assertThat(Utils.unionAll(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)))
            .isEqualTo(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9))
    }

    @Test
    fun `getSecondLinePosition works properly`() {
        assertThat(
            Utils.getSecondLinePosition(
                """123
            |456
            |789
        """.trimMargin()
            )
        ).isEqualTo(7)
    }

    @Test
    fun `sum works properly`() {
        assertThat(Utils.sum(listOf(1, 2, 3, 4, 5)))
            .isEqualTo(15)
    }

    @Test
    fun `md5OrNull works properly`() {
        assertThat(Utils.md5OrNull("12345")).isEqualTo("827CCB0EEA8A706C4C34A16891F84E7B")
    }

    @Test
    fun `md5 works properly`() {
        assertThat(Utils.md5("12345")).isEqualTo("827CCB0EEA8A706C4C34A16891F84E7B")
    }

    @Test
    fun `join array works properly`() {
        assertThat(Utils.join(";", longArrayOf(1L, 2L, 3L))).isEqualTo("1;2;3")
    }

    @Test
    fun `join two strings works properly 1`() {
        assertThat(Utils.join("123", "4", ";")).isEqualTo("123;4")
    }

    @Test
    fun `join two strings works properly 2`() {
        assertThat(Utils.join(null, "4", ";")).isEqualTo("4")
    }

    @Test
    fun `join two strings works properly 3`() {
        assertThat(Utils.join("1234", null, ";")).isEqualTo("1234")
    }

    @Test
    fun `join two lists works properly 1`() {
        assertThat(Utils.join(listOf(1, 2, 3), listOf(4, 5, 6))).isEqualTo(listOf(1, 2, 3, 4, 5, 6))
    }

    @Test
    fun `join two lists works properly 2`() {
        assertThat(Utils.join(listOf(1, 2, 3), null)).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `nonNull works properly 1`() {
        assertThat(Utils.nonNull(null)).isFalse()
    }

    @Test
    fun `nonNull works properly 2`() {
        assertThat(Utils.nonNull(emptyList<Int>())).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun `checkNotNull works properly 1`() {
        assertThat(Utils.checkNotNull(null, "description"))
        fail("Exception expected")
    }

    @Test
    fun `checkNotNull works properly 2`() {
        assertThat(Utils.checkNotNull(emptyList<Int>(), "description"))
    }

    @Test
    fun `orDefault works properly 1`() {
        assertThat(Utils.orDefault(null, listOf(2))).isEqualTo(listOf(2))
    }

    @Test
    fun `orDefault works properly 2`() {
        assertThat(Utils.orDefault(listOf(1), listOf(2))).isEqualTo(listOf(1))
    }

    @Test
    fun `floorDiv works properly`() {
        assertThat(Utils.floorDiv(6, 4)).isEqualTo(1)
    }

    @Test
    fun `floorMod works properly`() {
        assertThat(Utils.floorMod(6, 4)).isEqualTo(2)
    }

    @Test
    fun `getExtension works properly on bmp`() {
        assertThat(Utils.getExtension("/path/to/file/name.bmp")).isEqualTo("bmp")
    }

    @Test
    fun `getExtension works properly on txt`() {
        assertThat(Utils.getExtension("/path/to/file/name.txt")).isEqualTo("txt")
    }

    @Test
    fun `getExtension works properly on png`() {
        assertThat(Utils.getExtension("/path/to/file/name.png")).isEqualTo("png")
    }

    @Test
    fun `getExtension works properly on folder`() {
        assertThat(Utils.getExtension("/path/to/file/")).isEqualTo("")
    }

    @Test
    fun `getExtension works properly on disk folder`() {
        assertThat(Utils.getExtension("Загрузки")).isEqualTo("")
    }

    @Test
    fun `getExtension works properly on no extension file`() {
        assertThat(Utils.getExtension("/path/to/file/name")).isEqualTo("")
    }

    @Test
    fun `getExtension on null returns null`() {
        assertThat(Utils.getExtension(null)).isEqualTo(null)
    }

    @Test
    fun `reverseInPlace works properly`() {
        val array = arrayOf(1, 2, 3, 4, 5)
        Utils.reverseInPlace(array)
        assertThat(array).isEqualTo(arrayOf(5, 4, 3, 2, 1))
    }

    @Test
    fun `stringOrEmptyIfNull works properly 1`() {
        assertThat(Utils.stringOrEmptyIfNull(null)).isEqualTo("")
    }

    @Test
    fun `stringOrEmptyIfNull works properly 2`() {
        assertThat(Utils.stringOrEmptyIfNull("123")).isEqualTo("123")
    }

    @Test
    fun `parseIntWithDefault works properly 1`() {
        assertThat(Utils.parseIntWithDefault("123", 1, 10)).isEqualTo(123)
    }

    @Test
    fun `parseIntWithDefault works properly 2`() {
        assertThat(Utils.parseIntWithDefault("abc", 1, 10)).isEqualTo(1)
    }

    @Test
    fun `isOppositeSigns works properly 1`() {
        assertThat(Utils.isSameSign(1, 10)).isTrue()
    }

    @Test
    fun `isOppositeSigns works properly 2`() {
        assertThat(Utils.isSameSign(-10, 10)).isFalse()
    }

    @Config(sdk = [Build.VERSION_CODES.N], shadows = [MailShadowDownloadManager::class])
    @Test
    fun `createViewIntent should create download manager intent`() {
        val downloadManager = app().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse("https://url"))
            .setMimeType("application/octet-stream")
        val id = downloadManager.enqueue(request)

        val shadowRequest = Shadows.shadowOf(request)
        shadowRequest.status = DownloadManager.STATUS_SUCCESSFUL

        val intent = Utils.createViewIntent(app(), id)!!
        assertThat(intent.action).contains(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("content://downloads/my_downloads/$id")
    }

    @Test
    fun `open link in yabro if it is default browser`() {
        val url = "https://yandex.ru"
        val yabroPackageName = Utils.YA_BRO_PACKAGES.last()
        val anotherBroPackageName = "org.example.browser"

        installPackageName(yabroPackageName)
        installPackageName(anotherBroPackageName)

        preparePackageManagerForWebBrowsableActivity(url, yabroPackageName)

        val context = MockActivityStarter(RuntimeEnvironment.application)
        Utils.openLinkInYaBro(context, url)

        assertThat(context.activityStarted).isTrue()
        assertThat(context.lastStartedIntent).isNotNull()
        assertThat(context.lastStartedIntent?.getPackage()).isEqualTo(yabroPackageName)
    }

    @Test
    fun `open link in first installed yabro if default bro is not yabro`() {
        val url = "https://yandex.ru"
        val firstYabroPackageName = Utils.YA_BRO_PACKAGES.first()
        val secondYabroPackageName = Utils.YA_BRO_PACKAGES[1]
        val anotherBroPackageName = "org.example.browser"

        installPackageName(firstYabroPackageName)
        installPackageName(secondYabroPackageName)
        installPackageName(anotherBroPackageName)

        preparePackageManagerForWebBrowsableActivity(url, anotherBroPackageName)

        val context = MockActivityStarter(RuntimeEnvironment.application)
        Utils.openLinkInYaBro(context, url)

        assertThat(context.activityStarted).isTrue()
        assertThat(context.lastStartedIntent).isNotNull()
        assertThat(context.lastStartedIntent?.getPackage()).isEqualTo(firstYabroPackageName)
    }

    @Test
    fun `do nothing if there are no installed yabro`() {
        val url = "https://yandex.ru"
        val anotherBroPackageName = "org.example.browser"

        installPackageName(anotherBroPackageName)

        preparePackageManagerForWebBrowsableActivity(url, anotherBroPackageName)

        val context = MockActivityStarter(RuntimeEnvironment.application)
        Utils.openLinkInYaBro(context, url)

        assertThat(context.activityStarted).isFalse()
        assertThat(context.lastStartedIntent).isNull()
    }

    private fun installPackageName(packageName: String) {
        val packageManager = shadowOf(RuntimeEnvironment.application.packageManager)
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        val applicationInfo = ApplicationInfo()
        applicationInfo.packageName = packageName
        packageInfo.applicationInfo = applicationInfo
        packageManager.installPackage(packageInfo)
        packageManager.addOrUpdateActivity(
            ActivityInfo().apply {
                this.name = "BrowserActivity"
                this.packageName = packageName
            }
        )

        val filter = IntentFilter(Intent.ACTION_VIEW)
        filter.addCategory(Intent.CATEGORY_BROWSABLE)
        filter.addDataScheme("http")
        filter.addDataScheme("https")
        packageManager.addIntentFilterForActivity(ComponentName(packageName, "BrowserActivity"), filter)
    }

    private fun preparePackageManagerForWebBrowsableActivity(url: String, packageName: String) {
        val packageManager = shadowOf(RuntimeEnvironment.application.packageManager)
        try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.component = null
            val resolveInfo = ResolveInfo()
            val applicationInfo = ApplicationInfo()
            applicationInfo.packageName = packageName
            val activityInfo = ActivityInfo()
            activityInfo.applicationInfo = applicationInfo
            activityInfo.packageName = packageName
            resolveInfo.activityInfo = activityInfo
            resolveInfo.filter = IntentFilter(Intent.ACTION_VIEW)
            resolveInfo.filter.addCategory(Intent.CATEGORY_BROWSABLE)
            resolveInfo.filter.addDataScheme("http")
            resolveInfo.filter.addDataScheme("https")
            resolveInfo.match = IntentFilter.MATCH_CATEGORY_SCHEME
            packageManager.addResolveInfoForIntent(intent, resolveInfo)
        } catch (e: URISyntaxException) {
            fail(e.message)
        }
    }

    private class MockActivityStarter(context: Context) : ContextWrapper(context) {
        var activityStarted = false
        var lastStartedIntent: Intent? = null

        override fun startActivity(intent: Intent?) {
            activityStarted = true
            lastStartedIntent = intent
        }

        override fun startActivity(intent: Intent?, options: Bundle?) {
            activityStarted = true
            lastStartedIntent = intent
        }
    }
}
