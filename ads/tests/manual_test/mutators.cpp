#include "mutators.h"

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/text_format.h>

namespace NBSYeti {
    template <class TMap>
    void EraseRandom(TMap& map, i64 seed) {
        if (map.Size() != 0) {
            auto iter = map.begin();
            i64 index = std::abs(seed % map.Size());
            for (i64 c = 0; c < index; ++c) {
                ++iter;
            }
            map.Erase(iter);
        }
    }

    void EraseRandom(const TCounterMap::TStorage& storage, TCounterMap map, i64 seed) {
        seed = std::abs(seed);
        if (storage.size() > 0) {
            auto pack = storage.Get(seed % 173 % storage.size());
            if (pack.keys_size() > 0) {
                auto key = pack.keys(seed % 179 % pack.keys_size());
                auto id = pack.counter_ids(seed % 181 % pack.counter_ids_size());
                map.ErasePackKey(id, key);
            }
        }
    }

    void EraseRandomFromList(TUserSubscriptionsList& list, i64 seed) {
        if (list.Size() != 0) {
            i64 index = std::labs(seed) % list.Size();
            list.Remove(index);
        }
    }

    template <class TMap>
    void AssertSoloKeywordMapsEqual(const TMap& l, const TMap& r) {
        auto compare = [&](const auto& x, const auto& y) {
            EXPECT_THAT(x, NGTest::EqualsProto(std::ref(y)));
        };
        for (const auto& pair : l) {
            auto iter = r.FindByItem(pair.second.Get());
            TString str;
            google::protobuf::TextFormat::PrintToString(pair.second.Get(), &str);
            EXPECT_NE(iter, r.end()) << TStringBuilder{} << "lost: " << str;
            compare(pair.second.Get(), iter->second.Get());
        }
        for (const auto& pair : r) {
            auto iter = l.FindByItem(pair.second.Get());
            TString str;
            google::protobuf::TextFormat::PrintToString(pair.second.Get(), &str);
            EXPECT_NE(iter, l.end()) << TStringBuilder{} << "lost: " << str;
            compare(pair.second.Get(), iter->second.Get());
        }
    }

