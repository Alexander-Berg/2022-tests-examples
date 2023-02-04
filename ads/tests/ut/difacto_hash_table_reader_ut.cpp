#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <library/cpp/testing/unittest/registar.h>

namespace NTsarTransport {

    class TDifactoHashTableReaderTest : public TTestBase {
    public:
        void ModelSizeTest() {
            UNIT_ASSERT_VALUES_EQUAL(ModelTable.size(), 251);
        }

        void VectorSizeTest() {
            auto firstElement = ModelTable.begin();
            auto firstVector = firstElement->second;
            UNIT_ASSERT_VALUES_EQUAL(firstVector.size(), 50);
        }

        void VectorValueTest() {
            auto vector = ModelTable[8371294511277309424];
            UNIT_ASSERT_DOUBLES_EQUAL(vector[0], 0.00363877, 1.e-7);
            UNIT_ASSERT_DOUBLES_EQUAL(vector[vector.size() - 1], -0.008466359228, 1.e-7);
        }

        void SetUp() override {
            TString fileName("./example_dump");
            TDifactoHashTableReader reader(fileName);
            reader.ReadTable(ModelTable);
        }

    private:
        THashMap<ui64, TVector<float>> ModelTable;
        UNIT_TEST_SUITE(TDifactoHashTableReaderTest);
        UNIT_TEST(ModelSizeTest);
        UNIT_TEST(VectorSizeTest);
        UNIT_TEST(VectorValueTest);
        UNIT_TEST_SUITE_END();
    };

    class TDifactoHashTableStreamReaderTest : public TTestBase {
    public:
        void ModelSizeTest() {
            UNIT_ASSERT_VALUES_EQUAL(ModelTable.size(), 251);
        }

        void VectorSizeTest() {
            auto firstElement = ModelTable.begin();
            auto firstVector = firstElement->second;
            UNIT_ASSERT_VALUES_EQUAL(firstVector.size(), 50);
        }

        void VectorValueTest() {
            auto vector = ModelTable[8371294511277309424];
            UNIT_ASSERT_DOUBLES_EQUAL(vector[0], 0.00363877, 1.e-7);
            UNIT_ASSERT_DOUBLES_EQUAL(vector[vector.size() - 1], -0.008466359228, 1.e-7);
        }

        void VectorValueReadWithSkipZeroVectorTestTest() {
            TString fileName("./example_dump");
            THolder<IInputStream> stream = MakeHolder<TMappedFileInput>(TFile(fileName, RdOnly));
            TDifactoHashTableReader reader(std::move(stream), false);
            reader.ReadTable(ModelTable);
            auto vector = ModelTable[8371294511277309424];
            UNIT_ASSERT_DOUBLES_EQUAL(vector[0], 0.00363877, 1.e-7);
            UNIT_ASSERT_DOUBLES_EQUAL(vector[vector.size() - 1], -0.008466359228, 1.e-7);
        }

        void SetUp() override {
            TString fileName("./example_dump");
            THolder<IInputStream> stream = MakeHolder<TMappedFileInput>(TFile(fileName, RdOnly));
            TDifactoHashTableReader reader(std::move(stream));
            reader.ReadTable(ModelTable);
        }

    private:
        THashMap<ui64, TVector<float>> ModelTable;
        UNIT_TEST_SUITE(TDifactoHashTableStreamReaderTest);
        UNIT_TEST(ModelSizeTest);
        UNIT_TEST(VectorSizeTest);
        UNIT_TEST(VectorValueTest);
        UNIT_TEST(VectorValueReadWithSkipZeroVectorTestTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TDifactoHashTableReaderTest);
    UNIT_TEST_SUITE_REGISTRATION(TDifactoHashTableStreamReaderTest);
}
