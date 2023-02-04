#include <ads/bsyeti/libs/yt_storage/tests/manual_test/mutators.h>

#include <ads/bsyeti/libs/codec_factory/factory.h>

#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <util/string/builder.h>
#include <util/random/mersenne.h>

namespace NBSYeti {
    class TSerializableProfileTests: public ::testing::Test {
    public:
        void SetUp() {
            Seed = TInstant::Now().TimeT();
            Generator = TMersenne<ui64>(Seed);

            KeywordInfo = BuildKeywordInfoFromProto();
            CounterPacker = TCounterPackerBuilder::Build(BuildCounterInfoFromProto());
            Mutators = PrepareMutators(Factory, {NZstdFactory::TCodecFactory::TrainedZtd6Codec2Xdelta3Id});
        }

        void Mutate(TSerializableProfile& profile, const TVector<TString>& excluded) {
            for (ui64 i = 0u; i < Mutators.size(); ++i) {
                auto& mutator = Mutators[i];
                if (std::find(excluded.begin(), excluded.end(), mutator->GetProtoField()) != excluded.end()) {
                    continue;
                }
                ui64 t = Generator.Uniform(1400000000u, 1600000000u);
                i64 v = (1ull << 60);
                mutator->Mutate(profile, t, Generator.Uniform(-v, v + 1));
            }
        }

        NZstdFactory::TCodecFactory Factory;
        TKeywordInfo KeywordInfo;
        TCounterPacker CounterPacker;
        ui64 Seed;
        TMersenne<ui64> Generator;
        TVector<IMutatorPtr> Mutators;
    };
}
