#include <ads/bigkv/preprocessing/new_user_factors/lib/FeatureComputer.h>
#include <ads/bigkv/preprocessing/new_user_factors/yt/NewUserFactorsMapper.h>
#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/iterator/zip.h>


namespace NewUserFactors {
    class TNewUserFactorsTest : public TTestBase {
    public:
        void DefaultQueryFactorsComputerTest() {
            TQueryFactorsComputer computer;
            // We use defaultly-constructed QueryFactorsComputer in some places, changing defaults
            // may implicitly hurt them. Ask @robdrynkin or @alxmopo3ov
            UNIT_ASSERT_EQUAL(computer.MinAgeThreshold, -120.);
            UNIT_ASSERT_EQUAL(computer.MaxAgeThreshold, static_cast<double>(3600 * 24 * 30));
        }

        void CountersTest() {
            THashMap<TString, THashSet<ui64>> trueResult = {
                {"CounterKeys427", {1, 2, 3}},
                {"CounterKeys19", {8, 9, 10, 15}}
            };

            THashMap<TString, THashSet<ui64>> actualResult;
            for (const auto &counterID: Computer.GetCounterKeysComputer().Factors()) {
                if (ComputedFactors.contains(counterID)) {
                    for (auto key: ComputedFactors[counterID].AsList()) {
                        actualResult[counterID].insert(key.AsUint64());
                    }
                }
            }

            UNIT_ASSERT_EQUAL(trueResult.size(), actualResult.size());
            for (const auto &[key, value]: trueResult) {
                for (const auto &x: value) {
                    UNIT_ASSERT(trueResult[key].contains(x));
                }
            }
        }

        void CounterAggregatedFactorsTest() {
            UNIT_ASSERT_EQUAL(Computer.GetCounterAggValuesComputer().Factors().size(), 1);

            auto counterAggValuesName = Computer.GetCounterAggValuesComputer().Factors()[0];
            UNIT_ASSERT_EQUAL(ComputedFactors[counterAggValuesName].AsList().size(), 4 * (DefaultValueCounterIDs.size() + DefaultTimeCounterIDs.size()));
        }

        void QueryTest() {
            UNIT_ASSERT_EQUAL(Computer.GetQueryFactorsComputer().Factors().size(), 5);
            auto actualQueryTexts = ComputedFactors["QueryTexts"].AsList();
            auto actualQueryFactors = ComputedFactors["QueryFactors"].AsList();
            auto actualQueryHourMasks = ComputedFactors["QueryHourMasks"].AsList();
            auto actualQuerySelectTypes = ComputedFactors["QuerySelectTypes"].AsList();
            auto actualQueryCategories = ComputedFactors["QueryCategoriesNew"].AsList();

            UNIT_ASSERT_EQUAL(actualQueryTexts.size(), actualQueryFactors.size());
            UNIT_ASSERT_EQUAL(actualQueryTexts.size(), actualQueryHourMasks.size());
            UNIT_ASSERT_EQUAL(actualQueryTexts.size(), actualQuerySelectTypes.size());
            UNIT_ASSERT_EQUAL(actualQueryTexts.size(), actualQueryCategories.size());

            THashMap<TString, std::tuple<
                TVector<float>,
                ui64,
                ui64,
                TVector<ui64>
            >> trueQueriesFactors = {
                {"asd qwe", {
                    {1, 2, 3, 1200, 1199},
                    0,
                    4,
                    {1, 2, 3}
                }},
                {"zxc dsa", {
                    {4, 5, 6, 1100, 1050},
                    0,
                    5,
                    {7, 8, 9}
                }},
                {"slight future", {
                    {2, 4, 6, -5., -5.},
                    0,
                    7,
                    {10, 11, 12}
                }}
            };
            UNIT_ASSERT_EQUAL(trueQueriesFactors.size(), actualQueryTexts.size());

            for (ui32 i = 0; i < actualQueryTexts.size(); ++i) {
                const auto &[trueFactors, trueHourMask, trueSelectType, trueCategories] = trueQueriesFactors[actualQueryTexts[i].AsString()];

                UNIT_ASSERT_EQUAL(trueFactors.size(), actualQueryFactors[i].AsList().size());
                for (const auto &[trueValue, actualValue]: Zip(trueFactors, actualQueryFactors[i].AsList())) {
                    UNIT_ASSERT_EQUAL(trueValue, actualValue);
                }

                UNIT_ASSERT_EQUAL(trueHourMask, actualQueryHourMasks[i].AsUint64());

                UNIT_ASSERT_EQUAL(trueSelectType, actualQuerySelectTypes[i].AsUint64());

                UNIT_ASSERT(actualQueryCategories[i].AsList().size() == 3);
                for (const auto &[trueValue, actualValue]: Zip(trueCategories, actualQueryCategories[i].AsList())) {
                    UNIT_ASSERT_EQUAL(trueValue, actualValue.AsUint64());
                }
            }
        }

