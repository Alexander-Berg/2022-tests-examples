#include "user_proto_helpers.h"


namespace NProfilePreprocessing {
    yabs::proto::Profile* TUserProtoBuilder::GetProfile() {
        return &Profile;
    }

    TString TUserProtoBuilder::GetDump() {
        TString protoString;
        Y_PROTOBUF_SUPPRESS_NODISCARD Profile.SerializeToString(&protoString);
        return protoString;
    }

    void TUserProtoBuilder::AddItem(ui32 keywordID, TVector<ui32> values, ui32 updateTime) {
        auto item = Profile.add_items();
        item->set_keyword_id(keywordID);
        item->set_update_time(updateTime);
        for (auto x: values) {
            item->add_uint_values(x);
        }
    }

    void TUserProtoBuilder::AddItem(ui32 keywordID, TVector<std::pair<ui32, ui32>> values, ui32 updateTime) {
        auto item = Profile.add_items();
        item->set_keyword_id(keywordID);
        item->set_update_time(updateTime);
        for (const auto& [x, y]: values) {
            auto pair = item->add_pair_values();
            pair->set_first(x);
            pair->set_second(y);
        }
    }

    void TUserProtoBuilder::AddCounter(ui32 counterID, TVector<ui32> keys, TVector<double> values) {
        auto cntrPack = Profile.add_packed_counters();
        cntrPack->add_counter_ids(counterID);
        auto cntrPackValues = (cntrPack->add_values())->mutable_double_values();

        for (ui64 i = 0; i < keys.size(); ++i) {
            cntrPack->add_keys(keys[i]);
            cntrPackValues->add_value(values[i]);
        }
    }

    void TUserProtoBuilder::AddQuery(
        TString queryText,
        ui64 hits, ui64 shows, ui64 clicks, ui64 selectType,
        ui64 unixUpdateTime, ui64 createTime,
        ui64 cat4, ui64 cat5, ui64 cat6,
        bool hasTargetDomainMD5
    ) {
        auto query = Profile.add_queries();

        query->set_query_text(queryText);
        query->set_hits(hits);
        query->set_shows(shows);
        query->set_clicks(clicks);
        query->set_select_type(selectType);
        query->set_unix_update_time(unixUpdateTime);
        query->set_create_time(createTime);
        query->set_cat4(cat4);
        query->set_cat5(cat5);
        query->set_cat6(cat6);
        if (hasTargetDomainMD5) {
            query->set_target_domain_md5(873248582);
        }
    }

    void TUserProtoBuilder::AddVisitState(
        ui64 start_timestamp, ui64 end_timestamp,
        ui64 hits_count, bool is_bounce
    ) {
        auto visitState = Profile.add_visit_states();

        visitState->set_start_timestamp(start_timestamp);
        visitState->set_end_timestamp(end_timestamp);
        visitState->set_hits_count(hits_count);
        visitState->set_is_bounce(is_bounce);
    }

    void TUserProtoBuilder::AddCategoryProfile(
        ui32 categoryId, ui32 interest,
        ui32 interestUpdateTime, ui32 eventTime,
        ui32 clicks, ui32 shows
    ) {
        auto categoryProfile = Profile.add_category_profiles();

        categoryProfile->set_bm_category_id(categoryId);
        categoryProfile->set_interest(interest);
        categoryProfile->set_interest_update_time(interestUpdateTime);
        categoryProfile->set_event_time(eventTime);
        categoryProfile->set_clicks(clicks);
        categoryProfile->set_shows(shows);
    }

    void TUserProtoBuilder::AddSearchQuery(
        TString searchQuery,
        ui64 regionID
    ) {
        auto searchQueryField = Profile.mutable_current_search_query();
        searchQueryField->set_query_text(searchQuery);
        searchQueryField->set_region(regionID);
    }

    void TUserProtoBuilder::AddSearchQueryHistory(
        TVector<TString> searchQueryTexts,
        TVector<ui64> regions,
        TVector<ui64> ts
    ) {
        auto searchPersProfile = Profile.search_pers_profiles().size() == 0 ? Profile.add_search_pers_profiles() : Profile.mutable_search_pers_profiles(0);
        auto filteredRecord = searchPersProfile->mutable_profile()->MutableUserHistoryState()->MutableUserHistory()->AddFilteredRecords();
        filteredRecord->MutableDescription()->SetMaxRecords(130);

        for (ui32 i = 0; i < searchQueryTexts.size(); ++i) {
            auto record = filteredRecord->AddRecords();
            record->SetQuery(searchQueryTexts[i]);
            record->SetRegion(regions[i]);
            record->SetTimestamp(ts[i]);
        }
    }

    void TUserProtoBuilder::AddSearchDocumentHistory(
        TVector<TString> titles,
        TVector<TString> urls,
        TVector<ui64> dwellTimes,
        TVector<ui64> ts,
        TVector<ui64> reqTs
    ) {
        auto searchPersProfile = Profile.search_pers_profiles().size() == 0 ? Profile.add_search_pers_profiles() : Profile.mutable_search_pers_profiles(0);
        auto filteredRecord = searchPersProfile->mutable_profile()->MutableUserHistoryState()->MutableUserHistory()->AddFilteredRecords();
        filteredRecord->MutableDescription()->SetMaxRecords(256);

        for (ui32 i = 0; i < titles.size(); ++i) {
            auto record = filteredRecord->AddRecords();
            record->SetTitle(titles[i]);
            record->SetUrl(urls[i]);
            record->SetDwelltime(dwellTimes[i]);
            record->SetTimestamp(ts[i]);
            record->SetRequestTimestamp(reqTs[i]);
        }
    }

    void TUserProtoBuilder::AddTsarVector(
        ui64 vectorId,
        TString value,
        ui32 updateTime
    ) {
        auto tsarVector = Profile.add_tsar_vectors();
        tsarVector->set_vector_id(vectorId);
        tsarVector->set_value(value);
        tsarVector->set_update_time(updateTime);
    }



    TString TypeToString(
        const NYT::TNode& type
    ) {
        if (type.GetType() == NYT::TNode::String) {
            return type.AsString();
        }
        return type["type_name"].AsString();
    }

    void CheckType(
        const NYT::TNode& type,
        const NYT::TNode& val
    ) {
        switch (val.GetType()) {
            case NYT::TNode::String: {
                UNIT_ASSERT_VALUES_EQUAL(TypeToString(type), "string");
                break;
            }
            case NYT::TNode::Uint64: {
                UNIT_ASSERT_VALUES_EQUAL(TypeToString(type), "uint64");
                break;
            }
            case NYT::TNode::Double: {
                UNIT_ASSERT_VALUES_EQUAL(TypeToString(type), "double");
                break;
            }
            case NYT::TNode::List: {
                UNIT_ASSERT_VALUES_EQUAL(TypeToString(type), "list");
                for (const auto& x: val.AsList()) {
                    CheckType(type["item"], x);
                }
                break;
            }
            default: {
                UNIT_ASSERT(false);
                break;
            }
        }
    }

    void CheckSchema(
        const NYT::TNode::TMapType& schema,
        const NYT::TNode::TMapType& data
    ) {
        UNIT_ASSERT_VALUES_EQUAL(data.size(), schema.size());
        for (const auto& [name, type]: schema) {
            const auto& val = data.at(name);
            CheckType(type, val);
        }
    }
}
