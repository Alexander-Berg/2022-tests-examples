#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/factors.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/helpers.h>


namespace NProfilePreprocessing {
    class TSomePreprocessor: public IUserPreprocessor {
    public:
        TSomePreprocessor() = default;
        TSomePreprocessor(TString s): SomeParam(s) {}
        NYT::TNode::TMapType ParseProfile(const yabs::proto::Profile& profile, const TArgs& args, const TExtractedUserFeatures& extractedFeatures) const override {
            UNIT_ASSERT_UNEQUAL(extractedFeatures.Items.At(NBSData::NKeywords::KW_USER_REGION).size(), 0);
            UNIT_ASSERT_UNEQUAL(extractedFeatures.Counters.At(4), nullptr);
            
            auto region = extractedFeatures.Items.At(NBSData::NKeywords::KW_USER_REGION)[0]->uint_values(0);
            auto counter4FirstVal = extractedFeatures.Counters.At(4)->value(0);

            return NYT::TNode::TMapType{
                {"FirstQuery", profile.queries(0).query_text()},
                {"Counter4FirstVal", counter4FirstVal},
                {"Region", region},
                {"TimestampParsed", *args.Timestamp},
                {"Param", SomeParam}
            };
        }
        TExtractRequest ExtractRequest() const override {
            return TUserExtractRequest(
                {NBSData::NKeywords::KW_USER_REGION},
                {2, 4}
            );
        }
        NYT::TNode::TMapType Schema() const override {
            return {
                {"FirstQuery", NYT::TNode()("type_name", "string")},
                {"Counter4FirstVal", NYT::TNode()("type_name", "double")},
                {"Region", NYT::TNode()("type_name", "uint64")},
                {"TimestampParsed", NYT::TNode()("type_name", "uint64")},
                {"Param", NYT::TNode()("type_name", "string")},
            };
        }
        Y_SAVELOAD_JOB(SomeParam);
    private:
        TString SomeParam;
    };
}
