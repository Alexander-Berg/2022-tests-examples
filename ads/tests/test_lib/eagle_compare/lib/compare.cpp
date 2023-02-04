#include <ads/bsyeti/caesar/libs/compress_vectors/decompress.h>
#include <ads/bsyeti/tests/test_lib/eagle_compare/lib/compare.h>
#include <ads/bsyeti/libs/profile/profile.h>

#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection.h>
#include <google/protobuf/message.h>
#include <google/protobuf/util/field_comparator.h>
#include <google/protobuf/util/message_differencer.h>
#include <yabs/server/proto/keywords/keywords_data.pb.h>
#include <yabs/proto/tsar.pb.h>

#include <library/cpp/logger/global/global.h>

#include <util/generic/algorithm.h>
#include <util/generic/ymath.h>
#include <util/string/builder.h>
#include <util/string/cast.h>

#include <vector>


namespace NBSYeti {
    typedef google::protobuf::Message TMessage;
    typedef google::protobuf::util::MessageDifferencer TMessageDifferencer;

    template<class T>
    static inline TString KeywordToString(const T kid) {
        return IntToString<10>(static_cast<int>(kid));
    }

    static inline TString GetDebugString(const TMessage& msg, const std::vector<TMessageDifferencer::SpecificField>& field_path) {
        auto& field = field_path.back().field;
        if (field->is_repeated()) {
            return msg.GetReflection()->GetRepeatedMessage(msg, field, field_path.back().index).DebugString();
        } else {
            // optional
            return msg.GetReflection()->GetMessage(msg, field).DebugString();
        }
    }

    static inline bool CheckRequestWithUid(const TString& request, const TString& uid) {
        if (request.EndsWith(TString{'='} + uid) || request.find("=" + uid + "&") != TString::npos) {
            return true;
        }
        return false;
    }

    template<class TIt>
    static inline void MoveItToNextMatchedUid(TIt& it, const TIt& endIt, const TVector<TString>& uids) {
        if (!uids.empty()) {
            while (it != endIt) {
                for (const auto& uid: uids) {
                    if (CheckRequestWithUid(it->GetKey(), uid)) {
                        return;
                    }
                }
                ++it;
            }
        }
    }

    static void IncInDict(THashMap<TString, ui64>* dct, const std::vector<TMessageDifferencer::SpecificField>& field_path) {
        TStringBuilder builder;
        for (auto f: field_path) {
            builder << (f.field ? f.field->name() : "?") << ".";
        }
        ++(*dct)[builder];
    }

    static void PrintStat(const THashMap<TString, ui64>& dct, TString title) {
        if (dct.size()) {
            INFO_LOG << title << "\n";
            for (const auto& [field, cnt]: dct) {
                INFO_LOG << field << ": " << cnt << "\n";
            }
        }
    }

    class TSmartReporter : public TMessageDifferencer::Reporter {
    public:
        TSmartReporter(const google::protobuf::Descriptor& msgDesc)
            : MsgDescriptor(msgDesc)
            {
            }

        void SetResultObject(TProtoCompareResult& protoCompareResult) {
            ProtoCompareResult = &protoCompareResult;
        }

