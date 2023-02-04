#include <contrib/libs/benchmark/include/benchmark/benchmark.h>

#include <ads/bsyeti/eagle/wide_profile/builder.h>
#include <ads/bsyeti/eagle/wide_profile/wide_profile.h>
#include <ads/bsyeti/libs/counter_lib/counter_packer/counter_packer.h>
#include <ads/bsyeti/libs/yt_storage/collect_state.h>

#include <library/cpp/blockcodecs/codecs.h>

#include <util/datetime/base.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/system/shellcommand.h>

#include <tuple>


namespace {
    time_t FetchCurrentTime(time_t preset = 0) {
        static time_t currentTime = 0;
        if (preset != 0) {
            currentTime = preset;
        }
        Y_ENSURE(currentTime > 0);
        return currentTime;
    }

    TVector<TString> ReadProfilesFromDiskAndSetTimestamp() {
        TVector<TString> storage;
        Cerr << "will read thickProfilesForBenchmark from disk" << Endl;
        TIFStream iFile("thickProfilesForBenchmark", std::ios::binary);
        ui64 currentTime;
        iFile.Read(reinterpret_cast<char*>(&currentTime), 8);
        FetchCurrentTime(currentTime); // we set this ts as global
        ui64 pSize = 0;
        iFile.Read(reinterpret_cast<char*>(&pSize), 8);
        while (0 != pSize) {
            static char array[10 * 1024 * 1024];
            Y_ENSURE(pSize < sizeof array);
            iFile.Read(array, pSize);
            TString profile(array, pSize);
            storage.push_back(profile);

            pSize = 0;
            iFile.Read(reinterpret_cast<char*>(&pSize), 8);
        }
        Y_ENSURE(storage.size() > 100);
        Cerr << "read done" << Endl;
        return storage;
    }

    TVector<TString> FetchProfiles() {
        static TVector<TString> storage(Reserve(5000));
        if (storage.empty()) {
            storage = ReadProfilesFromDiskAndSetTimestamp();
        }
        return storage;
    }

    NZstdFactory::TCodecFactory& GetCodecFactory() {
        static NZstdFactory::TCodecFactory factory{"dicts_cache/"};
        return factory;
    }

    void FillRawProfile(NEagle::TMultiRawProfile* rawProfile, const TVector<TString>& storage) {
        auto& factory = GetCodecFactory();
        for (const auto& binaryRow : storage) {
            NBSYeti::TProfileTableRecord row;
            Y_PROTOBUF_SUPPRESS_NODISCARD row.ParseFromString(binaryRow);
            const NCodecs::ICodec* codec = nullptr;
            codec = factory.Get(row.GetCodecId());
            Y_ENSURE(codec != nullptr, "Codec wasn't initialized.\n");

            auto&& [state, _] = NBSYeti::CollectState(row, factory);

            NBSYeti::TSourceUniq meta;
            meta.set_uniq_id(row.GetUniqId());
            meta.set_is_main(true);
            meta.set_is_indevice(true);
            meta.set_id_type(NBSYeti::TSourceUniq::UNKNOWN_YID);
            meta.set_user_id(row.GetUniqId()); // it is not good, but its ok.

            auto p = rawProfile->Profiles.emplace(row.GetUniqId(), NEagle::TMetaRawProfile{meta});

            p.first->second.Protos = NBSYeti::ParseProfileProtoPack(state);
        }
    }

    void FillRawProfiles(NEagle::TMultiRawProfile* rawProfile, TVector<NEagle::TMetaRawProfile*>& rawProfiles) {
        for (auto& [profileId, rawProfile] : rawProfile->Profiles) {
            Y_UNUSED(profileId);
            if (!rawProfile.Protos) {
                rawProfile.Protos = MakeHolder<NBSYeti::TProfileProtoPackPlain>();
            }
            rawProfiles.push_back(&rawProfile);
        }
        SortBy(rawProfiles, [](const auto* x) {return x->Meta.uniq_id();}); // not equal with builder function, but its ok.
    }
}

