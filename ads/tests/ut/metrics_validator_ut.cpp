#include <ads/tensor_transport/lib_2/validator_stats.h>
#include <ads/tensor_transport/lib_2/metrics_validator.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/random/random.h>


using namespace NTsarTransport;


Y_UNIT_TEST_SUITE(TMetricsValidator) {
    Y_UNIT_TEST(CopyValidatorZeroToleranceTestCase) {
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats firstValidatorStats(numberOfHashes, outputTableSize);
        TValidatorStats secondValidatorStats = firstValidatorStats;
        TMetricsValidator metricsValidator(0.0f);
        bool result = metricsValidator.Validate(&firstValidatorStats, &secondValidatorStats);
        UNIT_ASSERT(result);
    }

    Y_UNIT_TEST(BrokenNumberOfHashesZeroToleranceTestCase) {
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats firstValidatorStats(numberOfHashes, outputTableSize);
        TValidatorStats secondValidatorStats(numberOfHashes * 10, outputTableSize);
        TMetricsValidator metricsValidator(0.0f);
        bool result = metricsValidator.Validate(&firstValidatorStats, &secondValidatorStats);
        UNIT_ASSERT(!result);
    }

    Y_UNIT_TEST(BrokenOutputTableSizeZeroToleranceTestCase) {
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats firstValidatorStats(numberOfHashes, outputTableSize);
        TValidatorStats secondValidatorStats(numberOfHashes, outputTableSize * 10);
        TMetricsValidator metricsValidator(0.0f);
        bool result = metricsValidator.Validate(&firstValidatorStats, &secondValidatorStats);
        UNIT_ASSERT(!result);
    }

    Y_UNIT_TEST(OutputSizeInToleranceIntervalTestCase) {
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats firstValidatorStats(numberOfHashes, outputTableSize);
        float secondTableOutputSize = outputTableSize * 0.09f;

        TValidatorStats secondValidatorStats(numberOfHashes, secondTableOutputSize);
        TMetricsValidator metricsValidator(0.1f);
        bool result = metricsValidator.Validate(&firstValidatorStats, &secondValidatorStats);
        UNIT_ASSERT(result);
    }

    Y_UNIT_TEST(NumberOfHashesInToleranceIntervalTestCase) {
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats firstValidatorStats(numberOfHashes, outputTableSize);
        float secondNumberOfHashes = outputTableSize * 0.09f;
        TValidatorStats secondValidatorStats(secondNumberOfHashes, outputTableSize);
        TMetricsValidator metricsValidator(0.1f);
        bool result = metricsValidator.Validate(&firstValidatorStats, &secondValidatorStats);
        UNIT_ASSERT(result);
    }
}


