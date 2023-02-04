package ru.auto.ara.core.testdata

data class HasCarfaxByVinTestParams(val snippetType: String, val vinStatus: String, val historyStatus: String)

val HAS_CARFAX_BY_VIN_TEST_DATA = listOf(
    HasCarfaxByVinTestParams("extended", "ok", "no"),
    HasCarfaxByVinTestParams("common", "ok", "no"),
    HasCarfaxByVinTestParams("extended", "invalid", "no"),
    HasCarfaxByVinTestParams("common", "invalid", "no"),
    HasCarfaxByVinTestParams("extended", "error", "no"),
    HasCarfaxByVinTestParams("common", "error", "no"),
    HasCarfaxByVinTestParams("extended", "unknown", "no"),
    HasCarfaxByVinTestParams("common", "unknown", "no"),
    HasCarfaxByVinTestParams("extended", "untrusted", "no"),
    HasCarfaxByVinTestParams("common", "untrusted", "no"),
    HasCarfaxByVinTestParams("extended", "not_matched_plate", "no"),
    HasCarfaxByVinTestParams("common", "not_matched_plate", "no"),
    HasCarfaxByVinTestParams("common", "invalid", "has")
)