        TString GetKeywordId(const TMessage& msg, const std::vector<TMessageDifferencer::SpecificField>& field_path) const {
            if (field_path.size() != 1) {
                // all keywords located in the base message at level one
                return "";
            }
            auto& field = field_path.back().field;
            auto& opts = MsgDescriptor.field(field->index())->options(); // hach to get options with extensions
            if (opts.ExtensionSize(yabs::proto::KeywordIds) == 0) {
                // at depth level = 2 is only one alowed field without this extension - is_full
                if (field == MsgDescriptor.FindFieldByName("is_full") || field == MsgDescriptor.FindFieldByName("incompleted_bigb_requests")) {
                    // this is okay, do nothing.
                } else {
                    Y_FAIL(); // unknown field. You need to write code about this.
                }
                return "";
            }
            auto kid = opts.GetExtension(yabs::proto::KeywordIds, 0);

            if (opts.ExtensionSize(yabs::proto::KeywordIds) > 1) {
                // NBSData::NKeywords::KW_EXCEPT_APPS_ON_CPI and NBSData::NKeywords::KW_INSTALLED_MOBILE_APPS - we always take first as lower one.
                for (auto i = 1; i < opts.ExtensionSize(yabs::proto::KeywordIds); ++i) {
                    kid = Min(kid, opts.GetExtension(yabs::proto::KeywordIds, 1));
                }
            }

#define CHECK_DESCRIPTOR_AND_RET_VALUE(keyword_id, field_name, message_type, keyword_field_name)                                                                        \
            if (kid == NBSData::NKeywords::keyword_id && field == MsgDescriptor.FindFieldByName(#field_name)) {                                                                    \
                const auto& curMsg = google::protobuf::DynamicCastToGenerated<const TPublicProfileProto>(&msg)->field_name()[field_path.back().index]; \
                if (NBSData::NKeywords::keyword_id == NBSData::NKeywords::KW_ANY) {                                                                                                           \
                    return KeywordToString(curMsg.keyword_field_name());                                                                                                \
                }                                                                                                                                                       \
                return KeywordToString(NBSData::NKeywords::keyword_id) + "_" + KeywordToString(curMsg.keyword_field_name()) + "_" + #field_name;                                   \
            }

#define FAIL_UNKNOWN_FIELD_KEYWORD(keyword_id)                                  \
            if (kid == NBSData::NKeywords::keyword_id){                                    \
                Y_FAIL(); /* unknown field. You need to write code about this.*/\
                return "";                                                      \
            }

            CHECK_DESCRIPTOR_AND_RET_VALUE(KW_ANY, items, ProfileItem, keyword_id)
            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_ANY, server_side_cookies, TCookie, GetKeywordId)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_ANY)

            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_SITE_SEARCH_TEXT, queries, Query, select_type)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_SITE_SEARCH_TEXT)

            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_QUERY_CANDIDATES, query_candidates, Query, select_type)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_QUERY_CANDIDATES)

            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_BT_COUNTER, counters, Counter, counter_id)
            // TODO: logic about counter packs need to be here
            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_BT_COUNTER, lm_features, LmFeature, counter_id)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_BT_COUNTER)

            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_DELAYED_COUNTER_UPDATES, delayed_counter_updates, TCounterUpdate, counter_id)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_DELAYED_COUNTER_UPDATES)

            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_USER_DIRECT_BANNERS, banners, Banner, select_type)
            else CHECK_DESCRIPTOR_AND_RET_VALUE(KW_USER_DIRECT_BANNERS, legacy_banners, BannerLegacy, select_type)
            else FAIL_UNKNOWN_FIELD_KEYWORD(KW_USER_DIRECT_BANNERS)

