#include "mutators.h"

#include <ads/bsyeti/libs/backtrace/backtrace.h>
#include <ads/bsyeti/libs/yt_rpc_client/master_client.h>
#include <ads/bsyeti/libs/yt_storage/load_profiles.h>
#include <ads/bsyeti/libs/yt_storage/save_profiles.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>
#include <ads/bsyeti/libs/codec_factory/factory.h>

#include <yt/yt/core/logging/log_manager.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/string/builder.h>
#include <util/random/mersenne.h>

using namespace NYTRpc;
using namespace NYT::NTableClient;
using namespace NBSYeti;

const int ATTEMPTS = 5;

using TProfiles = THashMap<TString, THolder<TSerializableProfile>>;

class TProfilesOperator {
public:
    TProfilesOperator(const TString& path, const TString& /*counters*/, IMasterMultiClientPtr client)
        : CounterPacker(TCounterPackerBuilder::Build(BuildCounterInfoFromProto()))
        , Seed(TInstant::Now().TimeT())
        , Generator(Seed)
        , Mutators(PrepareMutators(Factory, {NZstdFactory::TCodecFactory::TrainedZtd6Codec2Xdelta3Id}))
        , Client(client)
        , Path(path)
    {
        KeywordInfo = BuildKeywordInfoFromProto();

        Timestamp = client->Master->StartTransaction(NYT::NTransactionClient::ETransactionType::Tablet).Get().ValueOrThrow()->GetStartTimestamp();
        INFO_LOG << "commit timestamp: " << Timestamp << "\n";

        NOTICE_LOG << "seed (use it to reproduce data): " << Seed << "\n";
    }

    TProfiles LoadProfiles(const TVector<TString>& pids, bool retry = true) {
        TYtProfileLoader loader(Client, Path);
        TProfiles gotProfiles;
        for (int i = 0; i < ATTEMPTS; ++i) {
            try {
                gotProfiles.clear();
                loader.LoadProfiles(
                    pids,
                    Timestamp,
                    [&](const TString& pid, auto&& raw) {
                        gotProfiles[pid] = MakeHolder<TSerializableProfile>(pid, KeywordInfo, CounterPacker, std::move(raw));
                        gotProfiles[pid]->MarkClean();
                    });
                return gotProfiles;
            } catch (...) {
                WARNING_LOG << "Failed to load profiles " << NBSYeti::CurrentExceptionWithBacktrace();
            }
            if (retry) {
                Sleep(TDuration::Seconds(5));
            } else {
                break;
            }
        }
        ythrow yexception() << "Failed to commit after several attempts";
    }

    void ApplyMutators(TProfiles& profiles, const THashSet<TString>& excludedMutators) {
        for (auto mutator : Mutators) {
            if (!excludedMutators.contains(mutator->GetProtoField())) {
                ApplyMutator(profiles, mutator);
            }
        }
    }

    void ApplyMutator(TProfiles& profiles, const IMutatorPtr& mutator) {
        for (auto& p : profiles) {
            auto& profile = *p.second;
            ui64 t = Generator.Uniform(1400000000u, 1600000000u);
            i64 v = (1ull << 60);
            mutator->Mutate(profile, t, Generator.Uniform(-v, v + 1));
        }
    }

