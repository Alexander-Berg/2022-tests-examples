#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/factors.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/helpers.h>

#include <util/string/split.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>


namespace NProfilePreprocessing {

    class TSomePreprocessor1: public IUserPreprocessor {
    public:
        TSomePreprocessor1() = default;
        NYT::TNode::TMapType ParseProfile(const yabs::proto::Profile&, const TArgs&, const TExtractedUserFeatures&) const override {
            return NYT::TNode::TMapType{
                {"Feature1", 123U}
            };
        }
        TExtractRequest ExtractRequest() const override {
            return TUserExtractRequest(
                {NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS},
                {1, 3}
            );
        }
        NYT::TNode::TMapType Schema() const override {
            return {
                {"Feature1", ListType("uint64")}
            };
        }
    };

    class TSomePreprocessor2: public IUserPreprocessor {
    public:
        TSomePreprocessor2() = default;
        TSomePreprocessor2(TString s): SomeParam(s) {}
        NYT::TNode::TMapType ParseProfile(const yabs::proto::Profile& profile, const TArgs& args, const TExtractedUserFeatures& extractedFeatures) const override {
            UNIT_ASSERT_UNEQUAL(extractedFeatures.Items.At(NBSData::NKeywords::KW_USER_REGION).size(), 0);
            UNIT_ASSERT_UNEQUAL(extractedFeatures.Counters.At(4), nullptr);

            auto region = extractedFeatures.Items.At(NBSData::NKeywords::KW_USER_REGION)[0]->uint_values(0);
            auto counter4FirstVal = extractedFeatures.Counters.At(4)->value(0);

            return NYT::TNode::TMapType{
                {"FirstQuery", profile.queries(0).query_text()},
                {"Counter4FirstVal", counter4FirstVal},
                {"Region", region},
                {"Timestamp", *args.Timestamp},
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
                {"Timestamp", NYT::TNode()("type_name", "uint64")},
                {"Param", NYT::TNode()("type_name", "string")},
            };
        }
        Y_SAVELOAD_JOB(SomeParam);
    private:
        TString SomeParam;
    };

    class TSomePreprocessor3: public IUserPreprocessor {
    public:
        TSomePreprocessor3() = default;
        TSomePreprocessor3(TString s): SomeParam(s) {}
        NYT::TNode::TMapType ParseProfile(const yabs::proto::Profile&, const TArgs&, const TExtractedUserFeatures&) const override {
            return NYT::TNode::TMapType{
                {"SingleWord", SomeParam},
                {"Sentence", NYT::TNode::CreateList().Add("asdf").Add("qwer")},
                {"SentencesList", NYT::TNode::CreateList()
                    .Add(NYT::TNode::CreateList().Add("asdf").Add("qwer"))
                    .Add(NYT::TNode::CreateList().Add("zxcv").Add("tyui"))
                }
            };
        }
        TExtractRequest ExtractRequest() const override {
            return TUserExtractRequest(
                {NBSData::NKeywords::KW_USER_REGION, NBSData::NKeywords::KW_CRYPTA_LONGTERM_INTERESTS},
                {123, 4}
            );
        }
        NYT::TNode::TMapType Schema() const override {
            return {
                {"SingleWord", NYT::TNode()("type_name", "string")},
                {"Sentence", ListType("string")},
                {"SentencesList", ListType(ListType("string"))}
            };
        }
        Y_SAVELOAD_JOB(SomeParam);
    private:
        TString SomeParam;
    };

    class TMyTokenizer: public NYT::ISerializableForJob {
    public:
        TMyTokenizer() = default;

        TVector<TString> operator()(const TString& s) const {
            TVector<TString> result;
            Split(s, " ", result);
            return result;
        }

        void Save(IOutputStream& stream) const override {
            Save(&stream);
        }
        void Load(IInputStream& stream) override {
            Load(&stream);
        }
        inline void Save(IOutputStream*) const { }
        inline void Load(IInputStream*) { }
    };
}
