#include "stats.h"

#include <balancer/kernel/custom_io/chunkio.h>
#include <balancer/kernel/http/parser/http.h>
#include <balancer/kernel/http/parser/httpdecoder.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/json/json_reader.h>

namespace NSrvKernel::NTesting {

    TStatsFixture::TStatsFixture(TStatsFixtureOpts opts) {
        Manager_.SetWorkersCount(opts.Workers, opts.SpecWorkers);
    }

    THashMap<TString, NJson::TJsonValue> TStatsFixture::Unistat() {
        TChunksOutputStream out;

        Manager_.WriteResponseNoWait(TSharedStatsManager::TResponseType::Unistat, &out);

        NJson::TJsonValue json;
        NJson::ReadJsonTree(StrInplace(out.Chunks()), &json);

        THashMap<TString, NJson::TJsonValue> stats;
        for (const NJson::TJsonValue& value : json.GetArray()) {
            const TString& signalName = value[0].GetString();
            if (stats.find(signalName) != stats.end()) {
                UNIT_FAIL("repeated signal name: " << signalName);
            }
            stats[signalName] = value[1];
        }

        return stats;
    }
}