    void WriteProfiles(const TProfiles& profiles) {
        TYtProfileSaver saver;
        TMetrics metrics;
        for (auto& p : profiles) {
            auto& profile = *p.second;

            Y_ENSURE(profile.UserProfileData);
            saver.AddUserProfileDelta(profile.ProfileId, profile.ComputeMutationAndApply(), &metrics);
            profile.MarkClean();
        }
        auto result = saver.Finish();

        for (int i = 0; i < ATTEMPTS; ++i) {
            try {
                NYT::NApi::TModifyRowsOptions opts;
                opts.RequireSyncReplica = false;

                if (!result->Mutations.empty()) {
                    NBigRT::TMutableRowOperator<TUserProfile> op(Path);
                    auto tx = Client->Master->StartTransaction(NYT::NTransactionClient::ETransactionType::Tablet).Get().ValueOrThrow();
                    op.Write(tx, result->Keys, result->Mutations, opts);
                    Timestamp = tx->Commit().Get().ValueOrThrow().CommitTimestamps.Timestamps[0].second;
                }
                if (!result->Rows.empty()) {
                    auto range = MakeSharedRange(std::move(result->Rows), std::move(result->Buffer));
                    if (range.size()) {
                        auto tx = Client->Master->StartTransaction(NYT::NTransactionClient::ETransactionType::Tablet).Get().ValueOrThrow();
                        tx->ModifyRows(Path, result->NameTable, range, opts);
                        Timestamp = tx->Commit().Get().ValueOrThrow().CommitTimestamps.Timestamps[0].second;
                    }
                }

                INFO_LOG << "commit timestamp: " << Timestamp << "\n";
                return;
            } catch (...) {
                WARNING_LOG << NBSYeti::CurrentExceptionWithBacktrace();
            }
            Sleep(TDuration::Seconds(5));
        }
        ythrow yexception() << "Failed to commit after several attempts";
    }

    NZstdFactory::TCodecFactory Factory;
    TKeywordInfo KeywordInfo;
    TCounterPacker CounterPacker;
    ui64 Seed;
    TMersenne<ui64> Generator;
    TVector<IMutatorPtr> Mutators;
    IMasterMultiClientPtr Client;
    TString Path;
    ui64 Timestamp;
};

void ManualTest(const TString& path, const TString& counters, IMasterMultiClientPtr client) {
    TVector<TString> pids;
    for (ui64 i = 1; i <= 10; ++i) {
        pids.push_back(TStringBuilder{} << "y" << i);
    }
    TProfilesOperator op(path, counters, client);
    for (i64 i = 0; i < 1000; ++i) {
        auto& mutator = op.Mutators[i % op.Mutators.size()];
        NOTICE_LOG << "iteration: " << i << ", mutator: " << mutator->GetProtoField() << "\n";

        TProfiles profiles;

        [&]() { // Preload
            try {
                profiles = op.LoadProfiles(pids);
                return;
            } catch (...) {
                WARNING_LOG << NBSYeti::CurrentExceptionWithBacktrace();
            }

            for (const auto& [pid, profile] : profiles) {
                if (!profile) {
                    INFO_LOG << "MARK\n";
                    Y_FAIL();
                    continue;
                }
                if (auto creationTime = profile->GetCreationTime(); creationTime) {
                    TProfile baseProfile(pid, op.KeywordInfo, op.CounterPacker); // empty profile to compare with
                    baseProfile.TrySetCreationTime(creationTime);
                    bool hasSomethingExceptCreationTime = false;
                    for (const auto& mutator : op.Mutators) {
                        try {
                            mutator->AssertEqual(baseProfile, *profile);
                        } catch (...) {
                            hasSomethingExceptCreationTime = true;
                            break;
                        }
                    }
                    Y_ENSURE(hasSomethingExceptCreationTime);
                }
                profile->TrySetCreationTime(op.Seed);
            }
        }();

        op.ApplyMutator(profiles, mutator);

        // Change && commit
        op.WriteProfiles(profiles);

        auto loadAfterSaveAndComparision = [&]() {
            TProfiles gotProfiles = op.LoadProfiles(pids);
            for (const TString& p : pids) {
                for (const auto& m : op.Mutators) {
                    m->AssertEqual(*profiles[p], *gotProfiles[p]);
                }
            }
        };
        // Load after save && comparision
        loadAfterSaveAndComparision();
    }
}

