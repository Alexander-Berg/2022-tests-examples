#include <library/cpp/testing/unittest/registar.h>

#include <ads/pytorch/deploy/lib/util.h>
#include <ads/pytorch/deploy/math_lib/vector_math.h>

#include <library/cpp/iterator/zip.h>
#include <util/generic/hash.h>
#include <util/system/tempfile.h>


using namespace NPytorchTransport;

Y_UNIT_TEST_SUITE(TUtilFunctionsTest){
    namespace {
        template <EEmbeddingProcessMode Mode>
        void LoadHashEmbeddingsHelper(THashMap<ui64, TVector<float>> &hashTable) {
            TTempFileHandle tmpFile;
            tmpFile.Close();

            using namespace NPytorchTransport;

            SerializeEmbeddings(hashTable, tmpFile.Name());
            TLoadedHashMap<Mode> loadedTable = LoadSerializedEmbeddings<Mode>(tmpFile.Name());
            UNIT_ASSERT(loadedTable.count(0));
            UNIT_ASSERT(loadedTable.count(1));

            // key0 compare
            auto cmp_fn = [&](const ui64 &key) {
                using namespace NPytorchTransport::NMath;

                const auto &loadedVector = loadedTable.at(key);
                const auto &reference = hashTable.at(key);

                if constexpr(Mode == EEmbeddingProcessMode::FLOAT) {
                    UNIT_ASSERT_EQUAL(loadedVector.size(), reference.size());
                    for (const auto &[trueValue, actualValue] : Zip(reference, loadedVector)) {
                        UNIT_ASSERT_EQUAL(trueValue, actualValue);
                    }
                } else if constexpr(Mode == EEmbeddingProcessMode::HALF) {
                    UNIT_ASSERT_EQUAL(loadedVector.size(), reference.size());
                    TVector<float> loadedCasted(loadedVector.size());
                    MemcpyFloatsWithHalfCast(loadedCasted.data(), loadedVector.data(), loadedCasted.size());
                    for (const auto &[trueValue, actualValue] : Zip(reference, loadedCasted)) {
                        UNIT_ASSERT_EQUAL(trueValue, actualValue);
                    }
                } else if constexpr(Mode == EEmbeddingProcessMode::UINT8_LENGTH) {
                    TSTHashData<Mode> dataStruct{
                        .Size = loadedVector.Data.size(),
                        .Coordinates = loadedVector.Data.data(),
                        .Length = loadedVector.Length
                    };
                    float precision = 1.f / 128.f * dataStruct.Length;
                    for (ui64 i = 0; i < reference.size(); i++) {
                        UNIT_ASSERT_DOUBLES_EQUAL(dataStruct[i], reference[i], precision);
                    }
                } else {
                    ythrow yexception() << "unknown mode";
                }
            };

            cmp_fn(0);
            cmp_fn(1);
        }
    }

    Y_UNIT_TEST(LoadHashEmbeddingsTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(0, TVector<float>({0.1f, 0.2f}));
        hashTable.emplace(1, TVector<float>({1.f, 2.f, 3.f}));

        LoadHashEmbeddingsHelper<EEmbeddingProcessMode::FLOAT>(hashTable);
    }

    Y_UNIT_TEST(LoadHashEmbeddingsHalfTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(0, TVector<float>({1.f, 2.f}));
        hashTable.emplace(1, TVector<float>({3.f, 4.f, 5.f}));

        LoadHashEmbeddingsHelper<EEmbeddingProcessMode::HALF>(hashTable);
    }

    Y_UNIT_TEST(LoadHashEmbeddingsUint8LengthTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(0, TVector<float>({1.f, 2.f, -3.f, 0.5f}));
        hashTable.emplace(1, TVector<float>({0.f, 0.f, 0.f, 5.f, -3.4f}));

        LoadHashEmbeddingsHelper<EEmbeddingProcessMode::UINT8_LENGTH>(hashTable);
    }

    namespace {
        template <EEmbeddingProcessMode Mode>
        void DumpAsSTHashHelper(THashMap<ui64, TVector<float>> &hashTable) {
            TTempFileHandle tmpFile;
            tmpFile.Close();

            using namespace NPytorchTransport;

            SerializeEmbeddings(hashTable, tmpFile.Name());
            TLoadedHashMap<Mode> loadedTable = LoadSerializedEmbeddings<Mode>(tmpFile.Name());

            TTempFileHandle tmpFileSthash;
            DumpToSTHash(loadedTable, tmpFileSthash.Name());
            tmpFileSthash.Close();
            TBlob data = TBlob::PrechargedFromFile(tmpFileSthash.Name());
            auto stHashTable = reinterpret_cast<const sthash<ui64, TSTHashData<Mode>, ::hash<ui64>>*>(data.Data());

            // key0 compare
            auto cmp_fn = [&](const ui64 &key) {
                using namespace NPytorchTransport::NMath;

                const TSTHashData<Mode> &loadedVector = stHashTable->find(key).Value();
                const TVector<float> &reference = hashTable.at(key);

                if constexpr(Mode == EEmbeddingProcessMode::FLOAT) {
                    UNIT_ASSERT_EQUAL(loadedVector.Size, reference.size());
                    for (ui64 i = 0; i < loadedVector.Size; i++) {
                        UNIT_ASSERT_EQUAL(loadedVector[i], reference[i]);
                    }
                } else if constexpr(Mode == EEmbeddingProcessMode::HALF) {
                    UNIT_ASSERT_EQUAL(loadedVector.Size, reference.size());
                    TVector<float> loadedCasted(loadedVector.Size);
                    MemcpyFloatsWithHalfCast(loadedCasted.data(), &loadedVector[0], loadedCasted.size());
                    for (ui64 i = 0; i < loadedCasted.size(); i++) {
                        UNIT_ASSERT_EQUAL(loadedCasted[i], reference[i]);
                    }
                } else if constexpr(Mode == EEmbeddingProcessMode::UINT8_LENGTH) {
                    float precision = 1.f / 128.f * loadedVector.Length;
                    for (ui64 i = 0; i < reference.size(); i++) {
                        UNIT_ASSERT_DOUBLES_EQUAL(loadedVector[i], reference[i], precision);
                    }
                } else {
                    ythrow yexception() << "unknown mode";
                }
            };

            cmp_fn(0);
            cmp_fn(1);
        }
    }

    Y_UNIT_TEST(DumpAsSTHashFloatTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(std::make_pair<ui64, TVector<float>>(0, {0.1f, 0.2f}));
        hashTable.emplace(std::make_pair<ui64, TVector<float>>(1, {1.f, 2.f, 3.f}));

        DumpAsSTHashHelper<EEmbeddingProcessMode::FLOAT>(hashTable);
    }

    Y_UNIT_TEST(DumpAsSTHashHalfTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(0, TVector<float>({1.f, 2.f}));
        hashTable.emplace(1, TVector<float>({3.f, 4.f, 5.f}));

        DumpAsSTHashHelper<EEmbeddingProcessMode::HALF>(hashTable);
    }

    Y_UNIT_TEST(DumpAsSTHashUint8LengthTest) {
        THashMap<ui64, TVector<float>> hashTable;
        hashTable.emplace(std::make_pair<ui64, TVector<float>>(0, {0.1f, 0.2f}));
        hashTable.emplace(std::make_pair<ui64, TVector<float>>(1, {1.f, 2.f, 3.f}));

        DumpAsSTHashHelper<EEmbeddingProcessMode::UINT8_LENGTH>(hashTable);
    }
};