static void BM_WideProfileCreation(benchmark::State& state) {
    const auto& keywordInfo = NBSYeti::BuildKeywordInfoFromProto();
    const auto& counterPacker = NBSYeti::GetCounterPacker();

    TString mainProfileId = "y7387";

    for (auto _ : state) {
        auto wideProfile = MakeHolder<NEagle::TWideProfile>();
        wideProfile->Profile.ConstructInPlace(mainProfileId, keywordInfo, counterPacker);
    }
}

static void BM_ProfileParsing(benchmark::State& state) {
    const auto& factory = GetCodecFactory();
    Y_UNUSED(factory);

    const auto storage = FetchProfiles();

    for (auto _ : state) {

        NEagle::TMultiRawProfile rawProfile;
        FillRawProfile(&rawProfile, storage);

        TVector<NEagle::TMetaRawProfile*> rawProfiles(Reserve(rawProfile.Profiles.size()));
        FillRawProfiles(&rawProfile, rawProfiles);
    }
}

static void BM_Merge(benchmark::State& state) {
    const auto& factory = GetCodecFactory();
    Y_UNUSED(factory);
    const auto& keywordInfo = NBSYeti::BuildKeywordInfoFromProto();
    const auto& counterPacker = NBSYeti::GetCounterPacker();

    const auto storage = FetchProfiles();
    TString mainProfileId = "y7387";

    NEagle::TMultiRawProfile rawProfile;
    FillRawProfile(&rawProfile, storage);

    TVector<NEagle::TMetaRawProfile*> rawProfiles(Reserve(rawProfile.Profiles.size()));
    FillRawProfiles(&rawProfile, rawProfiles);

    for (auto _ : state) {
        auto wideProfile = MakeHolder<NEagle::TWideProfile>();
        wideProfile->Profile.ConstructInPlace(mainProfileId, keywordInfo, counterPacker);

        NBSYeti::TDataFilter dataFilter(Nothing(), Nothing(), true, false, true);
        MergeAllProfiles(wideProfile, rawProfiles, dataFilter, mainProfileId);
    }
}

static void BM_Cleanup(benchmark::State& state) {
    const auto& factory = GetCodecFactory();
    Y_UNUSED(factory);
    const auto& keywordInfo = NBSYeti::BuildKeywordInfoFromProto();
    const auto& counterPacker = NBSYeti::GetCounterPacker();

    const auto storage = FetchProfiles();
    TString mainProfileId = "y7387";

    time_t currentTime = FetchCurrentTime() + state.range(0);

    NEagle::TMultiRawProfile rawProfile;
    FillRawProfile(&rawProfile, storage);

    TVector<NEagle::TMetaRawProfile*> rawProfiles(Reserve(rawProfile.Profiles.size()));
    FillRawProfiles(&rawProfile, rawProfiles);

    for (auto _ : state) {
        state.PauseTiming();
        auto wideProfile = MakeHolder<NEagle::TWideProfile>();
        wideProfile->Profile.ConstructInPlace(mainProfileId, keywordInfo, counterPacker);
        NBSYeti::TDataFilter dataFilter(Nothing(), Nothing(), true, false, true);
        MergeAllProfiles(wideProfile, rawProfiles, dataFilter, mainProfileId);
        state.ResumeTiming();

        wideProfile->Profile->CleanUp(currentTime);
    }
}

