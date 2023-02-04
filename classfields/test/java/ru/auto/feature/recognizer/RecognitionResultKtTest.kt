package ru.auto.feature.recognizer

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe


internal class RecognitionResultValidationTest : FreeSpec({
    "should validate vin" - {
        val validVin = "1M8GDM9AXKP042788"
        "should change nothing if vin is valid" {
            validateVinAndCorrect(validVin) shouldBe validVin
        }
        "should check vin length" - {
            "should fail if length is less than 17" {
                val vinLessThan17 = validVin.drop(1)
                validateVinAndCorrect(vinLessThan17) shouldBe null
            }
            "should fail if length is more than 17" {
                val vinMoreThan17 = validVin + "A"
                validateVinAndCorrect(vin = vinMoreThan17) shouldBe null
            }
        }
        "should correct misreported vin symbols" - {
            "should correct i, q, o to numbers" {
                val vinWithMistakes = "iM8GDMqAXKPo42788"
                validateVinAndCorrect(vin = vinWithMistakes) shouldBe validVin
            }
            "should correct to uppercase" {
                val vinWithMistakes = "1M8gDM9AxKP042788"
                validateVinAndCorrect(vin = vinWithMistakes) shouldBe validVin
            }
        }
    }
    "should validate license number" - {
        "should change nothing if license is valid" - {
            val validLicenses = listOf("М179ОО", "ХХ179")
            val validRegions = listOf("99", "199")
            for (license in validLicenses) {
                for (region in validRegions) {
                    "$license$region (all russian letters) is valid" {
                        val validLicense =  license + region
                        validateLicenseAndCorrect(validLicense) shouldBe validLicense
                    }
                }
            }
        }
        "should correct latin to russian" {
            val licenseWithLatin = "M179OO199" // notice latin
            val correctedLicense = "М179ОО199" // notice cyrillic
            validateLicenseAndCorrect(license = licenseWithLatin) shouldBe correctedLicense
        }
        "should convert to uppercase" {
            val licenseLower = "м179оо199"
            val correctedLicense = "М179ОО199"
            validateLicenseAndCorrect(license = licenseLower) shouldBe correctedLicense
        }
        "should check license number length" - {
            "should fail if license number length is less than 7" {
                val licenseWithTooFewSymbols = "179О13"
                validateLicenseAndCorrect(license = licenseWithTooFewSymbols) shouldBe null
            }
            "should fail if license number length is more than 9" {
                val licenseWithTooManySymbols = "М179ОО1390"
                validateLicenseAndCorrect(license = licenseWithTooManySymbols) shouldBe null
            }
        }
    }
})