#undef FAIL_UNKNOWN_FIELD_KEYWORD
#undef CHECK_DESCRIPTOR_AND_RET_VALUE

            return KeywordToString(kid);
        }

        void ReportAdded(const TMessage&, const TMessage& message2, const std::vector<TMessageDifferencer::SpecificField>& field_path) override {
            auto keywordId = GetKeywordId(message2, field_path);
            if (keywordId.empty()) {
                return;
            }
            IncInDict(&ProtoCompareResult->Added, field_path);
            auto& keywordCheckResult = ProtoCompareResult->Keywords[keywordId];
            ++ProtoCompareResult->TotalCount;
            ++keywordCheckResult.TotalCount;
            ++ProtoCompareResult->FailCount;
            ++keywordCheckResult.FailCount;
            keywordCheckResult.AddedKeywords.push_back(GetDebugString(message2, field_path));
        }

        void ReportDeleted(const TMessage& message1, const TMessage&, const std::vector<TMessageDifferencer::SpecificField>& field_path) override {
            auto keywordId = GetKeywordId(message1, field_path);
            if (keywordId.empty()) {
                return;
            }
            IncInDict(&ProtoCompareResult->Deleted, field_path);
            auto& keywordCheckResult = ProtoCompareResult->Keywords[keywordId];
            ++ProtoCompareResult->TotalCount;
            ++keywordCheckResult.TotalCount;
            ++ProtoCompareResult->FailCount;
            ++keywordCheckResult.FailCount;
            keywordCheckResult.DeletedKeywords.push_back(GetDebugString(message1, field_path));
        }

        void ReportModified(const TMessage& message1, const TMessage& message2, const std::vector<TMessageDifferencer::SpecificField>& field_path) override {
            auto keywordId1 = GetKeywordId(message1, field_path);
            auto keywordId2 = GetKeywordId(message2, field_path);
            if (keywordId1.empty() && keywordId2.empty()) {
                return;
            } else if (keywordId1.empty() || keywordId2.empty()) {
                Y_FAIL(); // life not prepare me for that
            } else {
                if (keywordId1 == keywordId2) {
                    auto& keywordCheckResult = ProtoCompareResult->Keywords[keywordId1];
                    ++ProtoCompareResult->TotalCount;
                    ++keywordCheckResult.TotalCount;
                    ++ProtoCompareResult->FailCount;
                    ++keywordCheckResult.FailCount;
                    keywordCheckResult.DeletedKeywords.push_back(GetDebugString(message1, field_path));
                    keywordCheckResult.AddedKeywords.push_back(GetDebugString(message2, field_path));
                } else {
                    IncInDict(&ProtoCompareResult->Modified, field_path);
                    auto& keywordCheckResult1 = ProtoCompareResult->Keywords[keywordId1];
                    auto& keywordCheckResult2 = ProtoCompareResult->Keywords[keywordId2];
                    ProtoCompareResult->TotalCount += 2;
                    ++keywordCheckResult1.TotalCount;
                    ++keywordCheckResult2.TotalCount;
                    ProtoCompareResult->FailCount += 2;
                    ++keywordCheckResult1.FailCount;
                    ++keywordCheckResult2.FailCount;
                    keywordCheckResult1.DeletedKeywords.push_back(GetDebugString(message1, field_path));
                    keywordCheckResult2.AddedKeywords.push_back(GetDebugString(message2, field_path));
                }
            }
        }

        void ReportMatched(const TMessage& message1, const TMessage&, const std::vector<TMessageDifferencer::SpecificField>& field_path) override {
            auto keywordId = GetKeywordId(message1, field_path);
            if (keywordId.empty()) {
                return;
            }
            auto& keywordCheckResult = ProtoCompareResult->Keywords[keywordId];
            ++ProtoCompareResult->TotalCount;
            ++keywordCheckResult.TotalCount;
        }

    private:
        TProtoCompareResult* ProtoCompareResult = 0;
        const google::protobuf::Descriptor& MsgDescriptor;
    };

    void ModifyProto(TPublicProfileProto& msg) {
        static TVector<TString> sortOrder = {
            "id", "keyword_id", "KeywordId", "time", "update_time", "create_time",
            "vector_id", "value", "bm_category_id", "impression_id", "select_type", "counter_id", "offer_id_md5",
            "timestamp", "weight", "key", "domain_md5", "query_id", "start_timestamp", "page_id",
            "crypta_graph_distance", "uniq_id", "id_type", "user_id", "ItemId", "uint_values"
        };
        static THashSet<TString> skipFields = {
            "packed_counters"
        };
        NBSYeti::UnpackCountersInProto(msg.mutable_packed_counters(), msg.mutable_counters());

        static THashMap<TString, int> inheritFieldsSortOrderMap;
        constexpr i32 InvalidKeywordID = Max<i32>();

        // only one time run
        if (inheritFieldsSortOrderMap.empty()) {
            for (size_t i = 0; i < sortOrder.size(); ++i) {
                inheritFieldsSortOrderMap[sortOrder[i]] = i;
            }
        }

        // this keywords is not stable yet, so need to skip them
        static THashSet<i32> forbiddenKeywords = {NBSData::NKeywords::KW_PROFILE_LIFE_TIME};

        // queries
        for (auto& query: *msg.mutable_queries()) {
            query.set_unix_update_time(0);
            if (query.has_predicted_vw_models_values()) {
                query.mutable_predicted_vw_models_values()->set_calc_timestamp(0);
            }
        }
        for (auto& query: *msg.mutable_query_candidates()) {
            query.set_unix_update_time(0);
            if (query.has_predicted_vw_models_values()) {
                query.mutable_predicted_vw_models_values()->set_calc_timestamp(0);
            }
        }

        // items
        for (auto& item: *msg.mutable_items()) {
            // remove revision field if =0
            if (item.has_revision() && item.revision() == 0) {
                item.clear_revision();
            }
            // remove deleted if =false
            if (item.has_deleted() && item.deleted() == false) {
                // if deleted=True in eagle answer - this is NOT GOOD SITUATION IN PRODUCTION!
                item.clear_deleted();
            }
            if (forbiddenKeywords.contains(item.keyword_id())) {
                item.Clear();
                item.set_keyword_id(InvalidKeywordID); // will be removed later
            }
        }


        const auto* descriptor = msg.GetDescriptor();
        const auto* reflection = msg.GetReflection();

        for (int i = 0; i < descriptor->field_count(); ++i) {
            const auto* field = descriptor->field(i);
            if (skipFields.contains(field->name())) {
                continue;
            }
            if (field->is_repeated() && field->cpp_type() == NProtoBuf::FieldDescriptor::CPPTYPE_MESSAGE) {
                size_t msgCount = reflection->FieldSize(msg, field);
                if (msgCount < 2) {
                    continue;
                }
                TVector<size_t> indexes(Reserve(msgCount));
                TVector<size_t> posMap(Reserve(msgCount));
                for (size_t i = 0; i < msgCount; ++i) {
                    indexes.push_back(i);
                    posMap.push_back(i);
                }
                auto mutableReflectionFieldRef = reflection->GetMutableRepeatedFieldRef<TMessage>(&msg, field);
                TVector<std::pair<const NProtoBuf::FieldDescriptor*, int>> inheritFields;
                bool haveOneAtLeast = false;
                for (auto i = 0; i < field->message_type()->field_count(); ++i) {
                    auto* inheritField = field->message_type()->field(i);
                    auto it = inheritFieldsSortOrderMap.find(inheritField->name());
                    if (it == inheritFieldsSortOrderMap.end()) {
                        // do nothing
                    } else {
                        haveOneAtLeast = true;
                        inheritFields.push_back(std::make_pair(inheritField, it->second));
                    }
                }
                if (!haveOneAtLeast) {
                    WARNING_LOG << "Message field " << field->name() << " have no one known sorting field! Trying to sort using first field as sort key." << "\n";
                    WARNING_LOG << "Sorting using " << field->message_type()->field(0)->name() << " field" << "\n";
                    inheritFields.push_back(std::make_pair(field->message_type()->field(0), 0));
                }
                Sort(inheritFields, [](const auto& lhs, const auto& rhs) {
                    return lhs.second < rhs.second;
                });

                StableSort(indexes.begin(), indexes.end(), [&reflection, &inheritFields, &msg, &field](const auto lInd, const auto rInd) {
#define COMPARE_WITH_TYPE(CPP_TYPE, CastFunc)                                  \
    if (inheritField->cpp_type() == NProtoBuf::FieldDescriptor::CPP_TYPE) {    \
        auto val1 = locReflection->CastFunc(*lhs, inheritField);               \
        auto val2 = locReflection->CastFunc(*rhs, inheritField);               \
        if (val1 != val2) {                                                    \
            return val1 < val2;                                                \
        }                                                                      \
    }

#define COMPARE_FIRST_WITH_TYPE(CPP_TYPE, CastFunc)                            \
    if (inheritField->cpp_type() == NProtoBuf::FieldDescriptor::CPP_TYPE) {    \
        auto val1 = locReflection->CastFunc(*lhs, inheritField, 0);            \
        auto val2 = locReflection->CastFunc(*rhs, inheritField, 0);            \
        if (val1 != val2) {                                                    \
            return val1 < val2;                                                \
        }                                                                      \
    }

                    auto* lhs = reflection->MutableRepeatedMessage(&msg, field, lInd);
                    auto* rhs = reflection->MutableRepeatedMessage(&msg, field, rInd);
                    auto* locReflection = lhs->GetReflection();
                    for (auto& pairElem : inheritFields) {
                        const auto* inheritField = pairElem.first;
                        if (inheritField->is_repeated()) {
                            bool hasLeft = locReflection->FieldSize(*lhs, inheritField) > 0;
                            bool hasRight = locReflection->FieldSize(*rhs, inheritField) > 0;
                            if (hasLeft && hasRight) {
                                COMPARE_FIRST_WITH_TYPE(CPPTYPE_INT32,  GetRepeatedInt32)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_INT64,  GetRepeatedInt64)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_UINT32, GetRepeatedUInt32)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_UINT64, GetRepeatedUInt64)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_FLOAT,  GetRepeatedFloat)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_DOUBLE, GetRepeatedDouble)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_BOOL,   GetRepeatedBool)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_ENUM, GetRepeatedEnumValue)
                                else COMPARE_FIRST_WITH_TYPE(CPPTYPE_STRING, GetRepeatedString)
                            } else if (hasLeft) {
                                return false;
                            } else if (hasRight) {
                                return true;
                            }

                        } else {
                            bool hasLeft = locReflection->HasField(*lhs, inheritField);
                            bool hasRight = locReflection->HasField(*rhs, inheritField);
                            if (hasLeft && hasRight) {
                                COMPARE_WITH_TYPE(CPPTYPE_INT32,  GetInt32)
                                else COMPARE_WITH_TYPE(CPPTYPE_INT64,  GetInt64)
                                else COMPARE_WITH_TYPE(CPPTYPE_UINT32, GetUInt32)
                                else COMPARE_WITH_TYPE(CPPTYPE_UINT64, GetUInt64)
                                else COMPARE_WITH_TYPE(CPPTYPE_FLOAT,  GetFloat)
                                else COMPARE_WITH_TYPE(CPPTYPE_DOUBLE, GetDouble)
                                else COMPARE_WITH_TYPE(CPPTYPE_BOOL,   GetBool)
                                else COMPARE_WITH_TYPE(CPPTYPE_ENUM, GetEnumValue)
                                else COMPARE_WITH_TYPE(CPPTYPE_STRING, GetString)
                            } else if (hasLeft) {
                                return false;
                            } else if (hasRight) {
                                return true;
                            }
                        }
                    }
                    return false; // if we here, objects are equal
