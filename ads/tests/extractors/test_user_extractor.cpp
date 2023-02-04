#include <ads/bigkv/preprocessor_primitives/base_preprocessor/extract.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>


namespace NProfilePreprocessing {

    class TUserExtractFeaturesTests : public TTestBase {
    public:
        void EmptyRequestTest() {
            yabs::proto::Profile EmptyUserProfile;
            TUserExtractRequest userRequest({}, {});
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Items.GetSize(), 0);
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Counters.GetSize(), 0);
            TExtractedUserFeatures extractedUserFeatures(&EmptyUserProfile, userRequest);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Items.GetSize(), 0);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Counters.GetSize(), 0);
        }

        void OneItemRequestTest() {
            TUserExtractRequest userRequest({NBSData::NKeywords::KW_USER_REGION}, {});
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Items.GetSize(), 1);
            UNIT_ASSERT(userRequest.Items.Contains(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT(!userRequest.Items.Contains(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Counters.GetSize(), 0);

            TExtractedUserFeatures extractedUserFeatures(ProfileBuilder.GetProfile(), userRequest);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Items.GetSize(), 1);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Counters.GetSize(), 0);

            auto item = extractedUserFeatures.Items[NBSData::NKeywords::KW_USER_REGION][0];
            UNIT_ASSERT_UNEQUAL(item, nullptr);
            UNIT_ASSERT_VALUES_EQUAL(item->keyword_id(), static_cast<ui32>(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT_VALUES_EQUAL(item->uint_values(0), 123);
        }

        void MultipleItemsRequestTest() {
            TUserExtractRequest userRequest({NBSData::NKeywords::KW_USER_REGION, NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS}, {});
            UNIT_ASSERT_GE(userRequest.Items.GetSize(), 2);
            UNIT_ASSERT(userRequest.Items.Contains(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT(userRequest.Items.Contains(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Counters.GetSize(), 0);

            TExtractedUserFeatures extractedUserFeatures(ProfileBuilder.GetProfile(), userRequest);
            UNIT_ASSERT_GE(extractedUserFeatures.Items.GetSize(), 2);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Counters.GetSize(), 0);

            auto regionItem = extractedUserFeatures.Items[NBSData::NKeywords::KW_USER_REGION][0];
            UNIT_ASSERT_UNEQUAL(regionItem, nullptr);
            UNIT_ASSERT_VALUES_EQUAL(regionItem->keyword_id(), static_cast<ui32>(NBSData::NKeywords::KW_USER_REGION));
            UNIT_ASSERT_VALUES_EQUAL(regionItem->uint_values_size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(regionItem->uint_values(0), 123);

            auto interestsItem = extractedUserFeatures.Items[NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS][0];
            UNIT_ASSERT_UNEQUAL(interestsItem, nullptr);
            UNIT_ASSERT_VALUES_EQUAL(interestsItem->keyword_id(), static_cast<ui32>(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS));
            UNIT_ASSERT_VALUES_EQUAL(interestsItem->uint_values_size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(interestsItem->uint_values(0), 3);
            UNIT_ASSERT_VALUES_EQUAL(interestsItem->uint_values(1), 6);
            UNIT_ASSERT_VALUES_EQUAL(interestsItem->uint_values(2), 10);
        }

        void MultipleCountersRequestTest() {
            TUserExtractRequest userRequest({}, {123, 1023});
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Items.GetSize(), 0);
            UNIT_ASSERT_VALUES_EQUAL(userRequest.Counters.GetSize(), 1023 - 123 + 1);
            UNIT_ASSERT(userRequest.Counters.Contains(123));
            UNIT_ASSERT(userRequest.Counters.Contains(1023));

            TExtractedUserFeatures extractedUserFeatures(ProfileBuilder.GetProfile(), userRequest);
            UNIT_ASSERT_VALUES_EQUAL(extractedUserFeatures.Items.GetSize(), 0);
            UNIT_ASSERT_GE(extractedUserFeatures.Counters.GetSize(), 2);

            auto counter123 = extractedUserFeatures.Counters[123];
            UNIT_ASSERT_UNEQUAL(counter123, nullptr);
            UNIT_ASSERT_VALUES_EQUAL(counter123->counter_id(), 123);
            UNIT_ASSERT_VALUES_EQUAL(counter123->key_size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(counter123->value_size(), 3);

            auto counter1023 = extractedUserFeatures.Counters[1023];
            UNIT_ASSERT_UNEQUAL(counter1023, nullptr);
            UNIT_ASSERT_VALUES_EQUAL(counter1023->counter_id(), 1023);
            UNIT_ASSERT_VALUES_EQUAL(counter1023->key_size(), 4);
            UNIT_ASSERT_VALUES_EQUAL(counter1023->value_size(), 4);
        }

        void MergeRequestsTest() {
            TUserExtractRequest firstUserRequest({NBSData::NKeywords::KW_USER_REGION}, {1023});
            TUserExtractRequest secondUserRequest({NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS}, {123});
            TUserExtractRequest userRequest;
            userRequest.Merge(firstUserRequest);
            userRequest.Merge(secondUserRequest);

            TExtractedUserFeatures extractedUserFeatures(ProfileBuilder.GetProfile(), userRequest);
            UNIT_ASSERT_GE(extractedUserFeatures.Items.GetSize(), 2);
            UNIT_ASSERT_GE(extractedUserFeatures.Counters.GetSize(), 2);

            UNIT_ASSERT_UNEQUAL(extractedUserFeatures.Items[NBSData::NKeywords::KW_USER_REGION].size(), 0);
            UNIT_ASSERT_UNEQUAL(extractedUserFeatures.Items[NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS].size(), 0);

            UNIT_ASSERT_UNEQUAL(extractedUserFeatures.Counters[123], nullptr);
            UNIT_ASSERT_UNEQUAL(extractedUserFeatures.Counters[1023], nullptr);
        }

        void SetUp() override {
            ProfileBuilder.AddItem(NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS, {3, 6, 10});
            ProfileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {123});

            ProfileBuilder.AddCounter(123, {4, 5, 6}, {7, 8, 9});
            ProfileBuilder.AddCounter(1023, {14, 15, 16, 17}, {17, 18, 19, 20});
        }
    
    private:
        TUserProtoBuilder ProfileBuilder;

        UNIT_TEST_SUITE(TUserExtractFeaturesTests);
        UNIT_TEST(EmptyRequestTest);
        UNIT_TEST(OneItemRequestTest);
        UNIT_TEST(MultipleItemsRequestTest);
        UNIT_TEST(MultipleCountersRequestTest);
        UNIT_TEST(MergeRequestsTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TUserExtractFeaturesTests);
}
