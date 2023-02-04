#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/banner_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/preprocessors/banner_preprocessors/base_fields_preprocessors.h>
#include <library/cpp/iterator/zip.h>


namespace NProfilePreprocessing {

class TBannerCountersPreprocessorsTests : public TTestBase {
public:
    void SchemaTest() {
        TBannerBaseFieldsPreprocessor preproc;
        auto actualResult = preproc.Parse(*ProfileBuilder.GetProfile(), {});
        auto schema = preproc.Schema();
        
        UNIT_ASSERT_VALUES_EQUAL(schema.size(), actualResult.size());

        for (const auto& [name, _]: schema) {
            UNIT_ASSERT(actualResult.contains(name));
        }
    }

    void SetUp() override {
    }

private:
    TBannerProtoBuilder ProfileBuilder;

    UNIT_TEST_SUITE(TBannerCountersPreprocessorsTests);
    UNIT_TEST(SchemaTest);
    UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TBannerCountersPreprocessorsTests);
}
