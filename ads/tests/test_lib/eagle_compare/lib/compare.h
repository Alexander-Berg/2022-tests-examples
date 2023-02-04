#pragma once

#include <ads/bsyeti/tests/test_lib/eagle_answers_proto/answers.pb.h>
#include <yabs/proto/user_profile.pb.h>

#include <util/generic/hash.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>


namespace NBSYeti {
    typedef yabs::proto::Profile TPublicProfileProto;

    struct TKeywordCheckResult {
        ui64 TotalCount = 0;
        ui64 FailCount = 0;
        ui64 ProfilesCount = 0;
        ui64 FailedProfilesCount = 0;
        TVector<TString> AddedKeywords = {};
        TVector<TString> DeletedKeywords = {};
        // TODO: think about modified keywords, not just added and removed
    };

    struct TProtoCompareResult {
        ui64 TotalCount = 0;
        ui64 FailCount = 0;
        THashMap<TString, ui64> Added = {};
        THashMap<TString, ui64> Modified = {};
        THashMap<TString, ui64> Deleted = {};
        THashMap<TString, TKeywordCheckResult> Keywords = {};
    };

    void ModifyProto(TPublicProfileProto& msg);

    bool CompareProtos(const TString& msg1, const TString& msg2, TProtoCompareResult& protoCompareResult);

    bool CompareProtos(TPublicProfileProto msg1, TPublicProfileProto msg2, TProtoCompareResult& protoCompareResult);

    bool CompareAnswers(const NTestsResult::TTests& testPack1, const NTestsResult::TTests& testPack2, const TVector<TString>& uids);
}
