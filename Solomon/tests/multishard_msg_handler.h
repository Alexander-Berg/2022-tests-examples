#pragma once

#include <solomon/libs/cpp/multi_shard/multi_shard.h>

namespace NSolomon {
    class TBasicMsgHandler: public NMultiShard::IMessageHandler {
    public:
        struct TShardData {
            TString Project;
            TString Cluster;
            TString Service;
            TString Data;
        };

        NMultiShard::THeader Header;
        TVector<TShardData> ShardsData;

        TBasicMsgHandler() {
        }

        bool OnHeader(NMultiShard::THeader header) override {
            Header = std::move(header);
            return true;
        }

        bool OnShardData(TString project, TString cluster, TString service, TString data) override {
            ShardsData.emplace_back(TShardData{project, cluster, service, data});
            return true;
        }

        void OnError(TString msg) override {
            ythrow yexception() << "error during decoding: " << msg;
        }

        void OnStreamEnd() override {
        }
    };
}
