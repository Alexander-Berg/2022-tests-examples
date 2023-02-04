#include <ads/quality/adv_machine/lib/quorums/hnsw/index/simple/reader/simple_reader.h>
#include <ads/quality/adv_machine/lib/quorums/hnsw/index/simple/writer/simple_writer.h>

#include <ads/quality/adv_machine/lib/quorums/hnsw/protos/hnsw.pb.h>

#include <ads/quality/adv_machine/lib/common/protos/candidates.pb.h>
#include <ads/quality/adv_machine/lib/config/daemon/protos/daemon_config.pb.h>
#include <ads/quality/adv_machine/lib/embeddings/protos/embeddings.pb.h>
#include <ads/quality/adv_machine/lib/mr_index/modifiers/modifiers.h>
#include <ads/quality/adv_machine/lib/protobuf/pb_utils.h>
#include <ads/quality/adv_machine/lib/stage_runner/doc_container/doc_container.h>
#include <ads/quality/adv_machine/lib/vector_compact/lib_v4/protos/vector_compact.pb.h>
#include <ads/quality/adv_machine/lib/vector_compact/lib_v4/reader/reader.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/fwd.h>
#include <util/generic/ptr.h>
#include <util/generic/xrange.h>
#include <util/memory/pool.h>
#include <util/random/fast.h>
#include <util/system/fstat.h>

#include <limits>

using namespace NAdvMachine;

TVector<float> GetRandomFloatVector(TReallyFastRng32& rng, int size) {
    TVector<float> result(size);
    for (auto i : xrange(size)) {
        result[i] = rng.GenRandReal2();
    }
    return result;
}

TBannerIndexProfile GenerateProfile(const ui32 docId, const size_t modelId, TConstArrayRef<float> vector) {
    TBannerIndexProfile result;
    result.SetBannerID(ToString(docId * 100));

    auto& embedding = *result.MutableModelEmbeddings()->AddEmbeddings();
    embedding.SetVectorID(modelId);
    embedding.SetCompressedVector(TString {
        reinterpret_cast<const char*>(vector.begin()),
        reinterpret_cast<const char*>(vector.end()),
    });
    embedding.MutableCompressionParams()->SetElementType(NEmbeddings::TCompressionParams::ET_FLOAT);

    return result;
}

TVector<TBannerIndexProfile> GenerateData(TReallyFastRng32& rng, const size_t modelId, const size_t cnt, const ui32 vectorSize) {
    TVector<TBannerIndexProfile> result;

    TVector<TVector<float>> vectors;
    for ([[maybe_unused]] const auto i : xrange(cnt / 2)) {
        vectors.push_back(GetRandomFloatVector(rng, vectorSize));
    }

    for ([[maybe_unused]] const auto i : xrange(cnt)) {
        result.push_back(GenerateProfile(i, modelId, vectors[i / 2]));
    }
    return result;
}

void ApplyModifiers(const TFsPath& workDir, const TAdvMachineDaemonConfig& daemonConfig, TArrayRef<TBannerIndexProfile> profiles) {
    auto compactVectors = MakeAtomicShared<NCompact::TCompactBannerVectorsWriter>(daemonConfig, workDir, 0u /* batch id */);
    auto modifiers = GetMrBannerModifiersFromConfig(daemonConfig, compactVectors);

    for (const auto& modifier : modifiers) {
        for (const auto docId : xrange(profiles.size())) {
            modifier->Modify(docId, profiles[docId]);
        }
    }
}

void BuildHnsw(const THnswQuorumParameters& quorumParams, const TFsPath& compactVectorPath, const TFsPath& dstHnswDir) {
    const auto vectorReader = NCompactV4::TGenericVectorReader(compactVectorPath);
    TSimpleKnnWriter hnswWriter(quorumParams, vectorReader);
    hnswWriter.Build(dstHnswDir);
}

void TestHnsw(const THnswQuorumParameters& quorumParams, const TFsPath& srcHnswDir, const TFsPath& srcCompactVectorPath) {
    NCompactV4::TBannerVectorReader vectorReader(srcCompactVectorPath);

    auto indexData = MakeAtomicShared<TSimpleHnswIndexData>(srcHnswDir.GetPath() + ".hnsw");
    TBannerSimpleHnswReader hnswReader(1, quorumParams, indexData, vectorReader);

    TVector<float> vector {0.1, 0.2, 0.3, 0.4, 0.5};

    TMemoryPool memoryPool(1_MB);
    NEmbeddings::TModelEmbedding queryEmbedding;
    queryEmbedding.MutableCompressionParams()->SetElementType(NEmbeddings::TCompressionParams::ET_FLOAT);
    queryEmbedding.SetCompressedVector(TString {
        reinterpret_cast<const char*>(vector.begin()),
        reinterpret_cast<const char*>(vector.end())
    });

    THnswQuorumRequestParameters params;
    params.SetSearchNeighborhoodSize(10);

    TDocVector docVector(&memoryPool);
    TReaderRequestContext context;
    context.MemoryPool = &memoryPool;
    hnswReader.GetBestDocs(queryEmbedding, 10, params, context, nullptr, docVector);

    TVector<ui32> docIds;
    for (const auto& doc : docVector) {
        docIds.push_back(doc.DocId);
    }

    TDocVector docVectorBruteForce(&memoryPool);
    hnswReader.GetBestDocsBruteForce(memoryPool, queryEmbedding, 10, params, docVectorBruteForce);

    TVector<ui32> docIdsBruteForce;
    for (const auto& doc : docVectorBruteForce) {
        docIdsBruteForce.push_back(doc.DocId);
    }

    Cerr << DbgDump(docIdsBruteForce) << Endl;
    UNIT_ASSERT_VALUES_EQUAL(docIds, docIdsBruteForce);
}

Y_UNIT_TEST_SUITE(TestSimpleHnsw) {
    Y_UNIT_TEST(Simple) {
        TReallyFastRng32 rng(42);
        const size_t modelId = 2;
        const size_t profileCnt = 100;
        const size_t vectorSize = 5;
        const TFsPath workDir = GetWorkPath();
        const auto compactVectorDir = workDir / "compact_vectors" / "1";
        compactVectorDir.MkDirs();
        const auto hnswQuorumDir = workDir / "hnsw_quorum";
        hnswQuorumDir.MkDirs();

        auto profiles = GenerateData(rng, modelId, profileCnt, vectorSize);

        const auto compactVectorConfig = ReadPbJsonFromString<NCompactV4::TCompactVectorConfig>(R"___({
            "CompactVectorID": 1,
            "ModelID": 2,
            "VectorSize": 5,
            "CompressionParams": {
                "ElementType": "ET_FLOAT"
            },
            "DocumentType": "DT_BANNER"
        })___");

        const auto quorumConfig = ReadPbJsonFromString<TQuorumDesc>(R"___({
            "QuorumID": 1,
            "Hnsw": {
                "IndexName": "test_banner_float",
                "Simple": {
                },
                "CompactVectorID": 1
            }
        })___");

        TAdvMachineDaemonConfig daemonConfig;
        *daemonConfig.AddCompactVectors() = compactVectorConfig;
        *daemonConfig.AddQuorums() = quorumConfig;

        ApplyModifiers(workDir, daemonConfig, profiles);
        BuildHnsw(quorumConfig.GetHnsw(), compactVectorDir, hnswQuorumDir);

        TestHnsw(quorumConfig.GetHnsw(), hnswQuorumDir / quorumConfig.GetHnsw().GetIndexName(), compactVectorDir);
    }
}
