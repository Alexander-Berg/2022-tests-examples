#pragma once
#include <util/system/tempfile.h>
#include <util/folder/tempdir.h>
#include <util/generic/array_ref.h>
#include <util/system/tempfile.h>
#include <util/folder/path.h>
#include <util/string/split.h>
#include <util/generic/fwd.h>
#include <ads/pytorch/deploy/lib/sthash_embedding.h>
#include <ads/pytorch/deploy/lib/util.h>
#include <ads/pytorch/deploy/lib/trait.h>
#include <ads/pytorch/deploy/math_lib/vector_math.h>


namespace NPyTorchTransportTests {
    template <NPytorchTransport::EEmbeddingProcessMode Mode=NPytorchTransport::EEmbeddingProcessMode::FLOAT>
    class EmbeddingTableDumpGuard {
        TTempDir Guard;
    public:
        EmbeddingTableDumpGuard(
                const THashMap<ui64, TVector<float>> &hashTable,
                const ui64 &dim,
                const TString &computeMode
        )
        : Guard()
        {
            const auto &modelDir = Name();

            for (const auto &[key, value] : hashTable) {
                Y_ENSURE(value.size() == static_cast<ui64>(dim));
            }

            using namespace NPytorchTransport;

            TLoadedHashMap<Mode> dumpedHashTable;
            {
                TTempDir BlobDirGuard;
                auto path = JoinFsPaths(BlobDirGuard.Name(), "dumpedRawTable");
                SerializeEmbeddings(hashTable, path);
                dumpedHashTable = LoadSerializedEmbeddings<Mode>(path);
            }

            DumpToSTHash(dumpedHashTable, JoinFsPaths(modelDir, "processed_table").c_str());

            {
                TFileOutput dimOut(JoinFsPaths(modelDir, "dim_info"));
                dimOut << dim;
                dimOut.Finish();
            }
            {
                TFileOutput computeModeOut(JoinFsPaths(modelDir, "compute_mode"));
                computeModeOut << computeMode;
                computeModeOut.Finish();
            }
            {
                TFileOutput dtypeOut(JoinFsPaths(modelDir, "embedding_dtype"));
                switch(Mode) {
                    case EEmbeddingProcessMode::FLOAT: {
                        dtypeOut << "float";
                        break;
                    }
                    case EEmbeddingProcessMode::HALF: {
                        dtypeOut << "half";
                        break;
                    }
                    case EEmbeddingProcessMode::UINT8_LENGTH: {
                        dtypeOut << "uint8_length";
                        break;
                    }
                    default: {
                        ythrow yexception() << Mode << " mode is not implemented";
                    }
                }
                dtypeOut.Finish();
            }
        }

        ~EmbeddingTableDumpGuard() = default;

        inline TString Name() const {
            return Guard.Name();
        }
    };
}