#undef COMPARE_WITH_TYPE
                });

                // final sort
                for (size_t i = 0; i < msgCount; ++i) {
                    size_t ind = i;
                    while (posMap[indexes[ind]] != ind) {
                        mutableReflectionFieldRef.SwapElements(ind, posMap[indexes[ind]]);
                        posMap[indexes[ind]] = ind;
                        posMap[i] = indexes[ind];
                        ind = indexes[ind];
                    }
                }
            }
        }
        // remove marked items
        while (0 != msg.items_size() && msg.items(msg.items_size() - 1).keyword_id() == InvalidKeywordID) {
            msg.mutable_items()->RemoveLast();
        }

        // we need custom code to sort each counter keys and values
        {
            for (auto& counter : *msg.mutable_counters()) {
                size_t counterCount = counter.key().size();
                Y_ENSURE(counterCount == (size_t)(counter.value().size()));
                if (counterCount < 2) {
                    continue;
                }
                TVector<size_t> indexes(Reserve(counterCount));
                TVector<size_t> posMap(Reserve(counterCount));
                for (size_t i = 0; i < counterCount; ++i) {
                    indexes.push_back(i);
                    posMap.push_back(i);
                }

                StableSort(indexes.begin(), indexes.end(), [&counter](const auto lInd, const auto rInd) {
                    return counter.key()[lInd] < counter.key()[rInd];
                });

                for (size_t i = 0; i < counterCount; ++i) {
                    size_t ind = i;
                    while (posMap[indexes[ind]] != ind) {
                        counter.mutable_key()->SwapElements(ind, posMap[indexes[ind]]);
                        counter.mutable_value()->SwapElements(ind, posMap[indexes[ind]]);
                        posMap[indexes[ind]] = ind;
                        posMap[i] = indexes[ind];
                        ind = indexes[ind];
                    }
                }
            }
        }
    }

    static TVector<float> DecompressVector(const yabs::proto::NTsar::TProfileTsarVector& tsarVector) {
        return NCSR::DecompressVector(
            tsarVector.value().data(),
            tsarVector.value().size(),
            NCSR::TTsarModelDataFormat(
                static_cast<NCSR::ETsarVectorFactorType>(tsarVector.element_size()),
                tsarVector.min(),
                tsarVector.max())
        );
    }

    static TString Vector2String(const yabs::proto::NTsar::TProfileTsarVector& tsarVector) {
        auto vector = DecompressVector(tsarVector);
        TStringStream str;
        str << "[";
        for (auto v : vector) {
            str << v << ",";
        }
        str.Str().pop_back();
        str << "]";
        return str.Str();
    }

    static bool CompareTsarVectors(TPublicProfileProto& msg1, TPublicProfileProto& msg2, TProtoCompareResult& protoCompareResult) {
        bool everyIsGood = true;
        auto it1 = msg1.tsar_vectors().begin();
        auto it2 = msg2.tsar_vectors().begin();
        if (msg1.tsar_vectors().empty() && msg2.tsar_vectors().empty()) {
            return true;
        }
        auto& keywordCheckResult = protoCompareResult.Keywords[KeywordToString(NBSData::NKeywords::KW_USER_TSAR_VECTORS)];
        while (it1 != msg1.tsar_vectors().end() || it2 != msg2.tsar_vectors().end()) {
            ++protoCompareResult.TotalCount;
            ++keywordCheckResult.TotalCount;

            bool missed = false;
            bool added = false;

            if (it1 == msg1.tsar_vectors().end()) {
                added = true;

            } else if (it2 == msg2.tsar_vectors().end()) {
                missed = true;

            } else if (it1->vector_id() != it2->vector_id()) {
                if (it1->vector_id() < it2->vector_id()) {
                    missed = true;
                } else {
                    added = true;
                }

            } else {
                Y_ENSURE(it1->element_size() == it2->element_size(), it1->element_size() << "!=" << it2->element_size());
                bool isGood = true;
                isGood &= (it1->vector_size() == it2->vector_size());
                isGood &= (it1->update_time() == it2->update_time());
                isGood &= (it1->main() == it2->main());
                isGood &= (it1->source_uniq_index() == it2->source_uniq_index());

                isGood &= (Abs(it1->min() - it2->min()) < 0.01);
                isGood &= (Abs(it1->max() - it2->max()) < 0.01);

                if (isGood) {
                    TVector<float> vector1 = DecompressVector(*it1);
                    TVector<float> vector2 = DecompressVector(*it2);
                    for (ui64 i = 0; i < it1->vector_size(); ++i) {
                        if (Abs(vector1[i] - vector2[i]) > 0.01) {
                            isGood = false;
                            break;
                        }
                    }
                }
                if (isGood) {
                    // everything is good, keywords are the same.
                    // maybe later add some option to compare and print comparable things ?
                    ++it1;
                    ++it2;
                } else {
                    // not good, we have another vector.
                    missed = true;
                    added = true;
                }
            }

            if (missed) {
                ++protoCompareResult.FailCount;
                ++keywordCheckResult.FailCount;
                Y_ENSURE(it1 != msg1.tsar_vectors().end());
                keywordCheckResult.DeletedKeywords.push_back(Vector2String(*it1));
                ++it1;
                everyIsGood = false;
            }
            if (added) {
                ++protoCompareResult.FailCount;
                ++keywordCheckResult.FailCount;
                Y_ENSURE(it2 != msg2.tsar_vectors().end());
                keywordCheckResult.AddedKeywords.push_back(Vector2String(*it2));
                ++it2;
                everyIsGood = false;
            }
            if (missed && added) {
                --protoCompareResult.FailCount;
                --keywordCheckResult.FailCount;
            }
        }
        return everyIsGood;
    }

    bool CompareProtos(const TString& msg1, const TString& msg2, TProtoCompareResult& protoCompareResult) {
        // TODO : check that no unknown fields in deserialized messages
        TPublicProfileProto pmsg1;
        Y_PROTOBUF_SUPPRESS_NODISCARD pmsg1.ParseFromString(msg1);
        TPublicProfileProto pmsg2;
        Y_PROTOBUF_SUPPRESS_NODISCARD pmsg2.ParseFromString(msg2);
        return CompareProtos(std::move(pmsg1), std::move(pmsg2), protoCompareResult);
    }

    bool CompareProtos(TPublicProfileProto msg1, TPublicProfileProto msg2, TProtoCompareResult& protoCompareResult) {
        // ensure we have not packed counters (not ready yet) and no have common_queries (depricated)
        Y_ENSURE(msg1.common_queries().empty());
        Y_ENSURE(msg2.common_queries().empty());
        NBSYeti::UnpackCountersInProto(msg1.mutable_packed_counters(), msg1.mutable_counters());
        NBSYeti::UnpackCountersInProto(msg2.mutable_packed_counters(), msg2.mutable_counters());

        static TMessageDifferencer commonDiffTool;
        static TMessageDifferencer countersDiffTool;
        static auto msgDesc = msg1.GetDescriptor();
        static google::protobuf::util::DefaultFieldComparator customCommonComparator;
        static google::protobuf::util::DefaultFieldComparator customCounterComparator;
        static TSmartReporter customReporter(*msgDesc);
        static std::vector<const google::protobuf::FieldDescriptor*> fieldsToCompare;
        static bool diffToolInitialized = false;

        // we need initialized difftool that some fields we ignore
        if (!diffToolInitialized) {
            // we compare tsar_vectors by custom code, because of parsing bytes to another proto msg and floats accuracy
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("tsar_vectors"));
            // we compare counters with another settings
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("counters"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("packed_counters"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("lm_features"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("delayed_counter_updates"));
            // deprecated fields, we can ignore
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("common_queries"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("aura_recommendations"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("aura_vectors"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("aura_post_events"));
            commonDiffTool.IgnoreField(msgDesc->FindFieldByName("aura_counters"));

            // set custom reporter and customize comparator and some options for common difftool
            customCommonComparator.set_float_comparison(google::protobuf::util::DefaultFieldComparator::FloatComparison::APPROXIMATE);
            customCommonComparator.SetDefaultFractionAndMargin(0.0, 0.01);
            commonDiffTool.set_field_comparator(&customCommonComparator);
            commonDiffTool.ReportDifferencesTo(&customReporter);
            commonDiffTool.set_report_matches(true);
            commonDiffTool.set_report_moves(false);

            // set custom reporter and customize comparator and some options for couter difftool
            customCounterComparator.set_float_comparison(google::protobuf::util::DefaultFieldComparator::FloatComparison::APPROXIMATE);
            customCounterComparator.SetDefaultFractionAndMargin(0.0, 0.000001);
            countersDiffTool.set_field_comparator(&customCounterComparator);
            countersDiffTool.ReportDifferencesTo(&customReporter);
            countersDiffTool.set_report_matches(true);
            countersDiffTool.set_report_moves(false);
            fieldsToCompare = {
                msgDesc->FindFieldByName("counters"),
                msgDesc->FindFieldByName("packed_counters"),
                msgDesc->FindFieldByName("lm_features"),
                msgDesc->FindFieldByName("delayed_counter_updates")
            };
            diffToolInitialized = true;
        }
        customReporter.SetResultObject(protoCompareResult);

        // we need to prepare protos, f.e. remove some fields
        ModifyProto(msg1);
        ModifyProto(msg2);

        bool cmpResult = true;

        // we must compare protobuf is_full field - this is only field at depth level 1 without KeywordIds extension
        if (msg1.is_full() != msg2.is_full()) {
            INFO_LOG << "Messages not both full! Message1: if_full=" << msg1.is_full() <<  ", Message2: if_full=" << msg2.is_full() << "\n";
            cmpResult = false;
        }
        if (msg1.create_time() != msg2.create_time()) {
            INFO_LOG << "Messages not both full! Message1: create_time=" << msg1.create_time() <<  ", Message2: create_time=" << msg2.create_time() << "\n";
            cmpResult = false;
        }
        if (msg1.incompleted_bigb_requests() != msg2.incompleted_bigb_requests()) {
            INFO_LOG << "Messages not both full! Message1: incompleted_bigb_requests=" << msg1.incompleted_bigb_requests()
                     <<  ", Message2: incompleted_bigb_requests=" << msg2.incompleted_bigb_requests() << "\n";
            cmpResult = false;
        }


        cmpResult &= commonDiffTool.Compare(msg1, msg2);
        cmpResult &= countersDiffTool.CompareWithFields(msg1, msg2, fieldsToCompare, fieldsToCompare);
        cmpResult &= CompareTsarVectors(msg1, msg2, protoCompareResult);

        return cmpResult;
    }

    bool CompareAnswers(const NTestsResult::TTests& testPack1, const NTestsResult::TTests& testPack2, const TVector<TString>& uids) {
        THashMap<TString, const NTestsResult::TTests::TTest*> tests1;
        THashMap<TString, const NTestsResult::TTests::TTest*> tests2;
        TVector<TString> notDoneTests;
        TVector<TString> onlyNewTests;
        TVector<TString> testsToCheck;
        bool isGoodGlobal = true;
        {
            // we need to parse tests to tests1/2 variables and get not done tests and new created tests.
            for (const auto& testModule: testPack1.GetTests()) {
                const auto& testName = testModule.GetTestName();
                tests1[testName] = &testModule;
            }
            for (const auto& testModule: testPack2.GetTests()) {
                const auto& testName = testModule.GetTestName();
                tests2[testName] = &testModule;
                if (!tests1.contains(testName)) {
                    onlyNewTests.push_back(testName);
                } else {
                    testsToCheck.push_back(testName);
                }
            }
            for (const auto& testModule: testPack1.GetTests()) {
                const auto& testName = testModule.GetTestName();
                if (!tests2.contains(testName)) {
                    notDoneTests.push_back(testName);
                }
            }
        }
        {
            // print tests we cant compare
            // if uids is not empty, code will return false anyway
            if (!notDoneTests.empty()) {
                isGoodGlobal = false;
                for (const auto& testName : notDoneTests) {
                    INFO_LOG << "We cannot compare requests from test module " << testName << ": test results exist only in canondata and not have tested last run!" << "\n";
                }
            }
            if (!onlyNewTests.empty()) {
                isGoodGlobal = false;
                for (const auto& testName : onlyNewTests) {
                    INFO_LOG << "We cannot compare requests from test module " << testName << ": test results not found in canondata!" << "\n";
                }
            }
        }
        {
            // compare exising tests
            for (const auto& testName : testsToCheck) {
                size_t notFoundCases = 0;
                size_t onlyNewCases = 0;
                size_t failedCases = 0;
                size_t totalCases = 0;
                TProtoCompareResult globalTestResults;
                // test cases must be in the sorted order, always, in each test run.
                auto it1 = tests1[testName]->GetRequests().begin();
                auto it2 = tests2[testName]->GetRequests().begin();

                MoveItToNextMatchedUid(it1, tests1[testName]->GetRequests().end(), uids);
                MoveItToNextMatchedUid(it2, tests2[testName]->GetRequests().end(), uids);

                while (it1 != tests1[testName]->GetRequests().end() && it2 != tests2[testName]->GetRequests().end()) {
                    ++totalCases;

                    if (it1->GetKey() < it2->GetKey()) {
                        ++notFoundCases;
                        INFO_LOG << "Not found tests result for canonized case: " << it1->GetKey() << "\n";
                        ++it1;
                        MoveItToNextMatchedUid(it1, tests1[testName]->GetRequests().end(), uids);

                    } else if (it1->GetKey() > it2->GetKey()) {
                        ++onlyNewCases;
                        INFO_LOG << "Not found canonical data for case: " << it2->GetKey() << "\n";
                        ++it2;
                        MoveItToNextMatchedUid(it2, tests2[testName]->GetRequests().end(), uids);

                    } else {
                        TProtoCompareResult testResult;
                        const auto& msg1 = it1->GetAnswer();
                        const auto& msg2 = it2->GetAnswer();
                        bool testGood = CompareProtos(msg1, msg2, testResult);
                        Y_ENSURE(testGood == (testResult.FailCount == 0));
                        if (testGood) {
                            // do nothing, everything is great. Move on!
                        } else {
                            ++failedCases;
                            // TODO: control how many items of each keywordId/Profile or how many profiles will be printed.
                            for (auto& [keyword, pack]: testResult.Keywords) {
                                if (pack.FailCount > 0) {
                                    // simple crutch to solve BIGB-1120
                                    if (!pack.DeletedKeywords.empty() && !pack.AddedKeywords.empty() && pack.AddedKeywords.size() == pack.DeletedKeywords.size()) {
                                        bool allGood = true;
                                        for (size_t i = 0; i < pack.AddedKeywords.size(); ++i) {
                                            if (pack.AddedKeywords[i] != pack.DeletedKeywords[i]) {
                                                allGood = false;
                                                break;
                                            }
                                        }
                                        if (allGood) {
                                            pack.FailCount = 0;
                                            pack.FailedProfilesCount = 0;
                                            pack.AddedKeywords = {};
                                            pack.DeletedKeywords = {};
                                            continue;
                                        }
                                    }
                                    INFO_LOG << "--------" << "\n";
                                    INFO_LOG << it1->GetKey() << " - " << testName << "\n";
                                    INFO_LOG << keyword << " - is bad" << "\n";
                                    if (!pack.DeletedKeywords.empty()) {
                                        INFO_LOG << "missed " << pack.DeletedKeywords.size() << " expected recs (total " << pack.TotalCount << " recs)" << "\n";
                                        TStringBuilder sb;
                                        for (const auto& rec : pack.DeletedKeywords) {
                                            sb << "{" << rec << "}, ";
                                        }
                                        INFO_LOG << "[" << sb << "]" << "\n";
                                    }
                                    if (!pack.AddedKeywords.empty()) {
                                        INFO_LOG << "got " << pack.AddedKeywords.size() << " unexpected recs (total " << pack.TotalCount << " recs)" << "\n";
                                        TStringBuilder sb;
                                        for (const auto& rec : pack.AddedKeywords) {
                                            sb << "{" << rec << "}, ";
                                        }
                                        INFO_LOG << "[" << sb << "]" << "\n";
                                    }
                                    INFO_LOG << "Keyword " << keyword
                                              << " of request " << it1->GetKey()
                                              << ": total " << pack.FailCount << " failed of " << pack.TotalCount
                                              << " keywords" << "\n";
                                }
                            }
                        }
                        {
                            globalTestResults.TotalCount += testResult.TotalCount;
                            globalTestResults.FailCount += testResult.FailCount;
                            for (const auto& [k, v]: testResult.Added) {
                                globalTestResults.Added[k] += v;
                            }
                            for (const auto& [k, v]: testResult.Deleted) {
                                globalTestResults.Deleted[k] += v;
                            }
                            for (const auto& [k, v]: testResult.Modified) {
                                globalTestResults.Modified[k] += v;
                            }
                            for (const auto& [keyword, pack] : testResult.Keywords) {
                                auto& globalPack = globalTestResults.Keywords[keyword];
                                globalPack.ProfilesCount += 1;
                                globalPack.TotalCount += pack.TotalCount;
                                globalPack.FailCount += pack.FailCount;
                                if (pack.FailCount > 0) {
                                    globalPack.FailedProfilesCount += 1;
                                }
                            }
                        }
                        isGoodGlobal &= testGood;
                        ++it1;
                        ++it2;
                        MoveItToNextMatchedUid(it1, tests1[testName]->GetRequests().end(), uids);
                        MoveItToNextMatchedUid(it2, tests2[testName]->GetRequests().end(), uids);
                    }

                }
                while (it2 != tests2[testName]->GetRequests().end()) {
                    ++onlyNewCases;
                    INFO_LOG << "Not found canonical data for run case: " << it2->GetKey() << "\n";
                    ++it2;
                    MoveItToNextMatchedUid(it2, tests2[testName]->GetRequests().end(), uids);
                }
                while (it1 != tests1[testName]->GetRequests().end()) {
                    ++notFoundCases;
                    INFO_LOG << "Not found tests result for canonized case: " << it1->GetKey() << "\n";
                    ++it1;
                    MoveItToNextMatchedUid(it1, tests1[testName]->GetRequests().end(), uids);
                }
                {
                    INFO_LOG << "-----" << "\n";
                    INFO_LOG << "Test case " << testName << ":" << "\n";
                    TVector<TString> keywords(Reserve(globalTestResults.Keywords.size()));
                    for (const auto& [key, value] : globalTestResults.Keywords) {
                        Y_UNUSED(value);
                        keywords.push_back(key);
                    }
                    Sort(keywords);
                    THashMap<TString, std::pair<ui64, ui64>> notFailedKeywords;
                    TVector<TString> notFailedKeywordsList(Reserve(keywords.size()));
                    for (const auto& keyword: keywords) {
                        auto& pack = globalTestResults.Keywords[keyword];
                        if (pack.FailCount == 0u) {
                            Y_ENSURE(pack.FailedProfilesCount == 0u);
                            notFailedKeywordsList.push_back(keyword);
                            notFailedKeywords.emplace(keyword, std::make_pair(pack.TotalCount, pack.ProfilesCount));
                            continue;
                        }
                        INFO_LOG << keyword << " - failed: " << pack.FailCount << "/" << pack.TotalCount
                                  << " keywords (failed in " << pack.FailedProfilesCount << "/" << pack.ProfilesCount << " profiles)" << "\n";
                    }
                    INFO_LOG << "_total - failed " << globalTestResults.FailCount << "/" << globalTestResults.TotalCount
                             << " keywords (failed in " << failedCases << "/" << totalCases
                             << " profiles) plus " << onlyNewCases << " unexpected profiles and " << notFoundCases << " not found profiles)" << "\n";
                    for (const auto& keyword: notFailedKeywordsList) {
                        auto& rec = notFailedKeywords.at(keyword);
                        INFO_LOG << "Not failed: " << keyword << " - " << rec.first << " keywords in " << rec.second << " profiles" << "\n";
                    }
                    PrintStat(globalTestResults.Added, "==Added==");
                    PrintStat(globalTestResults.Deleted, "==Deleted==");
                    PrintStat(globalTestResults.Modified, "==Modified==");
                }
                isGoodGlobal &= (notFoundCases == 0);
                isGoodGlobal &= (onlyNewCases == 0);
            }
        }
        return isGoodGlobal;
    }
}
