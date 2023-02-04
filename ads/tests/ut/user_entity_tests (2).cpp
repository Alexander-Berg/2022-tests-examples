#include <library/cpp/testing/unittest/registar.h>

#include <ads/bigkv/torch_v2/entities/user.h>


Y_UNIT_TEST_SUITE(TUserEntityTests) {
    Y_UNIT_TEST(EmptyProfileTest) {
        NewUserFactors::TProfile profile;
        NTorchV2::TUser user(profile, 12345);
        Y_UNUSED(user);
    }

    Y_UNIT_TEST(EmptyProfileWithQueryHistoryTest) {
        NewUserFactors::TProfile profile;
        NTorchV2::TUserWithSearchHistory user(profile, 12345);
        Y_UNUSED(user);
    }
}


Y_UNIT_TEST_SUITE(TUserV1PrepreprocessorTests) {
    Y_UNIT_TEST(ParseEmptyProfileTest) {
        NTorchV2::TUserV1Preprocessor preprocessor;

        const auto schema = preprocessor.Schema();
        const auto features = preprocessor.ParseProfile(yabs::proto::Profile(), NProfilePreprocessing::TArgs(0), {});

        UNIT_ASSERT_EQUAL(schema.size(), features.size());
        for (const auto& [name, type] : schema) {
            UNIT_ASSERT(features.contains(name));
        }
    }
}
