#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/model_builder_lib/sthash_hash_embedding_builder.h>
#include <ads/pytorch/deploy/tests/lib/dump_util.h>

#include <util/generic/hash.h>
#include <util/folder/path.h>
#include <util/system/tempfile.h>


namespace {
    auto MakeEmbeddingHolder(const TString &path) {
        NPytorchTransport::TSTHashBuilderImpl builder;
        builder.InitFromDump(path);
        return builder.MakeHashEmbedding();
    }
}

using EEmbeddingProcessMode = NPytorchTransport::EEmbeddingProcessMode;


Y_UNIT_TEST_SUITE(TSTHashEmbeddingTest) {
    Y_UNIT_TEST(TestCorrectDtypeForDumpHelper) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard<EEmbeddingProcessMode::HALF> guard_half({{0, {0.1f, 0.2f, 0.3f}}}, 3, "sum");
        NPyTorchTransportTests::EmbeddingTableDumpGuard<EEmbeddingProcessMode::FLOAT> guard_float({{0, {0.1f, 0.2f, 0.3f}}}, 3, "sum");

        auto emb_half = MakeEmbeddingHolder(guard_half.Name());
        auto emb_float = MakeEmbeddingHolder(guard_float.Name());

        UNIT_ASSERT(dynamic_cast<NPytorchTransport::TSTHashEmbedding<EEmbeddingProcessMode::FLOAT> *>(emb_float.Get()) == emb_float.Get());
        UNIT_ASSERT(dynamic_cast<NPytorchTransport::TSTHashEmbedding<EEmbeddingProcessMode::HALF> *>(emb_float.Get()) == nullptr);

        UNIT_ASSERT(dynamic_cast<NPytorchTransport::TSTHashEmbedding<EEmbeddingProcessMode::FLOAT> *>(emb_half.Get()) == nullptr);
        UNIT_ASSERT(dynamic_cast<NPytorchTransport::TSTHashEmbedding<EEmbeddingProcessMode::HALF> *>(emb_half.Get()) == emb_half.Get());
    }

    Y_UNIT_TEST(CorrectEmbeddingSizeTest) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard guard(
                {
                    {0, {0.1f, 0.2f, 0.3f}},
                    {1, {1.f, 2.f, 3.f}}
                },
                3,
                "sum"
        );

        auto hashEmbedding = MakeEmbeddingHolder(guard.Name());

        UNIT_ASSERT_EQUAL(hashEmbedding->GetEmbeddingSize(), 3ULL);
        UNIT_ASSERT_EQUAL(hashEmbedding->LookUp({0, 1, 2, 3, 4, 5}).size(), 3ULL);
    }

    Y_UNIT_TEST(UnknownKeysTest) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard guard(
                {
                        {0, {0.1f, 0.2f, 0.3f}},
                        {1, {1.f, 2.f, 3.f}}
                },
                3,
                "sum"
        );
        auto hashEmbedding = MakeEmbeddingHolder(guard.Name());

        auto embedding = hashEmbedding->LookUp({2, 3, 4, 5});
        for (auto e : embedding) {
            UNIT_ASSERT_EQUAL(e, 0.f);
        }
    }

    Y_UNIT_TEST(EmptyKeysListTest) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard guard(
                {
                        {0, {0.1f, 0.2f, 0.3f}},
                        {1, {1.f, 2.f, 3.f}}
                },
                3,
                "sum"
        );

        auto hashEmbedding = MakeEmbeddingHolder(guard.Name());
        auto embedding = hashEmbedding->LookUp({});
        for (auto e : embedding) {
            UNIT_ASSERT_EQUAL(e, 0.f);
        }
    }

    // FIXME: currently this is testing legacy format without compute mode
    // we check that code works as expected on old dumps
    // when we release new compute mode code everywhere, this should be removed
    Y_UNIT_TEST(CorrectEmbeddingTest) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard guard(
                {
                        {0, {0.1f, 0.2f, 0.3f}},
                        {1, {1.f, 2.f, 3.f}}
                },
                3,
                "sum"
        );

        auto hashEmbedding = MakeEmbeddingHolder(guard.Name());
        auto embedding = hashEmbedding->LookUp({0, 1});
        for (ui64 i = 0; i < embedding.size(); ++i) {
            UNIT_ASSERT_EQUAL(embedding[i], (float)((i + 1) * 1.1));
        }
    }

    template <EEmbeddingProcessMode Mode>
    void CheckCorrectEmbeddingWithComputeMode(const TString &computeMode) {
        NPyTorchTransportTests::EmbeddingTableDumpGuard<Mode> guard(
                {
                        {0, {1.f, 2.f, 3.f}},
                        {1, {3.f, 4.f, 5.f}}
                },
                3,
                computeMode
        );

        auto hashEmbedding = MakeEmbeddingHolder(guard.Name());
        auto embedding = hashEmbedding->LookUp({0, 1});

        TVector<float> reference;
        if (computeMode == "mean") {
            reference = {2., 3., 4.};
        } else if (computeMode == "sum") {
            reference = {4., 6., 8.};
        } else {
            ythrow yexception() << "Unknown compute mode " << computeMode;
        }

        for (ui64 i = 0; i < embedding.size(); ++i) {
            UNIT_ASSERT_EQUAL(embedding[i], reference[i]);
        }
    }

    Y_UNIT_TEST(CorrectEmbeddingTestWithComputeModeSumFloat) {
        CheckCorrectEmbeddingWithComputeMode<EEmbeddingProcessMode::FLOAT>("sum");
    }

    Y_UNIT_TEST(CorrectEmbeddingTestWithComputeModeSumHalf) {
        CheckCorrectEmbeddingWithComputeMode<EEmbeddingProcessMode::HALF>("sum");
    }

    Y_UNIT_TEST(CorrectEmbeddingTestWithComputeModeMeanFloat) {
        CheckCorrectEmbeddingWithComputeMode<EEmbeddingProcessMode::FLOAT>("mean");
    }

    Y_UNIT_TEST(CorrectEmbeddingTestWithComputeModeMeanHalf) {
        CheckCorrectEmbeddingWithComputeMode<EEmbeddingProcessMode::HALF>("mean");
    }

    // FIXME empty dump test????.... will remove it soon
    Y_UNIT_TEST(ModelDumpTest) {
        auto hashEmbedding = MakeEmbeddingHolder(".");
        auto embedding = hashEmbedding->LookUp({0, 200015136, 200001955, 200051593, 200002209, 200001511, 12345, 67890});
        UNIT_ASSERT_EQUAL(hashEmbedding->GetEmbeddingSize(), 90ULL);
        UNIT_ASSERT_EQUAL(embedding.size(), 90ULL);
    }

    template <EEmbeddingProcessMode Mode>
    void CheckSaveLoad(const TString &computeMode) {
        // If we've fucked up wtih dtype save/load, storing these values in half and compare vs float
        // will slightly harm precision and make embedding sum assert fail
        NPyTorchTransportTests::EmbeddingTableDumpGuard<Mode> guard(
                {
                        {0, {1.f, 2.f, 3.f}},
                        {1, {3.f, 4.f, 5.f}}
                },
                3,
                computeMode
        );

        NPytorchTransport::TSTHashBuilderImpl builder;
        builder.InitFromDump(guard.Name());

        auto hashEmbedding = builder.MakeHashEmbedding();

        TTempFile saveTmpFile(MakeTempName());
        TFileOutput outputStream(saveTmpFile.Name());
        builder.Save(outputStream);
        outputStream.Finish();

        TFileInput inputStream(saveTmpFile.Name());

        NPytorchTransport::TSTHashBuilderImpl loadedBuilder;
        loadedBuilder.Load(inputStream);

        auto loadedHashEmbedding = loadedBuilder.MakeHashEmbedding();

        UNIT_ASSERT_EQUAL(loadedHashEmbedding->GetEmbeddingSize(), 3ULL);
        UNIT_ASSERT_EQUAL(loadedHashEmbedding->GetEmbeddingSize(), hashEmbedding->GetEmbeddingSize());

        auto firstVec = loadedHashEmbedding->LookUp({0, 1, 2});
        auto secondVec = hashEmbedding->LookUp({0, 1, 2});
        UNIT_ASSERT_EQUAL(firstVec.size(), 3ULL);
        UNIT_ASSERT_EQUAL(firstVec.size(), secondVec.size());
        for (ui64 i = 0; i < firstVec.size(); ++i) {
            UNIT_ASSERT_EQUAL(firstVec[i], secondVec[i]);
        }
    }

    Y_UNIT_TEST(SaveAndLoadTestSumFloat) {
        CheckSaveLoad<EEmbeddingProcessMode::FLOAT>("sum");
    }

    Y_UNIT_TEST(SaveAndLoadTestSumHalf) {
        CheckSaveLoad<EEmbeddingProcessMode::HALF>("sum");
    }

    Y_UNIT_TEST(SaveAndLoadTestMeanFloat) {
        CheckSaveLoad<EEmbeddingProcessMode::FLOAT>("mean");
    }

    Y_UNIT_TEST(SaveAndLoadTestMeanHalf) {
        CheckSaveLoad<EEmbeddingProcessMode::HALF>("mean");
    }
};