    class TUserItemMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.UserItems, seed);
                return;
            }
            TUserItemProto item;
            item.set_keyword_id(235);
            item.set_update_time(timestamp);
            item.add_uint_values(seed);
            profile.UserItems.Add(&item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.UserItems, right.UserItems);
        }
        TString GetProtoField() override {
            return "UserItems";
        }
    };

    class TInterestsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Interests, seed);
                return;
            }
            TInterestProto item;
            ui64 id = seed % 14;
            item.set_bm_category_id(id);
            item.set_interest(seed % 999);
            item.set_interest_update_time(timestamp - seed % 1000);
            item.set_shows(seed % 432);
            item.set_clicks(seed % 100);
            item.set_event_time(timestamp - seed % 999);
            profile.Interests.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Interests, right.Interests);
        }
        TString GetProtoField() override {
            return "Interests";
        }
    };

    class TCountersMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            Y_UNUSED(seed);
            Y_UNUSED(profile);
            Y_UNUSED(timestamp);
            // counter.FindOrInsert(counterId, key).first->second.SetValue(timestamp + value);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            Y_UNUSED(left);
            Y_UNUSED(right);
        }
        TString GetProtoField() override {
            return "Counters";
        }
    private:
        virtual const TCounterMap& GetCounters(const TProfile& profile) const {
            return profile.Counters;
        }
    };

    class TCountersPackMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            using NBSYeti::EValueCase;
            auto& counter = const_cast<TCounterMap&>(GetCounters(profile));
            if (seed < 0) {
                EraseRandom(profile.GetProtos().GetCountersProto().GetCounterPack(), profile.Counters, seed);
                return;
            }
            const auto& packer = profile.CounterPacker;
            const auto& packMembers = packer.GetGroupByIndex(seed % packer.GetGroupsAmount());
            const auto key = seed % 100000;
            for (const auto& id : packMembers) {
                switch (packer.GetCounterType(id)) {
                    case EValueCase::kFloatValues: {
                            counter.Set<float>(id, key, (seed % 1000) * id / 1000.);
                        }
                        break;
                    case EValueCase::kFixed32Values: {
                            counter.Set<ui32>(id, key, timestamp);
                        }
                        break;
                    default:
                        ythrow yexception{} << "Not implemented\n";
                }
            }
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            auto leftCounterPack = left.GetProtos().GetMainProto().GetCounterPack();
            auto rightCounterPack = right.GetProtos().GetMainProto().GetCounterPack();
            EXPECT_EQ(leftCounterPack.size(), rightCounterPack.size()) << "differnt size of counters";
            for (i64 i = 0; i < leftCounterPack.size(); ++i) {
                EXPECT_THAT(leftCounterPack.Get(i), NGTest::EqualsProto(std::ref(rightCounterPack.Get(i))));
            }
        }
        TString GetProtoField() override {
            return "CounterPack";
        }
    private:
        virtual const TCounterMap& GetCounters(const TProfile& profile) const {
            return profile.Counters;
        }
    };

    class TDelayedCountersMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                if (profile.DelayedCounterUpdates.size() != 0) {
                    profile.DelayedCounterUpdates.Remove(seed % profile.DelayedCounterUpdates.size());
                }
                return;
            }
            auto* proto = profile.DelayedCounterUpdates.Add();
            proto->set_counter_id(seed % 999);
            proto->set_key(seed % 999);
            proto->set_value(seed * 2.0 / 54345345);
            proto->set_timestamp(timestamp - seed % 90000);
            if (seed % 3 != 0) {
                proto->set_target_domain_md5(seed % 145354);
            }
        }

        void AssertEqual(const TProfile& left, const TProfile& right) override {
            EXPECT_EQ(left.DelayedCounterUpdates.size(), right.DelayedCounterUpdates.size());
            for (ui64 i = 0; i < left.DelayedCounterUpdates.size(); ++i) {
                EXPECT_THAT(left.DelayedCounterUpdates.Get(i), NGTest::EqualsProto(std::ref(right.DelayedCounterUpdates.Get(i))));
            }
        }
        TString GetProtoField() override {
            return "DelayedCounterUpdates";
        }
    };

    class TInterestUpdatesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                if (profile.InterestUpdates.size() != 0) {
                    profile.InterestUpdates.Remove(seed % profile.InterestUpdates.size());
                }
                return;
            }
            profile.InterestUpdates.AddUpdate(
                200'000'000 + seed % 999,
                timestamp - seed % 90000,
                1.,
                seed % 145354);
        }

        void AssertEqual(const TProfile& left, const TProfile& right) override {
            EXPECT_EQ(left.InterestUpdates.size(), right.InterestUpdates.size());
            for (ui64 i = 0; i < left.InterestUpdates.size(); ++i) {
                EXPECT_THAT(left.InterestUpdates.Get(i), NGTest::EqualsProto(std::ref(right.InterestUpdates.Get(i))));
                EXPECT_EQ(
                    left.InterestUpdates.GetCommonDecay() * left.InterestUpdates.Get(i).value(),
                    right.InterestUpdates.GetCommonDecay() * right.InterestUpdates.Get(i).value()
                );
                // UNIT_ASSERT_C(comp, diff);
            }
        }
        TString GetProtoField() override {
            return "InterestsUpdates";
        }
    };

    class TOffersMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Offers, seed);
                return;
            }
            TOfferProto item;
            item.set_counter_id(seed % 432);
            item.set_offer_id_md5(seed % 123);
            item.set_action_bits(seed % 16);
            item.set_select_type(seed % 30);
            item.set_update_time(timestamp);
            profile.Offers.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Offers, right.Offers);
        }
        TString GetProtoField() override {
            return "Offers";
        }
    };

    class TQueriesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Queries, seed);
                return;
            }
            TQuery item;
            item.SetQueryId(seed % 131);
            item.SetQueryText(ToString(seed));
            item.SetUpdateTime(timestamp);
            item.SetCreateTime(timestamp - seed % 1000);
            item.SetCat4(BM_CATEGORY_ID_DUMP_SHIFT + seed % 10000);
            item.SetSelectType(seed % 150);
            item.SetHits(seed % 34);
            item.SetShows(seed % 38);
            item.SetClicks(seed % 37);
            profile.Queries.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            THashSet<TQueryId> ids;
            for (const auto& q : left.Queries) {
                ids.insert(q.first);
            }
            for (const auto& q : right.Queries) {
                ids.insert(q.first);
            }
            for (const auto& id : ids) {
                auto l = left.Queries.Find(id);
                auto r = right.Queries.Find(id);
                EXPECT_NE(l, left.Queries.end()) << id;
                EXPECT_NE(r, right.Queries.end()) << id;
                EXPECT_EQ(l->second.GetQueryId(), r->second.GetQueryId());
                EXPECT_EQ(l->second.GetQueryText(), r->second.GetQueryText());
                EXPECT_EQ(l->second.GetUpdateTime(), r->second.GetUpdateTime());
                EXPECT_EQ(l->second.GetCreateTime(), r->second.GetCreateTime());
                EXPECT_EQ(l->second.GetCat4(), r->second.GetCat4());
                EXPECT_EQ(l->second.GetSelectType(), r->second.GetSelectType());
                EXPECT_EQ(l->second.GetHits(), r->second.GetHits());
                EXPECT_EQ(l->second.GetShows(), r->second.GetShows());
                EXPECT_EQ(l->second.GetClicks(), r->second.GetClicks());
            }
        }
        TString GetProtoField() override {
            return "Queries";
        }
    };

    class TBannersMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Banners, seed);
                return;
            }
            TBannerProto item;
            item.set_banner_id(seed % 123456);
            item.set_update_time(timestamp);
            item.set_query_id(seed % 42123);
            profile.Banners.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Banners, right.Banners);
        }
        TString GetProtoField() override {
            return "Banners";
        }
    };

    class TDmpsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Banners, seed);
                return;
            }
            TDmpProto item;
            item.set_dmp_id(seed % 54324);
            item.set_update_time(timestamp);
            for (i64 i : xrange<i64>(0, seed % 10)) {
                item.add_segments(seed % (i + 1234));
            }
            profile.Dmps.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Dmps, right.Dmps);
        }
        TString GetProtoField() override {
            return "Dmps";
        }
    };

    class TLmFeaturesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.LmFeatures, seed);
                return;
            }
            ui64 counterId = seed % 100;
            ui64 key = seed % 1000;
            profile.LmFeatures.FindOrInsert(counterId, key).first->second.SetUpdateTime(timestamp);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            EXPECT_EQ(left.LmFeatures.Size(), right.LmFeatures.Size());
            for (const auto& p : left.LmFeatures) {
                auto iter = right.LmFeatures.Find(p.second.GetCounterId(), p.second.GetFeature());
                TString message = TStringBuilder{} << "problems with: " << p.second.GetCounterId() << " - " << p.second.GetFeature();
                EXPECT_NE(iter, right.LmFeatures.end()) << message;
                EXPECT_EQ(p.second.GetCounterId(), iter->second.GetCounterId()) << message;
                EXPECT_EQ(p.second.GetFeature(), iter->second.GetFeature()) << message;
                EXPECT_EQ(p.second.GetUpdateTime(), iter->second.GetUpdateTime()) << message;
            }
        }
        TString GetProtoField() override {
            return "LmFeatures";
        }
    };

    class TRegularCoordsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                if (profile.RegularCoords.Get().size() != 0) {
                    i64 index = std::abs(seed % profile.RegularCoords.Get().size());
                    profile.RegularCoords.Mutable()->SwapElements(index, profile.RegularCoords.Get().size() - 1);
                    profile.RegularCoords.Mutable()->RemoveLast();
                }
                return;
            }
            auto& item = *profile.RegularCoords.Mutable()->Add();
            item.set_latitude(seed / 43243.0);
            item.set_longitude(seed / 542435.0);
            item.set_update_time(timestamp);
            item.set_region_id(seed % 234567);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            EXPECT_EQ(left.RegularCoords.Get().size(), right.RegularCoords.Get().size());
            for (int i = 0; i < left.RegularCoords.Get().size(); ++i) {
                EXPECT_THAT(left.RegularCoords.Get()[i], NGTest::EqualsProto(std::ref(right.RegularCoords.Get()[i])));
            }
        }
        TString GetProtoField() override {
            return "RegularCoords";
        }
    };

    class TAuditoriumSegmentsMutator: public IMutator {
        public:
            void Mutate(TProfile&, ui64, i64) override {
            }
            void AssertEqual(const TProfile&, const TProfile&) override {
            }
            TString GetProtoField() override {
                return "AuditoriumSegments";
            }
    };

    class TAudienceSegmentsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                auto segments = profile.AudienceSegments.GetSegmentIds().Segments;
                if (segments.size() != 0) {
                    profile.AudienceSegments.EraseSegment(segments[seed % segments.size()]);
                }
                return;
            }
            profile.AudienceSegments.SetTimestamp(timestamp);
            profile.AudienceSegments.AddSegment(seed % 600000000 + 2000000000);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            auto f = left.AudienceSegments.GetSegmentIds().Segments;
            auto s = right.AudienceSegments.GetSegmentIds().Segments;
            Sort(f);
            Sort(s);
            EXPECT_EQ(f, s);
            if (f.size() != 0) {
                EXPECT_EQ(left.AudienceSegments.GetTimestamp(), right.AudienceSegments.GetTimestamp());
            }
        }
        TString GetProtoField() override {
            return "AudienceSegmentsWithPriorities";
        }
    };

    class TCdpSegmentsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                auto segments = profile.CdpSegments.GetSegmentIds();
                if (segments.size() != 0) {
                    profile.CdpSegments.EraseSegment(segments[seed % segments.size()]);
                }
                return;
            }
            profile.CdpSegments.SetTimestamp(timestamp);
            profile.CdpSegments.AddSegment(seed % 400000000 + 2600000000);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            auto f = left.CdpSegments.GetSegmentIds();
            auto s = right.CdpSegments.GetSegmentIds();
            Sort(f);
            Sort(s);
            EXPECT_EQ(f, s);
            if (f.size() != 0) {
                EXPECT_EQ(left.CdpSegments.GetTimestamp(), right.CdpSegments.GetTimestamp());
            }
        }
        TString GetProtoField() override {
            return "CdpSegments";
        }
    };

    class TApplicationsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Applications, seed);
                return;
            }
            TApplicationProto item;
            item.set_md5int_hash(seed % 1234567);
            item.set_crc32_hash(seed % 54323432);
            item.set_update_time(timestamp - seed % 321);
            item.set_install_time(timestamp - seed % 123);
            item.set_last_active_time(timestamp - seed % 13);
            item.set_active_month_frequency(seed % 100);
            profile.Applications.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Applications, right.Applications);
        }
        TString GetProtoField() override {
            return "Applications";
        }
    };

    class TUserSubscriptionsMutator : public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandomFromList(profile.UserSubscriptions, seed);
                return;
            }
            TUserSubscriptionProto item;
            item.set_update_time(timestamp + seed % 54325);
            item.set_expiration_timestamp(timestamp + seed % 143223 + 123);
            item.set_state(seed % 54321);
            *profile.UserSubscriptions.Add() = item;
        }
        void AssertEqual(const TProfile& leftProfile, const TProfile& rightProfile) override {
            auto left = leftProfile.GetProtos().GetMainProto().GetUserSubscriptions();
            auto right = rightProfile.GetProtos().GetMainProto().GetUserSubscriptions();
            EXPECT_EQ(left.size(), right.size()) << "differnt size";
            for (i64 i = 0; i < left.size(); ++i) {
                EXPECT_THAT(left.Get(i), NGTest::EqualsProto(std::ref(right.Get(i))));
            }
        }
        TString GetProtoField() override {
            return "UserSubscriptions";
        }
    };

    class TUserActivityMutator : public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.UserActivity, seed);
                return;
            }
            TUserActivityProto item;
            item.set_update_time(timestamp + seed % 54325);
            item.set_start_timestamp(timestamp + seed % 54325);
            item.set_end_timestamp(timestamp + seed % 54325 + 100);
            item.set_expiration_timestamp(timestamp + seed % 143223 + 123);
            item.set_value(seed % 54321);
            item.set_key(seed % 100);
            profile.UserActivity.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.UserActivity, right.UserActivity);
        }
        TString GetProtoField() override {
            return "UserActivity";
        }
    };

    class TUserEconomyMutator : public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.UserEconomy, seed);
                return;
            }
            TUserEconomyProto item;
            item.set_start_timestamp(timestamp + seed % 54325);
            item.set_end_timestamp(timestamp + seed % 54325 + 100);
            item.set_expiration_timestamp(timestamp + seed % 143223 + 123);
            item.set_tariff(seed % 54321);
            item.set_service(timestamp + seed % 100);
            profile.UserEconomy.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.UserEconomy, right.UserEconomy);
        }
        TString GetProtoField() override {
            return "UserEconomy";
        }
    };

    class TAdSystemsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.AdSystems, seed);
                return;
            }
            TAdSystemProto item;
            item.set_ad_system_id(seed % 54325);
            item.set_page_id(seed % 143223);
            item.set_impression_id(seed % 54321);
            item.set_last_event_time(timestamp - seed % 100);
            item.set_count(seed % 999);
            item.set_win_count_approx(seed % 123);
            // TODO: fix it
            profile.AdSystems.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.AdSystems, right.AdSystems);
        }
        TString GetProtoField() override {
            return "AdSystems";
        }
    };

    class TAuraRecommendationsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.AuraRecommendations, seed);
                return;
            }
            TRecommendationProto item;
            item.set_algo_id(seed % 7);
            item.set_update_time(timestamp - seed % 100);

            for (i64 i = 0; i < seed % 300; ++i) {
                item.add_item_id(seed * (i + 17) % 1000000);
                item.add_weight((seed * (i + 19) % 1000000) * 0.00000077);
            }

            profile.AuraRecommendations.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.AuraRecommendations, right.AuraRecommendations);
        }
        TString GetProtoField() override {
            return "AuraRecommendations";
        }
    };

    class TAuraVectorsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.AuraVectors, seed);
                return;
            }
            TFloatVectorProto item;
            item.set_vector_id(seed % 10);
            item.set_update_time(timestamp - seed % 100);

            for (i64 i = 0; i < seed % 30; ++i) {
                item.add_value((seed * (i + 19) % 1000000) * 0.00000077);
            }

            profile.AuraVectors.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.AuraVectors, right.AuraVectors);
        }
        TString GetProtoField() override {
            return "AuraVectors";
        }
    };

    class TCartsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.Carts, seed);
                return;
            }
            TCartProto item;
            item.set_cart_id(seed % 8);
            item.set_update_time(timestamp - seed % 999);
            for (int i = 0; i < seed % 5; i++) {
                item.add_bm_category_id((seed * (i + 7)) % 100000);
            }
            profile.Carts.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.Carts, right.Carts);
        }
        TString GetProtoField() override {
            return "Carts";
        }
    };

    class TVisitStatesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.VisitStates, seed);
                return;
            }
            TVisitStateProto item;
            item.set_start_timestamp(timestamp - seed % 1234);
            item.set_end_timestamp(timestamp);
            item.set_domain_md5(seed % 43245);
            item.set_is_bounce(seed % 2 == 0);
            item.set_hits_count(seed % 1234);
            profile.VisitStates.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.VisitStates, right.VisitStates);
        }
        TString GetProtoField() override {
            return "VisitStates";
        }
    };

    class TWatchIdStatesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.VisitStates, seed);
                return;
            }
            TWatchIdStatesProto watchItem;
            watchItem.set_bm_category_id(200);
            watchItem.set_watch_id(300);
            watchItem.set_update_time(timestamp + 1);
            profile.WatchIdStates.Add(watchItem);

            TWatchIdStatesProto visitItem;
            visitItem.set_watch_id(300);
            visitItem.set_update_time(timestamp + 2);
            visitItem.mutable_counters()->set_duration(40);
            visitItem.mutable_counters()->set_sum_mxt_good(1);
            visitItem.mutable_counters()->set_bounce(1);
            profile.WatchIdStates.Add(visitItem);

            TWatchIdStatesProto doubleVisitItem;
            doubleVisitItem.set_watch_id(100);
            doubleVisitItem.set_update_time(timestamp + 2);
            profile.WatchIdStates.Add(doubleVisitItem);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.WatchIdStates, right.WatchIdStates);
        }
        TString GetProtoField() override {
            return "WatchIdStates";
        }
    };

    class TSspFeedbacksMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.SspFeedbacks, seed);
                return;
            }
            TSspFeedbackProto item;
            item.set_page_id(seed % 1234567);
            item.set_page_token_id(seed % 56765754);
            item.set_tag_id(seed % 1234);
            item.set_update_time(timestamp - seed % 100);
            item.set_last_win_time(item.update_time() - seed % 111);

            auto mutateCounterPack = [seed](auto& counters) {
                auto mutateCounter = [seed](auto& counter, i64 counterId) {
                    counter.set_min_bid_log_sum(seed % (123 * counterId - 13));
                    counter.set_count(seed % (137 * counterId - 29));
                    counter.set_low_min_bid_count(seed % (231 * counterId - 7));
                    counter.set_medium_min_bid_count(seed % (97 * counterId - 11));
                };

                mutateCounter(*counters.mutable_event_counter(), 1);
                mutateCounter(*counters.mutable_win_counter(), 2);
                mutateCounter(*counters.mutable_lose_counter(), 3);
            };
            mutateCounterPack(*item.mutable_counters());

            profile.SspFeedbacks.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.SspFeedbacks, right.SspFeedbacks);
        }
        TString GetProtoField() override {
            return "SspFeedbacks";
        }
    };

    class TCreationTimeMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                return;
            }
            profile.TrySetCreationTime(timestamp);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            if (left.GetCreationTime() && right.GetCreationTime()) {
                EXPECT_EQ(left.GetCreationTime(), right.GetCreationTime());
            }

        }
        TString GetProtoField() override {
            return "CreationTime";
        }
    };

    class TDspFeedbacksMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.DspFeedbacks, seed);
                return;
            }
            TDspFeedbackProto item;
            item.set_page_id(seed % 12367);
            item.set_update_time(timestamp - seed % 213);
            item.mutable_hit_counter()->set_event_count(seed % 1999);
            item.mutable_hit_counter()->set_partner_price_sum(seed % 4377);
            item.mutable_show_counter()->set_event_count(seed % 9991);
            item.mutable_show_counter()->set_partner_price_sum(seed % 7734);

            profile.DspFeedbacks.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.DspFeedbacks, right.DspFeedbacks);
        }
        TString GetProtoField() override {
            return "DspFeedbacks";
        }
    };

    class TTsarVectorsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.TsarVectors, seed);
                return;
            }
            TTsarVectorProto item;
            item.set_vector_id(seed % 39);
            item.set_update_time(timestamp - seed % 5436);
            TString str;
            ui64 len = 1 + (seed % 111);
            for (size_t i = 1; i <= 4 * len; ++i) {
                char c = (seed / i) % 256;
                str += c;
            }
            item.set_value(str);
            item.set_vector_size(str.size());
            item.set_element_size(4);

            profile.TsarVectors.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.TsarVectors, right.TsarVectors);
        }
        TString GetProtoField() override {
            return "TsarVectors";
        }
    };

    class TImportantRegionsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.ImportantRegions, seed);
                return;
            }

            TImportantRegionProto item;
            item.set_region_id(seed % 5001);
            auto updateTime = timestamp - seed % 8675;
            item.set_update_time(updateTime);
            item.set_last_visit_time(updateTime - seed % 10167);

            auto typesDescriptor = TImportantRegionProto::ERegionType_descriptor();
            auto typesCnt = typesDescriptor->value_count();
            auto residue = seed % (1 << typesCnt);
            i8 bit = 1;

            for (auto i: xrange(typesCnt)){
                if (bit & residue) {
                    item.add_types(static_cast<TImportantRegionProto::ERegionType>(typesDescriptor->value(i)->number()));
                }
                bit <<= 1;
            }

            profile.ImportantRegions.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.ImportantRegions, right.ImportantRegions);
        }
        TString GetProtoField() override {
            return "ImportantRegions";
        }
    };

    TBannerClickProto GenerateBannerClickProto(ui64 timestamp, i64 seed) {
        TBannerClickProto item;
        auto updateTime = timestamp - seed % 8675;
        item.set_timestamp(updateTime);
        item.set_update_time(updateTime);
        item.set_position(seed % 123);
        item.set_icookie(seed % 54321);
        item.set_event_hash(seed % 12345);
        item.set_hit_log_id(seed % 10167);
        item.set_banner_id(seed % 987);

        TString str;
        ui64 len = 1 + (seed % 111);
        for (size_t i = 1; i <= 4 * len; ++i) {
            char c = (seed / i) % 256;
            str += c;
        }

        item.set_banner_title(str);
        item.set_banner_url(str);

        return item;
    }

    class TLastRsyaBannerClicksMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.LastRsyaBannerClicks, seed);
                return;
            }

            profile.LastRsyaBannerClicks.Add(GenerateBannerClickProto(timestamp, seed));
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.LastRsyaBannerClicks, right.LastRsyaBannerClicks);
        }
        TString GetProtoField() override {
            return "LastRsyaBannerClicks";
        }
    };

    class TLastSearchBannerClicksMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.LastSearchBannerClicks, seed);
                return;
            }

            profile.LastSearchBannerClicks.Add(GenerateBannerClickProto(timestamp, seed));
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.LastSearchBannerClicks, right.LastSearchBannerClicks);
        }
        TString GetProtoField() override {
            return "LastSearchBannerClicks";
        }
    };

    class TMarketLoyaltyCoinsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.MarketLoyaltyCoins, seed);
                return;
            }
            TMarketLoyaltyCoinProto item;
            item.set_id(seed % 995783);
            item.set_end_date(timestamp + seed % 864103);
            item.set_update_time(timestamp - seed % 8677);
            item.set_update_msec(seed % 997);
            item.set_nominal(seed % 1117);
            item.set_reason(TMarketLoyaltyCoinProto::EMAIL_COMPANY);
            item.set_type(TMarketLoyaltyCoinProto::FIXED);
            item.set_promo_id(seed % 41513);
            item.set_title("Скидка 100 ₽");
            item.set_subtitle("на заказ от 1 000 ₽");
            profile.MarketLoyaltyCoins.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.MarketLoyaltyCoins, right.MarketLoyaltyCoins);
        }
        TString GetProtoField() override {
            return "MarketLoyaltyCoins";
        }
    };

    class TMarketLoyaltyDisabledPromoThreasholdsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.MarketLoyaltyDisabledPromoThreasholds, seed);
                return;
            }
            TMarketLoyaltyDisabledPromoThreasholdsProto item;
            item.set_update_time(timestamp - seed % 8677);
            item.set_block_promo_id("some_block_promo_id");
            profile.MarketLoyaltyDisabledPromoThreasholds.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.MarketLoyaltyDisabledPromoThreasholds, right.MarketLoyaltyDisabledPromoThreasholds);
        }
        TString GetProtoField() override {
            return "MarketLoyaltyDisabledPromoThreasholds";
        }
    };

    class TOrderShowsMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            Y_UNUSED(profile);
            Y_UNUSED(timestamp);
            Y_UNUSED(seed);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            Y_UNUSED(left);
            Y_UNUSED(right);
        }

        TString GetProtoField() override {
            return "OrderShows";
        }
    };

    class TFrequencyEventsMutator: public IMutator {
        // currently this field should be empty
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            Y_UNUSED(profile);
            Y_UNUSED(timestamp);
            Y_UNUSED(seed);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            Y_UNUSED(left);
            Y_UNUSED(right);
        }
        TString GetProtoField() override {
            return "FrequencyEvents";
        }
    };

    class TDJProfilesMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                EraseRandom(profile.DJProfiles, seed);
                return;
            }
            TDJProfileProto item;
            item.set_keyword_id(235);
            NDJ::TProfile djProfile(NDJ::TProfileKey(NDJ::TObjectType(NDJ::EProfileNamespace::PN_Common, 1), ToString(seed % 12345678)));
            djProfile.Counters.Add(
                NDJ::TObjectType(NDJ::EProfileNamespace::PN_Common, 5),
                NDJ::TCounterType(NDJ::EProfileNamespace::PN_Znatoki, 1),
                ToString(seed % 54321),
                TInstant::Seconds(timestamp),
                seed % 321
            );
            item.mutable_profile()->CopyFrom(djProfile.ToProto());
            profile.DJProfiles.Add(item);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            AssertSoloKeywordMapsEqual(left.DJProfiles, right.DJProfiles);
        }
        TString GetProtoField() override {
            return "DjProfiles";
        }
    };

    /*
     * Mutator to check delete rows case
     */
    class TEraseAllMutator: public IMutator {
    public:
        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            if (seed < 0) {
                Y_UNUSED(timestamp);
                profile.Clear();
                return;
            }
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            Y_UNUSED(left);
            Y_UNUSED(right);
        }
        TString GetProtoField() override {
            return "";
        }
    };

    class TCodecMutator: public IMutator {
        TVector<ui64> PossibleCodecIds;
        TSerializableProfile& Serialiszble(TProfile& profile) {
            return static_cast<TSerializableProfile&>(profile);
        }
        const TSerializableProfile& Serialiszble(const TProfile& profile) {
            return static_cast<const TSerializableProfile&>(profile);
        }

    public:
        TCodecMutator() = delete;
        TCodecMutator(const NZstdFactory::TCodecFactory& factory, const THashSet<ui64>& exclude) {
            PossibleCodecIds = factory.ListPossibleCodecs();
            std::erase_if(PossibleCodecIds, [&exclude](const ui64 value) {return exclude.contains(value);});
        }

        void Mutate(TProfile& profile, ui64 timestamp, i64 seed) override {
            auto& serialisableProfile = Serialiszble(profile);
            if (seed < 0) {
                return;
            }
            serialisableProfile.SetEncodingCodec(PossibleCodecIds[seed % PossibleCodecIds.size()]);
            Y_UNUSED(timestamp);
        }
        void AssertEqual(const TProfile& left, const TProfile& right) override {
            ui64 savedCodec = Serialiszble(left).GetEncodingCodec();
            ui64 loadedCodec = Serialiszble(right).GetEncodingCodec();
            TString message = TStringBuilder{} << "Saved codec:" << savedCodec << ", loaded:" << loadedCodec << ", id: " << right.ProfileId;
            Y_ENSURE(savedCodec == loadedCodec, message);
        }
        TString GetProtoField() override {
            // It is not a real protobuf field
            return "CodecID";
        }
    };

    TVector<IMutatorPtr> PrepareMutators(const NZstdFactory::TCodecFactory& factory, const THashSet<ui64>& excludedCodecs) {
        TVector<IMutatorPtr> mutators{
            MakeAtomicShared<TUserItemMutator>(),
            MakeAtomicShared<TInterestsMutator>(),
            MakeAtomicShared<TCountersMutator>(),
            MakeAtomicShared<TCountersPackMutator>(),
            MakeAtomicShared<TOffersMutator>(),
            MakeAtomicShared<TQueriesMutator>(),
            MakeAtomicShared<TBannersMutator>(),
            MakeAtomicShared<TDmpsMutator>(),
            MakeAtomicShared<TLmFeaturesMutator>(),
            MakeAtomicShared<TRegularCoordsMutator>(),
            MakeAtomicShared<TAuditoriumSegmentsMutator>(),
            MakeAtomicShared<TAudienceSegmentsMutator>(),
            MakeAtomicShared<TCdpSegmentsMutator>(),
            MakeAtomicShared<TApplicationsMutator>(),
            MakeAtomicShared<TAdSystemsMutator>(),
            MakeAtomicShared<TVisitStatesMutator>(),
            MakeAtomicShared<TDelayedCountersMutator>(),
            MakeAtomicShared<TInterestUpdatesMutator>(),
            MakeAtomicShared<TCartsMutator>(),
            MakeAtomicShared<TWatchIdStatesMutator>(),
            MakeAtomicShared<TAuraRecommendationsMutator>(),
            MakeAtomicShared<TAuraVectorsMutator>(),
            MakeAtomicShared<TSspFeedbacksMutator>(),
            MakeAtomicShared<TDspFeedbacksMutator>(),
            MakeAtomicShared<TUserEconomyMutator>(),
            MakeAtomicShared<TUserActivityMutator>(),
            MakeAtomicShared<TUserSubscriptionsMutator>(),
            MakeAtomicShared<TTsarVectorsMutator>(),
            MakeAtomicShared<TCreationTimeMutator>(),
            MakeAtomicShared<TImportantRegionsMutator>(),
            MakeAtomicShared<TLastRsyaBannerClicksMutator>(),
            MakeAtomicShared<TLastSearchBannerClicksMutator>(),
            MakeAtomicShared<TMarketLoyaltyCoinsMutator>(),
            MakeAtomicShared<TMarketLoyaltyDisabledPromoThreasholdsMutator>(),
            MakeAtomicShared<TOrderShowsMutator>(),
            MakeAtomicShared<TFrequencyEventsMutator>(),
            MakeAtomicShared<TDJProfilesMutator>(),
        };

        THashSet<TString> fields;
        for (auto& m : mutators) {
            fields.insert(m->GetProtoField());
        }
        fields.insert("LegacyBanners");
        THashSet<TString> expectedFields;
        {
            const auto* desc = NBSYeti::TProfileProto::descriptor();
            for (int i = 0; i < desc->field_count(); ++i) {
                expectedFields.insert(desc->field(i)->name());
            }
        }
        EXPECT_EQ(fields, expectedFields);
        mutators.push_back(MakeAtomicShared<TEraseAllMutator>());
        mutators.push_back(MakeAtomicShared<TCodecMutator>(factory, excludedCodecs)); // CodecId is not a part of general profile
        return mutators;
    }
}
