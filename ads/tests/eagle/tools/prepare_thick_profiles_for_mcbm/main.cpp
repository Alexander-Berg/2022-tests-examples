#include <ads/bsyeti/libs/codec_factory/factory.h>
#include <ads/bsyeti/libs/yt_operations/acl.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>
#include <ads/bsyeti/libs/yt_storage/collect_state.h>
#include <ads/bsyeti/libs/yt_storage/scheme.pb.h>
#include <ads/bsyeti/protos/profile.pb.h>
#include <ads/bsyeti/libs/experiments/builder.h>
#include <ads/bsyeti/libs/profile/profile.h>

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/operation.h>
#include <mapreduce/yt/util/temp_table.h>

#include <library/cpp/blockcodecs/codecs.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/logger/global/global.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/svnversion/svnversion.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/retry/retry.h>
#include <library/cpp/yson/node/node.h>

#include <util/stream/file.h>
#include <util/system/fs.h>
#include <util/stream/null.h>
#include <util/system/hostname.h>
#include <util/system/shellcommand.h>


#define MAX_PROFILES_WITH_FIELD 50


class TGrepThickProfileMapper : public NYT::IMapper<NYT::TTableReader<NBSYeti::TProfileTableRecord>, NYT::TTableWriter<NYT::TNode>> {
public:
    TGrepThickProfileMapper() = default;

    TGrepThickProfileMapper(const THashMap<ui64, TString>& storedFactory)
        : NYT::IMapper<NYT::TTableReader<NBSYeti::TProfileTableRecord>, NYT::TTableWriter<NYT::TNode>>()
        , StoredFactory(storedFactory)
    {
    }

    void Do(TReader* reader, TWriter* writer) override {
        NZstdFactory::TCodecFactory factory{StoredFactory};
        for (; reader->IsValid(); reader->Next()) {
            const auto& row = reader->GetRow();
            ui64 size = row.ByteSize();
            if (size > 100 * 1024) { // we looking for profiles with size more than 100KB
                const NCodecs::ICodec* codec = nullptr;
                codec = factory.Get(row.GetCodecId());
                Y_ENSURE(codec != nullptr, "Codec wasn't initialized.\n");

                auto&& [state, _] = NBSYeti::CollectState(row, factory);
                NBSYeti::TProfileProtoPackPtr protos = NBSYeti::ParseProfileProtoPack(state);

                THashMap<int, int> repFieldCounts;

                for (ui32 ytFieldNumber = 0; ytFieldNumber < protos->Size(); ++ytFieldNumber) {
                    auto& msg = protos->Get(ytFieldNumber);
                    const auto* descriptor = msg.GetDescriptor();
                    const auto* reflection = msg.GetReflection();

                    for (int i = 0; i < descriptor->field_count(); ++i) {
                        const auto* field = descriptor->field(i);
                        if (field->is_repeated() && field->cpp_type() == NProtoBuf::FieldDescriptor::CPPTYPE_MESSAGE) {
                            size_t msgCount = reflection->FieldSize(msg, field);
                            if (msgCount > 0) {
                                // TODO: maybe for items and counters we shoud use not only count?
                                repFieldCounts[i] += msgCount;
                            };
                        }
                    }
                }

                TString serialized = row.SerializeAsString();
                for (auto [fieldNumber, fieldCount] : repFieldCounts) {
                    NYT::TNode result;
                    result["UniqID"] = row.GetUniqId();
                    result["FieldNumber"] = static_cast<ui64>(fieldNumber);
                    result["FieldCount"] = static_cast<ui64>(fieldCount);
                    result["Row"] = serialized;
                    writer->AddRow(result);
                }
            }
        }
    }
private:
    THashMap<ui64, TString> StoredFactory;
public:
    Y_SAVELOAD_JOB(StoredFactory);
};
REGISTER_MAPPER(TGrepThickProfileMapper);


