#include <ads/pytorch/deploy/lib/sthash_embedding.h>
#include <ads/pytorch/deploy/model_builder_lib/sthash_hash_embedding_builder.h>

#include <thread>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TSTHashEmbeddingMultithreadingTest) {
    Y_UNIT_TEST(MultithreadTest) {
        NPytorchTransport::TSTHashBuilderImpl builder;
        builder.InitFromDump(".");
        auto hashEmbedding = builder.MakeHashEmbedding();
        TVector<ui64> ids({0, 200015136, 200001955, 200051593, 200002209, 200001511, 12345, 67890});

        UNIT_ASSERT_EQUAL(hashEmbedding->GetEmbeddingSize(), 90ULL);

        auto calculateModel = [&hashEmbedding, &ids]() {
            auto emb1 = hashEmbedding->LookUp(ids);
            auto emb2 = hashEmbedding->LookUp(ids);
            auto emb3 = hashEmbedding->LookUp(ids);
            UNIT_ASSERT_EQUAL(emb1.size(), 90ULL);
            UNIT_ASSERT_EQUAL(emb2.size(), 90ULL);
            UNIT_ASSERT_EQUAL(emb3.size(), 90ULL);
            for (ui64 i = 0; i < 90; ++i) {
                UNIT_ASSERT_EQUAL(emb1[i], emb2[i]);
                UNIT_ASSERT_EQUAL(emb2[i], emb3[i]);
            }
        };

        TVector<std::thread> threads;
        for (ui64 i = 0; i < 20; ++i) {
            threads.emplace_back(std::thread(calculateModel));
        }

        for (auto& thr : threads) {
            thr.join();
        }
    }
};
