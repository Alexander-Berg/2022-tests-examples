#include "mr_opts.h"

#include <saas/api/yt_pull_client/saas_yt_writer.h>

#include <mapreduce/yt/common/config.h>

#include <library/cpp/logger/global/global.h>

#include <util/generic/guid.h>
#include <util/system/env.h>

namespace NSaas {
    namespace NYTPull {
        TString FindFreeName(NYT::IClientBase& client, const TString& prefix, ui32& index) {
            TString path;
            do {
                path = prefix + ToString(index);
                ++index;
            } while (client.Exists(path));
            return path;
        }

        TScopedYtStorageBuilder::TScopedYtStorageBuilder(NYT::IClient& ytClient,
                                                         const TRtyServerTestMROpts& config,
                                                         TAtomicSharedPtr<IShardDispatcher> shardDispatcher)
            : Transaction(ytClient.StartTransaction())
            , Config(config)
            , ShardDispatcher(std::move(shardDispatcher))
        {
            Transaction->Create(Config.TestFolder, NYT::NT_MAP, NYT::TCreateOptions().IgnoreExisting(true).Recursive(true));
            CreateMasterTable(Config.MasterTablePath, Transaction);
        }

        TScopedYtStorageBuilder::~TScopedYtStorageBuilder() {
            if (Transaction) {
                try {
                    Transaction->Abort();
                    Transaction.Reset();
                } catch (...) {
                    ERROR_LOG << "Unable to abort a YT transaction: " << CurrentExceptionMessage() << Endl;
                }
            }
        }

        TString TScopedYtStorageBuilder::CreateChunk(const TVector<NRTYServer::TMessage>& messages, ui64 timestamp) {
            Y_ENSURE(Transaction);
            auto chunkPath = FindFreeName(*Transaction, Config.TestFolder + "/chunk.", NextChunkIndex);
            Transaction->Create(chunkPath, NYT::NT_TABLE);

            TSaasSlaveTableHoldingWriter writer(Transaction, chunkPath, ShardDispatcher, false);
            for (auto& message : messages) {
                writer.WriteMessage(message);
            }
            writer.Finish();

            NSaas::NYTPull::RegisterSlaveTable(Config.MasterTablePath, chunkPath, timestamp, Transaction);
            return chunkPath;
        }

        void TScopedYtStorageBuilder::AddExistingChunk(const TString& chunkPath, ui64 timestamp) {
            NSaas::NYTPull::RegisterSlaveTable(Config.MasterTablePath, chunkPath, timestamp, Transaction);
        }

        void TScopedYtStorageBuilder::Commit() {
            Transaction->Commit();
            Transaction.Reset();
        }

        TScopedYtStorage::TScopedYtStorage(TRtyServerTestMROpts config,
                                           TAtomicSharedPtr<IShardDispatcher> shardDispatcher)
            : Config(std::move(config))
            , ShardDispatcher(std::move(shardDispatcher))
        {
            YtClient = NYT::CreateClient(Config.MrServer,  NYT::TCreateClientOptions().Token(Config.MrToken));
        }

        TScopedYtStorage::~TScopedYtStorage() {
            try {
                YtClient->Remove(Config.TestFolder, NYT::TRemoveOptions().Recursive(true));
            } catch (...) {
                ERROR_LOG << "Unable to remove a test folder from YT: " << CurrentExceptionMessage() << Endl;
            }
        }

        TScopedYtStorageBuilder TScopedYtStorage::MakeBuilder() {
            return TScopedYtStorageBuilder(*YtClient, Config, ShardDispatcher);
        }

        NYT::IClient& TScopedYtStorage::GetYtClient() {
            return *YtClient;
        }

        TRtyServerTestMROpts CreateMROpts(const TString& testFolderName) {
            TRtyServerTestMROpts opts;

            const TString envYtServerName = GetEnv("YT_TESTS_HOST");
            opts.MrServer = envYtServerName.empty() ? "hahn.yt.yandex.net" : envYtServerName;
            opts.MrToken = NYT::TConfig::Get()->Token;
            opts.TestFolder = "//home/saas/tests/" + testFolderName + "-" + CreateGuidAsString();
            opts.MasterTablePath = opts.TestFolder + "/master_table";

            return opts;
        }
    }
}