        void VisitStateTest() {
            UNIT_ASSERT_EQUAL(Computer.GetVisitStateFactorsComputer().Factors().size(), 1);
            auto visitStatesFactorName = Computer.GetVisitStateFactorsComputer().Factors()[0];
            TVector<float> trueValues = {2, 23227.5, 35344, 234, 345, 0.01041565743, 0.5};

            for (const auto &[trueValue, actualValue]: Zip(trueValues, ComputedFactors[visitStatesFactorName].AsList())) {
                UNIT_ASSERT_DOUBLES_EQUAL(trueValue, actualValue.AsDouble(), 1e-5);
            }
        }

        void AdSystemTest() {
            UNIT_ASSERT_EQUAL(Computer.GetMediationFactorsComputer().Factors().size(), 3);
            THashSet<ui64> truePageIDs = {4566, 1234}, trueAdSystems = {456, 123};
            TVector<float> trueRealFactors = {2, 195, 345, 45, 1.49000001, 1.480000019, 1.49000001, 382.75, 654, 4};

            UNIT_ASSERT_EQUAL(truePageIDs.size(), ComputedFactors["MediationPageIDs"].AsList().size());
            UNIT_ASSERT_EQUAL(trueAdSystems.size(), ComputedFactors["MediationAdSystems"].AsList().size());
            UNIT_ASSERT_EQUAL(trueRealFactors.size(), ComputedFactors["MediationRealFactors"].AsList().size());

            for (auto x: ComputedFactors["MediationPageIDs"].AsList()) {
                UNIT_ASSERT(truePageIDs.contains(x.AsUint64()));
            }

            for (auto x: ComputedFactors["MediationAdSystems"].AsList()) {
                UNIT_ASSERT(trueAdSystems.contains(x.AsUint64()));
            }

            for (const auto &[trueValue, actualValue]: Zip(trueRealFactors, ComputedFactors["MediationRealFactors"].AsList())) {
                UNIT_ASSERT_DOUBLES_EQUAL(trueValue, actualValue.AsDouble(), 1e-5);
            }
        }