class TFilterReducer : public NYT::IAggregatorReducer<NYT::TTableRangesReader<NYT::TNode>, NYT::TTableWriter<NYT::TNode>> {
public:
    void Do(NYT::TTableRangesReader<NYT::TNode>* rangesInput, NYT::TTableWriter<NYT::TNode>* output) override {
        for (; rangesInput->IsValid(); rangesInput->Next()) {
            auto& input = rangesInput->GetRange();

            size_t stored = 0;
            size_t seen = 0;
            size_t printed = 0;
            bool toManyProfilesPrintAll = false;
            size_t threshold = 0;

            ui64 fieldNumber = 100000;
            THashMap<TString, TString> profileStorage;
            TMap<ui64, TVector<TString>> profilesWithCount;

            for (; input.IsValid(); input.Next()) {
                NYT::TNode row = input.GetRow();
                if (100000 == fieldNumber) {
                    fieldNumber = row["FieldNumber"].AsUint64();
                }
                Y_ENSURE(row["FieldNumber"].AsUint64() == fieldNumber);
                ++seen;

                if (row["FieldCount"].AsUint64() >= threshold) {
                    if (toManyProfilesPrintAll) {
                        output->AddRow(row);
                    } else {
                        profileStorage[row["UniqID"].AsString()] = row["Row"].AsString();
                        profilesWithCount[row["FieldCount"].AsUint64()].push_back(row["UniqID"].AsString());
                        ++stored;

                        if (seen >= 20 * MAX_PROFILES_WITH_FIELD) {
                            // we do this, because we can have to many profiles in memory of one reduce job. Optimizing it.
                            if (stored >= 5 * MAX_PROFILES_WITH_FIELD && profilesWithCount.size() != 1) {
                                // cleanup
                                TVector<ui64> toDrop;
                                ui64 culm = 0;
                                for (auto it = profilesWithCount.rbegin(); it != profilesWithCount.rend(); ++it) {
                                    if (culm >= MAX_PROFILES_WITH_FIELD) {
                                        toDrop.push_back(it->first);
                                        for (const auto& uniqId : it->second) {
                                            profileStorage.erase(uniqId);
                                        }
                                        stored -= it->second.size();
                                    } else {
                                        culm += it->second.size();
                                    }
                                }
                                for (auto i : toDrop) {
                                    Y_ENSURE(profilesWithCount.erase(i));
                                }
                                threshold = profilesWithCount.begin()->first;
                            }
                            if (stored >= 20 * MAX_PROFILES_WITH_FIELD) {
                                // we have many profiles with maximum value. We want to print them all.
                                toManyProfilesPrintAll = true;
                            }
                        }
                    }
                }
            }

            for (auto it = profilesWithCount.rbegin(); it != profilesWithCount.rend(); ++it) {
                if (printed >= MAX_PROFILES_WITH_FIELD) {
                    // yes. we want ALL profiles with similar counts. So end here.
                    return;
                }
                for (const auto& uniqId : it->second) {
                    NYT::TNode result;
                    result["UniqID"] = uniqId;
                    result["FieldNumber"] = fieldNumber;
                    result["FieldCount"] = it->first;
                    result["Row"] = profileStorage[uniqId];
                    output->AddRow(result);
                    ++printed;
                }
            }
        }
    }
};
REGISTER_REDUCER(TFilterReducer)

void FinalGrepProfiles(TIntrusivePtr<NYT::ITransaction, TDefaultIntrusivePtrOps<NYT::ITransaction>>& tx, TString tableName, TVector<TString>& storage) {
    THashMap<TString, THashSet<ui64>> profileFields;
    THashMap<ui64, TVector<TString>> fieldUniqIds;
    {
        INFO_LOG << "First read table started" << "\n";
        auto reader = tx->CreateTableReader<NYT::TNode>(tableName);
        for (; reader->IsValid(); reader->Next()) {
            const auto& row = reader->GetRow();
            profileFields[row["UniqID"].AsString()].insert(row["FieldNumber"].AsUint64());
            fieldUniqIds[row["FieldNumber"].AsUint64()].push_back(row["UniqID"].AsString());
        }
        INFO_LOG << "First read complete" << "\n";
    }
    THashSet<TString> takenUniqIds;
    THashMap<ui64, i64> fieldLimits;
    ui64 border = static_cast<ui64>(1.1 * MAX_PROFILES_WITH_FIELD);

    // zero run. Setting limits
    for (auto [fieldNum, profiles] : fieldUniqIds) {
        fieldLimits[fieldNum] = Min(profiles.size(), border);
    }
    INFO_LOG << "Total we have " << fieldLimits.size() << " fields" << "\n";

    auto isOk = [&fieldLimits]() -> bool {
        for (const auto [fieldNum, count] : fieldLimits) {
            Y_UNUSED(fieldNum);
            if (count > 0) {
                return false;
            }
        }
        return true;
    };

    auto newCandidate = [&fieldLimits]() -> ui64 {
        ui64 candidate = 10000;
        i64 candSize = 1000000;
        for (const auto [fieldNum, count] : fieldLimits) {
            if (count > 0) {
                if (count < candSize) {
                    candidate = fieldNum;
                    candSize = count;
                }
            }
        }
        return candidate;
    };

    while (!isOk()) {
        // filter iteration. Take all uniqs, with field, with minimum profiles count
        ui64 fieldNum = newCandidate();
        for (const auto& profile : fieldUniqIds[fieldNum]) {
            if (0 == fieldLimits[fieldNum]) {
                break;
            }
            auto res = takenUniqIds.emplace(profile);
            if (res.second) {
                // if this profile is new for us, we need to remember, that we already have some fields
                for (const ui64 field: profileFields[profile]) {
                    fieldLimits[field] -= 1;
                }
            }
        }
        Y_ENSURE(0 == fieldLimits[fieldNum]);
        INFO_LOG << "Chosen " << takenUniqIds.size() << " profiles after filter iteration" << "\n";
    }
    INFO_LOG << "Total chosen " << takenUniqIds.size() << " profiles" << "\n";

    {
        INFO_LOG << "Second read table started (searching profiles)" << "\n";
        auto reader = tx->CreateTableReader<NYT::TNode>(tableName);
        for (; reader->IsValid(); reader->Next()) {
            const auto& row = reader->GetRow();
            if (takenUniqIds.contains(row["UniqID"].AsString())) {
                storage.push_back(row["Row"].AsString());
                takenUniqIds.erase(row["UniqID"].AsString());
                INFO_LOG << "Taken " << row["UniqID"].AsString() << " with size " << row["Row"].AsString().size()
                         << "; " << takenUniqIds.size() << " profiles need to find" << "\n";
                if (takenUniqIds.empty()) {
                    break;
                }
            }
        }
        INFO_LOG << "Read complete" << "\n";
    }

    TStringBuilder toPrint;
    for (const auto& it : fieldLimits) {
        toPrint << " " << it.first;
    }
    INFO_LOG << "We have profiles with fields" << toPrint << "\n";
    INFO_LOG << "Finally selected " << storage.size() << " profiles" << "\n";
}

