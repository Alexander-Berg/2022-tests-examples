#pragma once

#include <saas/library/sharding/sharding.h>
#include <mapreduce/yt/interface/client.h>

namespace NSaas {
    namespace NYTPull {
        struct TRtyServerTestMROpts {
            TString MrServer;
            TString MrToken;
            TString TestFolder;
            TString MasterTablePath;
        };

        struct TScopedYtStorageBuilder {
            TScopedYtStorageBuilder(NYT::IClient&, const TRtyServerTestMROpts&, TAtomicSharedPtr<IShardDispatcher>);
            ~TScopedYtStorageBuilder();

            TString CreateChunk(const TVector<NRTYServer::TMessage>& messages, ui64 timestamp);
            void AddExistingChunk(const TString& path, ui64 timestamp);
            void Commit();

        private:
            NYT::ITransactionPtr Transaction;
            const TRtyServerTestMROpts& Config;
            TAtomicSharedPtr<IShardDispatcher> ShardDispatcher;
            ui32 NextChunkIndex = 0;
        };

        struct TScopedYtStorage {
            TScopedYtStorage(TRtyServerTestMROpts config, TAtomicSharedPtr<IShardDispatcher>);
            ~TScopedYtStorage();

            TScopedYtStorageBuilder MakeBuilder();
            NYT::IClient& GetYtClient();

        private:
            const TRtyServerTestMROpts Config;
            NYT::IClientPtr YtClient;
            TAtomicSharedPtr<IShardDispatcher> ShardDispatcher;
        };

        TRtyServerTestMROpts CreateMROpts(const TString& testFolderName);
    }
}