static void BM_MergeCleanup(benchmark::State& state) {
    const auto& factory = GetCodecFactory();
    Y_UNUSED(factory);
    const auto& keywordInfo = NBSYeti::BuildKeywordInfoFromProto();
    const auto& counterPacker = NBSYeti::GetCounterPacker();

    const auto storage = FetchProfiles();
    TString mainProfileId = "y7387";

    time_t currentTime = FetchCurrentTime() + state.range(0);

    NEagle::TMultiRawProfile rawProfile;
    FillRawProfile(&rawProfile, storage);

    TVector<NEagle::TMetaRawProfile*> rawProfiles(Reserve(rawProfile.Profiles.size()));
    FillRawProfiles(&rawProfile, rawProfiles);

    for (auto _ : state) {
        auto wideProfile = MakeHolder<NEagle::TWideProfile>();
        wideProfile->Profile.ConstructInPlace(mainProfileId, keywordInfo, counterPacker);

        NBSYeti::TDataFilter dataFilter(Nothing(), Nothing(), true, false, true);
        MergeAllProfiles(wideProfile, rawProfiles, dataFilter, mainProfileId);

        wideProfile->Profile->CleanUp(currentTime);
    }
}

static void BM_ParseMergeCleanup(benchmark::State& state) {
    const auto& factory = GetCodecFactory();
    Y_UNUSED(factory);
    const auto& keywordInfo = NBSYeti::BuildKeywordInfoFromProto();
    const auto& counterPacker = NBSYeti::GetCounterPacker();

    const auto storage = FetchProfiles();
    TString mainProfileId = "y7387";

    time_t currentTime = FetchCurrentTime() + state.range(0);

    for (auto _ : state) {
        auto wideProfile = MakeHolder<NEagle::TWideProfile>();
        wideProfile->Profile.ConstructInPlace(mainProfileId, keywordInfo, counterPacker);

        NEagle::TMultiRawProfile rawProfile;
        FillRawProfile(&rawProfile, storage);

        TVector<NEagle::TMetaRawProfile*> rawProfiles(Reserve(rawProfile.Profiles.size()));
        FillRawProfiles(&rawProfile, rawProfiles);

        NBSYeti::TDataFilter dataFilter(Nothing(), Nothing(), true, false, true);
        MergeAllProfiles(wideProfile, rawProfiles, dataFilter, mainProfileId);

        wideProfile->Profile->CleanUp(currentTime);
    }
}

BENCHMARK(BM_WideProfileCreation);
BENCHMARK(BM_ProfileParsing);
BENCHMARK(BM_Merge);
BENCHMARK(BM_Cleanup)
    ->Arg(-1 * 3600 * 24 * 2) // two days ago
    ->Arg(-1 * 3600 * 24) // one day ago
    ->Arg(0) // now
    ->Arg(3600 * 24) // tomorrow
    ->Arg(3600 * 24 * 2) // 2 days in future
    ->Arg(3600 * 24 * 7) // week in future
    ->Arg(3600 * 24 * 14) // 2 weeks in future
    ->Arg(3600 * 24 * 30) // month in future
    ->Arg(3600 * 24 * 200); // half a year in future
BENCHMARK(BM_MergeCleanup)
    ->Arg(-1 * 3600 * 24 * 2) // two days ago
    ->Arg(-1 * 3600 * 24) // one day ago
    ->Arg(0) // now
    ->Arg(3600 * 24) // tomorrow
    ->Arg(3600 * 24 * 2) // 2 days in future
    ->Arg(3600 * 24 * 7) // week in future
    ->Arg(3600 * 24 * 14) // 2 weeks in future
    ->Arg(3600 * 24 * 30) // month in future
    ->Arg(3600 * 24 * 200); // half a year in future
BENCHMARK(BM_ParseMergeCleanup)
    ->Arg(-1 * 3600 * 24 * 2) // two days ago
    ->Arg(-1 * 3600 * 24) // one day ago
    ->Arg(0) // now
    ->Arg(3600 * 24) // tomorrow
    ->Arg(3600 * 24 * 2) // 2 days in future
    ->Arg(3600 * 24 * 7) // week in future
    ->Arg(3600 * 24 * 14) // 2 weeks in future
    ->Arg(3600 * 24 * 30) // month in future
    ->Arg(3600 * 24 * 200); // half a year in future
