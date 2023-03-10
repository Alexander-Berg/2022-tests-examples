// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/feature/translator-features.ts >>>

package com.yandex.xplat.testopithecus

import com.yandex.xplat.common.*
import com.yandex.xplat.eventus.common.*
import com.yandex.xplat.eventus.*
import com.yandex.xplat.mapi.*
import com.yandex.xplat.testopithecus.common.*

public typealias LanguageName = String

public typealias LanguageCode = String

public open class TranslatorBarFeature private constructor(): Feature<TranslatorBar>("TranslatorBar", "Плашка переводчика в просмотре письма") {
    companion object {
        @JvmStatic var `get`: TranslatorBarFeature = TranslatorBarFeature()
    }
}

public interface TranslatorBar {
    fun isTranslatorBarShown(): Boolean
    fun isMessageTranslated(): Boolean
    fun getSourceLanguage(): LanguageName
    fun tapOnSourceLanguage(): Unit
    fun getTargetLanguage(): LanguageName
    fun tapOnTargetLanguage(): Unit
    fun tapOnTranslateButton(): Unit
    fun tapOnRevertButton(): Unit
    fun getSubmitButtonLabel(): String
    fun tapOnCloseBarButton(hideTranslatorForThisLanguage: Boolean): Unit
}

public open class TranslatorLanguageListFeature private constructor(): Feature<TranslatorLanguageList>("TranslatorLanguageListFeature", "Список языков, появляющийся при тапе на язык письма/язык перевода") {
    companion object {
        @JvmStatic var `get`: TranslatorLanguageListFeature = TranslatorLanguageListFeature()
    }
}

public interface TranslatorLanguageList {
    fun getAllSourceLanguages(): YSArray<LanguageName>
    fun setSourceLanguage(language: LanguageName): Unit
    fun getCurrentSourceLanguage(): LanguageName?
    fun getDeterminedAutomaticallySourceLanguage(): LanguageName
    fun getRecentSourceLanguages(): YSArray<LanguageName>
    fun getAllTargetLanguages(): YSArray<LanguageName>
    fun setTargetLanguage(language: LanguageName, addToRecent: Boolean): Unit
    fun getCurrentTargetLanguage(): LanguageName?
    fun getDefaultTargetLanguage(): LanguageName
    fun getRecentTargetLanguages(): YSArray<LanguageName>
}

public open class TranslatorLanguageListSearchFeature private constructor(): Feature<TranslatorLanguageListSearch>("TranslatorLanguageListSearchFeature", "Поиск на экране выбора языка в перводчике") {
    companion object {
        @JvmStatic var `get`: TranslatorLanguageListSearchFeature = TranslatorLanguageListSearchFeature()
    }
}

public interface TranslatorLanguageListSearch {
    fun tapOnSearchTextField(): Unit
    fun isSearchTextFieldFocused(): Boolean
    fun tapOnCancelButton(): Unit
    fun enterSearchQuery(query: String): Unit
    fun getSearchQuery(): String
    fun getSearchedLanguageList(): YSArray<LanguageName>
    fun tapOnClearSearchFieldButton(): Unit
}

public open class TranslatorSettingsFeature private constructor(): Feature<TranslatorSettings>("TranslatorSettingsFeature", "Переводчик в настройках") {
    companion object {
        @JvmStatic var `get`: TranslatorSettingsFeature = TranslatorSettingsFeature()
    }
}

public interface TranslatorSettings {
    fun switchTranslator(): Unit
    fun isTranslatorEnabled(): Boolean
    fun isIgnoredLanguageCellShown(): Boolean
    fun openIgnoredTranslationLanguageList(): Unit
    fun removeTranslationLanguageFromIgnored(language: LanguageName): Unit
    fun getIgnoredTranslationLanguages(): YSArray<LanguageName>
    fun closeIgnoredTranslationLanguageList(): Unit
    fun openDefaultTranslationLanguageList(): Unit
    fun setDefaultTranslationLanguage(language: LanguageName): Unit
    fun getDefaultTranslationLanguage(): LanguageName
    fun getDefaultTranslationLanguageFromGeneralSettingsPage(): LanguageName
    fun closeDefaultTranslationLanguageList(): Unit
}