void ManualTestCodecs(const TString& path, const TString& counters, IMasterMultiClientPtr client) {
    TVector<TString> pids;
    for (ui64 i = 1; i <= 10; ++i) {
        pids.push_back(TStringBuilder{} << "y" << i);
    }
    TProfilesOperator op(path, counters, client);
    THashSet<TString> excluded = {"", "CodecID"};
    using namespace NZstdFactory;
    TVector<ui64> codecs = {TCodecFactory::ZlibCodecId, TCodecFactory::TrainedZtd6Codec1Id, TCodecFactory::TrainedZtd6Codec2Id};
    for (ui64 codec : codecs) {
        TProfiles profiles = op.LoadProfiles(pids);

        op.ApplyMutators(profiles, excluded);

        for (auto& [pid, profile] : profiles) {
            profile->SetEncodingCodec(codec);
        }

        op.WriteProfiles(profiles);

        TProfiles gotProfiles = op.LoadProfiles(pids);
        auto assertEqual = [&]() {
            for (const TString& p : pids) {
                for (const auto& m : op.Mutators) {
                    m->AssertEqual(*profiles[p], *gotProfiles[p]);
                }
            }
        };
        assertEqual();

        for (const auto& [pid, profile] : profiles) {
            EXPECT_EQ(codec, profile->GetEncodingCodec());
        }

        op.ApplyMutators(profiles, excluded);

        op.WriteProfiles(profiles);

        gotProfiles = op.LoadProfiles(pids);
        assertEqual();

        for (const auto& [pid, profile] : profiles) {
            EXPECT_EQ(codec, profile->GetEncodingCodec());
        }
    }
}

void ManualTestXdeltaCodec(const TString& path, const TString& counters, IMasterMultiClientPtr client) {
    using namespace NZstdFactory;

    TVector<TString> pids;
    for (ui64 i = 1; i <= 10; ++i) {
        pids.push_back(TStringBuilder{} << "y" << i);
    }
    TProfilesOperator op(path, counters, client);
    THashSet<TString> excluded = {"", "CodecID"};

    auto codec = TCodecFactory::TrainedZtd6Codec2Id;
    TProfiles profiles = op.LoadProfiles(pids);
    op.ApplyMutators(profiles, excluded);

    for (auto& [pid, profile] : profiles) {
        profile->SetEncodingCodec(codec);
    }
    op.WriteProfiles(profiles);

    TProfiles gotProfiles = op.LoadProfiles(pids);
    auto assertEqual = [&](auto& expected, auto& actual) {
        for (const TString& p : pids) {
            for (const auto& m : op.Mutators) {
                m->AssertEqual(*expected[p], *actual[p]);
            }
        }
    };
    for (const auto& [pid, profile] : gotProfiles) {
        EXPECT_EQ(codec, profile->GetEncodingCodec());
    }
    assertEqual(profiles, gotProfiles);

    codec = TCodecFactory::TrainedZtd6Codec2Xdelta3Id; // codecId 3 could be set for user-profiles only
    for (auto& [pid, profile] : gotProfiles) {
        profile->SetEncodingCodec(codec);
    }
    op.WriteProfiles(gotProfiles);

    profiles = op.LoadProfiles(pids);
    for (const auto& [pid, profile] : gotProfiles) {
        EXPECT_EQ(codec, profile->GetEncodingCodec());
    }
    assertEqual(profiles, gotProfiles);

    EXPECT_NO_THROW(op.LoadProfiles(pids, false)); // UserProfile supports xdelta unless legacy NBSYeti::SerializableProfile
}

void ManualTestLoadSaveUserProfile(const TString& path, const TString& counters, IMasterMultiClientPtr client) {
    TVector<TString> pids;
    for (ui64 i = 1; i <= 10; ++i) {
        pids.push_back(TStringBuilder{} << "y" << i);
    }
    TProfilesOperator op(path, counters, client);
    THashSet<TString> excluded = {""};
    for (ui64 i = 1; i <= 100; ++i) {
        TProfiles profiles = op.LoadProfiles(pids);
        op.ApplyMutators(profiles, excluded);
        op.WriteProfiles(profiles);
    }
}

void ManualTestLoadProfilesWithWrongCodecId(const TString& path, const TString& counters, IMasterMultiClientPtr client) {
    TVector<TString> pids {"duid/1009822503601904909"};
    TProfilesOperator op(path, counters, client);
    EXPECT_THROW(op.LoadProfiles(pids, false), yexception);
    EXPECT_THROW(op.LoadProfiles(pids, false), yexception);
}
