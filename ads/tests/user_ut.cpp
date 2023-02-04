#include <ads/bigkv/search/entities/user/user.h>
#include <library/cpp/testing/unittest/registar.h>
#include <yabs/server/util/bobhash.h>


class TUserEntityFeaturesTest : public TTestBase {
    public:
        void TestCounters() {
            UNIT_ASSERT_EQUAL(Features["CountersAggregatedValues"].Size(), (507 + 450) * 4);
        }

        void SetUp() override {
            NBSYeti::TBigbPublicProto profile;
            NSearchTsar::TUserEntity userEntity;
            Features = userEntity.GetParser()->Parse(profile, 0);
        }

    private:
        NYT::TNode::TMapType Features;

        UNIT_TEST_SUITE(TUserEntityFeaturesTest);

        UNIT_TEST(TestCounters);

        UNIT_TEST_SUITE_END();
};

UNIT_TEST_SUITE_REGISTRATION(TUserEntityFeaturesTest);