        void DMPTest() {
            THashSet<ui64> trueDMPSegments = {456000000006, 456000000007, 456000000008, 456000000009, 456000000010, 123000000001, 123000000002, 123000000003, 123000000004, 123000000005};
            TVector<float> trueUpdateDateTimeStats = {195, 345, 45};

            UNIT_ASSERT_EQUAL(trueDMPSegments.size(), ComputedFactors["DMPSegments"].AsList().size());
            for (const auto &[trueValue, actualValue]: Zip(trueDMPSegments, ComputedFactors["DMPSegments"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue);
            }

            UNIT_ASSERT_EQUAL(ComputedFactors["DMPUpdateTimeStatistics"].AsList().size(), 3);
            for (const auto &[trueValue, actualValue]: Zip(trueUpdateDateTimeStats, ComputedFactors["DMPUpdateTimeStatistics"].AsList())) {
                UNIT_ASSERT_DOUBLES_EQUAL(trueValue, actualValue.AsDouble(), 1e-5);
            }
        }

        void SearchQueryTest() {
            UNIT_ASSERT_EQUAL(ComputedFactors["SearchQueryText"].AsString(), "Hello world search query");
            UNIT_ASSERT_EQUAL(ComputedFactors["SearchQueryRegion"].AsList().size(), 1);
            UNIT_ASSERT_EQUAL(ComputedFactors["SearchQueryRegion"].AsList()[0].AsUint64(), 123);
        }

        void SearchQueryHistoryTest() {
            TVector<TString> texts = {"asd df", "qwer ksmfg"};
            TVector<i64> regions = {12, 34};
            TVector<double> ts = {56, 78};

            UNIT_ASSERT_EQUAL(ComputedFactors["QueryHistoryTexts"].AsList().size(), texts.size());
            for (const auto &[trueValue, actualValue]: Zip(texts, ComputedFactors["QueryHistoryTexts"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue.AsString());
            }

            UNIT_ASSERT_EQUAL(ComputedFactors["QueryHistoryRegions"].AsList().size(), regions.size());
            for (const auto &[trueValue, actualValue]: Zip(regions, ComputedFactors["QueryHistoryRegions"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue.AsInt64());
            }

            UNIT_ASSERT_EQUAL(ComputedFactors["QueryHistoryFactors"].AsList().size(), ts.size());
            for (const auto &[trueValue, actualValue]: Zip(ts, ComputedFactors["QueryHistoryFactors"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue.AsDouble());
            }
        }

        void SearchDocumentHistoryTest() {
            TVector<TString> titles = {"asd df", "qwer ksmfg"}, urls = {"http://asdf.df", "www.qwer.ru/ksmfg"};
            TVector<TVector<double>> factors = {
                {56, 66, 12},
                {78, 88, 34}
            };

            UNIT_ASSERT_EQUAL(ComputedFactors["DocumentHistoryTitles"].AsList().size(), titles.size());
            for (const auto &[trueValue, actualValue]: Zip(titles, ComputedFactors["DocumentHistoryTitles"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue.AsString());
            }

            UNIT_ASSERT_EQUAL(ComputedFactors["DocumentHistoryUrls"].AsList().size(), urls.size());
            for (const auto &[trueValue, actualValue]: Zip(urls, ComputedFactors["DocumentHistoryUrls"].AsList())) {
                UNIT_ASSERT_EQUAL(trueValue, actualValue.AsString());
            }

            UNIT_ASSERT_EQUAL(ComputedFactors["DocumentHistoryFactors"].AsList().size(), factors.size());
            for (const auto &[trueList, actualList]: Zip(factors, ComputedFactors["DocumentHistoryFactors"].AsList())) {
                UNIT_ASSERT_EQUAL(actualList.AsList().size(), trueList.size());
                for (const auto &[trueValue, actualValue]: Zip(trueList, actualList.AsList())) {
                    UNIT_ASSERT_EQUAL(trueValue, actualValue.AsDouble());
                }
            }
        }

        void SetUp() override {
            const ui64 timestamp = 1601988640;
            const ui64 far_timedelta = 1e7;
            UNIT_ASSERT(timestamp > far_timedelta); // This is used below for building profile
            NewUserFactors::TProfile profile;

            AddCounter(profile, 427, {1, 2, 3}, {5, 6, 7});
            AddCounter(profile, 19, {8, 9, 10, 15}, {0.1, 2.4, 5.0, -10.3});
            AddCounter(profile, 10, {4, 5, 6}, {0, 1, 2});
            AddCounter(profile, 182, {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}, {0, 1, 2, 5, 3, 6, 7, 9, 1, 4, 3});

            AddQuery(profile, "asd qwe", 1, 2, 3, 4, timestamp - 1200, timestamp - 1199, 1, 2, 3);
            AddQuery(profile, "zxc dsa", 4, 5, 6, 5, timestamp - 1100, timestamp - 1050, 7, 8, 9);
            AddQuery(profile, "target domain md5", 4, 5, 6, 5, timestamp - 1101, timestamp - 1052, 7, 8, 9, true);
            AddQuery(profile, "slight future", 2, 4, 6, 7, timestamp + 5, timestamp + 5, 10, 11, 12);
            AddQuery(profile, "Far future", 2, 4, 6, 7, timestamp + far_timedelta, timestamp + far_timedelta, 10, 11, 12);
            AddQuery(profile, "Far past", 2, 4, 6, 7, timestamp - far_timedelta, timestamp - far_timedelta, 10, 11, 12);

            AddVisitState(profile, 12345, 23456, 123, true);
            AddVisitState(profile, 43566, 78910, 345, false);

            AddAdSystem(profile, 1234, 456, timestamp - 345, 0.99, 1.98, 0.99, {433, 321});
            AddAdSystem(profile, 4566, 123, timestamp - 45, 1.99, 0.98, 1.99, {123, 654});

            AddDMP(profile, 123, timestamp - 345, {1, 2, 3, 4, 5});
            AddDMP(profile, 456, timestamp - 45, {6, 7, 8, 9, 10});

            AddSearchQuery(profile, "Hello world search query", 123);

            AddSearchQueryHistory(profile, {"asd df", "qwer ksmfg"}, {12, 34}, {timestamp - 56, timestamp - 78});
            AddSearchDocumentHistory(
                profile,
                {"asd df", "qwer ksmfg"}, {"http://asdf.df", "www.qwer.ru/ksmfg"},
                {12, 34},
                {timestamp - 56, timestamp - 78}, {timestamp - 66, timestamp - 88}
            );

            ComputedFactors = Computer.ComputeFactors(profile, timestamp);
            for (const auto &[key, val]: SearchQueryHistoryComputer.ComputeFactors(profile, timestamp)) {
                ComputedFactors[key] = val;
            }
            for (const auto &[key, val]: SearchDocumentHistoryComputer.ComputeFactors(profile, timestamp)) {
                ComputedFactors[key] = val;
            }
        }

    private:
        NYT::TNode::TMapType ComputedFactors;
        NewUserFactors::TNewUserFactorsComputer Computer;
        NewUserFactors::TSearchQueryHistoryComputer SearchQueryHistoryComputer;
        NewUserFactors::TSearchDocumentHistoryComputer SearchDocumentHistoryComputer;

        UNIT_TEST_SUITE(TNewUserFactorsTest);
        UNIT_TEST(DefaultQueryFactorsComputerTest);
        UNIT_TEST(CountersTest);
        UNIT_TEST(CounterAggregatedFactorsTest);
        UNIT_TEST(QueryTest);
        UNIT_TEST(VisitStateTest);
        UNIT_TEST(AdSystemTest);
        UNIT_TEST(DMPTest);
        UNIT_TEST(SearchQueryTest);
        UNIT_TEST(SearchQueryHistoryTest);
        UNIT_TEST(SearchDocumentHistoryTest);
        UNIT_TEST_SUITE_END();

        void AddCounter(
            NewUserFactors::TProfile &profile,
            ui32 counterID,
            TVector<ui64> keys,
            TVector<float> values
        ) {
            Y_ENSURE(keys.size() == values.size());

            {
                auto cntr = profile.add_counters();
                cntr->set_counter_id(counterID);
                for (ui64 i = 0; i < keys.size(); ++i) {
                    cntr->add_key(keys[i]);
                    cntr->add_value(values[i]);
                }
            }

            {
                auto cntrPack = profile.add_packed_counters();
                cntrPack->add_counter_ids(counterID);
                auto cntrPackValues = (cntrPack->add_values())->mutable_double_values();

                for (ui64 i = 0; i < keys.size(); ++i) {
                    cntrPack->add_keys(keys[i]);
                    cntrPackValues->add_value(values[i]);
                }
            }
        }

        void AddQuery(
            NewUserFactors::TProfile &profile,
            TString queryText,
            ui64 hits, ui64 shows, ui64 clicks, ui64 selectType,
            ui64 unixUpdateTime, ui64 createTime,
            ui64 cat4, ui64 cat5, ui64 cat6,
            bool hasTargetDomainMD5 = false
        ) {
            auto query = profile.add_queries();

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

        void AddVisitState(
            NewUserFactors::TProfile &profile,
            ui64 start_timestamp, ui64 end_timestamp,
            ui64 hits_count, bool is_bounce
        ) {
            auto visitState = profile.add_visit_states();

            visitState->set_start_timestamp(start_timestamp);
            visitState->set_end_timestamp(end_timestamp);
            visitState->set_hits_count(hits_count);
            visitState->set_is_bounce(is_bounce);
        }

        void AddAdSystem(
            NewUserFactors::TProfile &profile,
            ui64 page_id, ui64 ad_system_id,
            ui64 last_event_time, double count, double hit_count,
            double win_count_approx, TVector<ui64> bids
        ) {
            auto events = profile.add_ad_systems();

            events->set_page_id(page_id);
            events->set_ad_system_id(ad_system_id);
            events->set_last_event_time(last_event_time);
            events->set_count(count);
            events->set_hit_count(hit_count);
            events->set_win_count_approx(win_count_approx);
            for (auto bid: bids) {
                auto event = events->add_wins_approx();
                event->set_bid(bid);
            }
        }

        void AddDMP(
            NewUserFactors::TProfile &profile,
            ui64 DMPID, ui64 updateTime,
            TVector<ui64> segments
        ) {
            auto dmp = profile.add_dmps();

            dmp->set_dmp_id(DMPID);
            dmp->set_update_time(updateTime);
            for (auto segment: segments) {
                dmp->add_segments(segment);
            }
        }

        void AddSearchQuery(
            NewUserFactors::TProfile &profile,
            TString searchQuery,
            ui64 regionID
        ) {
            auto searchQueryField = profile.mutable_current_search_query();
            searchQueryField->set_query_text(searchQuery);
            searchQueryField->set_region(regionID);
        }

        void AddSearchQueryHistory(
            NewUserFactors::TProfile &profile,
            TVector<TString> searchQueryTexts,
            TVector<ui64> regions,
            TVector<ui64> ts
        ) {
            auto searchPersProfile = profile.search_pers_profiles().size() == 0 ? profile.add_search_pers_profiles() : profile.mutable_search_pers_profiles(0);
            auto filteredRecord = searchPersProfile->mutable_profile()->MutableUserHistoryState()->MutableUserHistory()->AddFilteredRecords();
            filteredRecord->MutableDescription()->SetMaxRecords(130);

            for (ui32 i = 0; i < searchQueryTexts.size(); ++i) {
                auto record = filteredRecord->AddRecords();
                record->SetQuery(searchQueryTexts[i]);
                record->SetRegion(regions[i]);
                record->SetTimestamp(ts[i]);
            }
        }

        void AddSearchDocumentHistory(
            NewUserFactors::TProfile &profile,
            TVector<TString> titles,
            TVector<TString> urls,
            TVector<ui64> dwellTimes,
            TVector<ui64> ts,
            TVector<ui64> reqTs
        ) {
            auto searchPersProfile = profile.search_pers_profiles().size() == 0 ? profile.add_search_pers_profiles() : profile.mutable_search_pers_profiles(0);
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
    };

    UNIT_TEST_SUITE_REGISTRATION(TNewUserFactorsTest);
}