void SaveProfilesToFile(TString filename, TVector<TString>& storage) {
    INFO_LOG << "Saving profiles to file " << filename << "..." << "\n";

    TOFStream oFile(filename);
    size_t curTime = TInstant::Now().TimeT();
    oFile.Write(reinterpret_cast<char*>(&curTime), 8); // first 8 bytes is the time
    for (const auto& profile : storage) {
        ui64 pSize = profile.size();
        oFile.Write(reinterpret_cast<char*>(&pSize), 8);
        oFile.Write(profile.data(), pSize);
    }
    oFile.Finish();

    INFO_LOG << "...Saved." << "\n";
}

void UploadToSandbox(TString filename) {
    INFO_LOG << "Uploading " << filename << " to Sandbox..." << "\n";

    TString cmdStr = TStringBuilder{} << "ya upload -T=BIGB_BENCHMARK_TESTDATA --ttl=inf " << filename;
    TShellCommand cmd(cmdStr);
    if (cmd.Run().Wait().GetStatus() != TShellCommand::SHELL_FINISHED) {
        ythrow yexception() << "shell command " << cmdStr << " finished with errors: " << cmd.GetError();
    }

    INFO_LOG << "...Uploaded." << "\n";
}

void RemoveFile(TString filename) {
    INFO_LOG << "Removing file " << filename << "..." << "\n";

    if (!NFs::Remove(filename)) {
        ERROR_LOG << "Something went wrong, file not removed!" << "\n";
    } else {
        INFO_LOG << "...Removed." << "\n";
    }
}

int main(int argc, const char** argv) {
    NYT::Initialize(argc, argv);

    NLastGetopt::TOpts options;
    int verbose = 6;
    options.AddCharOption('v', "verbose")
        .StoreResult(&verbose);
    TString global = "cerr";
    options.AddCharOption('l', "log")
        .StoreResult(&global);

    NLastGetopt::TOptsParseResult optParsing(&options, argc, argv);
    DoInitGlobalLog(CreateLogBackend(global, (ELogPriority)verbose, true));

    NZstdFactory::TCodecFactory factory{"/dicts_cache/"};
    INFO_LOG << "Possible Codecs count=" << factory.ListPossibleCodecs().size() << "\n";
    auto client = NYT::CreateClient("hahn");
    NYT::TNode mrSpec;
    mrSpec["annotations"]["script_name"] = "Grep Thick Profiles";
    mrSpec["data_size_per_map_job"] = 536870912u;
    NBSYeti::AddBigbAclRecord(mrSpec);

    auto tx = client->StartTransaction();
    NYT::TTempTable tempTable(tx, "bigb_bm_prepared_profiles");
    TString importTableName;
    {
        TString dirName = "//home/bigb/production/backup/profiles";
        for (const auto& table : tx->List(dirName)) {
            if (importTableName < table.AsString()) {
                importTableName = table.AsString();
            }
        }
        importTableName = dirName + "/" + importTableName;
        INFO_LOG << "Using hahn table " << importTableName << " as source table of profiles" << "\n";
    }
    INFO_LOG << "Starting grep MR operation" << "\n";
    tx->MapReduce(
        NYT::TMapReduceOperationSpec{}
            .AddInput<NBSYeti::TProfileTableRecord>(NYT::TRichYPath(importTableName))
            .AddOutput<NYT::TNode>(tempTable.Name())
            .ReduceBy({"FieldNumber"}),
        new TGrepThickProfileMapper(factory.Store()),
        new TFilterReducer(),
        NYT::TOperationOptions{}.Spec(mrSpec)
    );
    INFO_LOG << "MR operation complete" << "\n";

    TVector<TString> storage;
    FinalGrepProfiles(tx, tempTable.Name(), storage);
    tx->Commit();

    TString filename = "/tmp/thickProfilesForBenchmark";
    SaveProfilesToFile(filename, storage);
    UploadToSandbox(filename);
    RemoveFile(filename);

    return 0;
}
