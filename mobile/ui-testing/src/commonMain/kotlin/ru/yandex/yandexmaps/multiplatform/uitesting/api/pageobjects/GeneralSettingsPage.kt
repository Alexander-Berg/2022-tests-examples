package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public enum class NightMode {
    ON,
    OFF,
    AUTO,
    SYSTEM
}

public enum class MeasurementUnits {
    KILOMETERS,
    MILES
}

public enum class VoiceInputLanguage {
    RUSSIAN,
    ENGLISH,
    UKRAINIAN,
    TURKISH,
    UZBEK,
}

public interface GeneralSettingsPage {
    public fun openGeneralSettings()
    public fun closeGeneralSettings()

    public fun getNightMode(): NightMode
    public fun setNightMode(nightMode: NightMode)

    public fun getMeasurementUnits(): MeasurementUnits
    public fun setMeasurementUnits(units: MeasurementUnits)

    public fun getVoiceInputLanguage(): VoiceInputLanguage
    public fun setVoiceInputLanguage(language: VoiceInputLanguage)

    public fun isOrganizationReviewsPushesEnabled(): Boolean
    public fun switchOrganizationReviewsPushesEnabled()

    public fun isPlacesRecommendationsPushesEnabled(): Boolean
    public fun switchPlacesRecommendationsPushesEnabled()
}

public data class SettingsStorage internal constructor(
    var nightMode: NightMode,
    var measurementUnits: MeasurementUnits,
    var voiceInputLanguage: VoiceInputLanguage,
    var orgReviewsPushesEnabled: Boolean,
    var placesRecommendationsPushesEnabled: Boolean,
) {
    public companion object {
        public fun iosDefault(): SettingsStorage {
            return SettingsStorage(
                NightMode.SYSTEM,
                MeasurementUnits.KILOMETERS,
                VoiceInputLanguage.RUSSIAN,
                orgReviewsPushesEnabled = false,
                placesRecommendationsPushesEnabled = false
            )
        }

        public fun androidDefault(): SettingsStorage {
            return SettingsStorage(
                NightMode.SYSTEM,
                MeasurementUnits.KILOMETERS,
                VoiceInputLanguage.ENGLISH,
                orgReviewsPushesEnabled = true,
                placesRecommendationsPushesEnabled = true
            )
        }
    }
}
