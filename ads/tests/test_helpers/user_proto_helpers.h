#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>

#include <yabs/proto/user_profile.pb.h>
#include <library/cpp/yson/node/node.h>
#include <library/cpp/testing/unittest/registar.h>


namespace NProfilePreprocessing {

    class TUserProtoBuilder {
    public:
        TUserProtoBuilder() = default;

        yabs::proto::Profile* GetProfile();
        TString GetDump();

        void AddItem(ui32 keywordID, TVector<ui32> values, ui32 updateTime=0);
        void AddItem(ui32 keywordID, TVector<std::pair<ui32, ui32>> values, ui32 updateTime=0);
        void AddCounter(ui32 counterID, TVector<ui32> keys, TVector<double> values);
        void AddQuery(
            TString queryText,
            ui64 hits=0, ui64 shows=0, ui64 clicks=0, ui64 selectType=0,
            ui64 unixUpdateTime=0, ui64 createTime=0,
            ui64 cat4=0, ui64 cat5=0, ui64 cat6=0,
            bool hasTargetDomainMD5 = false
        );
        void AddVisitState(
            ui64 start_timestamp, ui64 end_timestamp,
            ui64 hits_count, bool is_bounce
        );
        void AddCategoryProfile(
            ui32 categoryId, ui32 interest,
            ui32 interestUpdateTime, ui32 eventTime,
            ui32 clicks=0, ui32 shows=0
        );
        void AddSearchQuery(
            TString searchQuery,
            ui64 regionID
        );
        void AddSearchQueryHistory(
            TVector<TString> searchQueryTexts,
            TVector<ui64> regions,
            TVector<ui64> ts
        );
        void AddSearchDocumentHistory(
            TVector<TString> titles,
            TVector<TString> urls,
            TVector<ui64> dwellTimes,
            TVector<ui64> ts,
            TVector<ui64> reqTs
        );
        void AddTsarVector(
            ui64 vectorId,
            TString value,
            ui32 updateTime
        );

    private:
        yabs::proto::Profile Profile;
    };


    TString TypeToString(const NYT::TNode& type);
    void CheckType(const NYT::TNode& type, const NYT::TNode& val);
    void CheckSchema(const NYT::TNode::TMapType& schema, const NYT::TNode::TMapType& data);

    template<class T>
    void CheckPreprocessor(
        const T& preprocessor,
        const TProfilesPack& profilesPack,
        const NProfilePreprocessing::TArgs &args
    ) {
        auto schema = preprocessor.Schema();
        auto result = preprocessor.Parse(profilesPack, args);
        CheckSchema(schema, result);

        TStringStream s;
        preprocessor.Save(s);
        T newPreprocessor;
        newPreprocessor.Load(s);

        auto newResult = newPreprocessor.Parse(profilesPack, args);
        UNIT_ASSERT_EQUAL(result, newResult);
    }

}
