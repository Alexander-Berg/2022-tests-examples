#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <ads/tensor_transport/lib_2/st_hash/st_hash.h>
#include <ads/tensor_transport/lib_2/st_hash/difacto_model.h>

#include <library/cpp/testing/unittest/registar.h>


namespace NTsarTransport {

    class TModelSTTableTest : public TTestBase {
    public:
        void ModelTablesEqualSizeTest() {
            UNIT_ASSERT_VALUES_EQUAL(ModelSTTable->size(), ModelTable.size());
        }

        void ModelTablesEqualTest() {
            for (auto it = ModelSTTable->begin(); it != ModelSTTable->end(); ++it) {
                auto modelVal = ModelTable[it.Key()];
                auto stModelVal = it.Value();

                UNIT_ASSERT_VALUES_EQUAL(modelVal.size(), stModelVal.Size);

                for (size_t i = 0; i < modelVal.size(); ++i) {
                    UNIT_ASSERT_DOUBLES_EQUAL(modelVal[i], stModelVal[i], 1.e-7);
                }
            }
        }

        void CreateModelTest() {
            auto model = TSTModel(STModelFileName);
            UNIT_ASSERT_VALUES_EQUAL(model.GetVectorSize(), 50);
        }

        void SetUp() override {
            ModelFileName = "./example_dump";
            STModelFileName = "./st_example_dump";

            TDifactoHashTableReader reader(ModelFileName);
            reader.ReadTable(ModelTable);

            DumpSTModel(ModelTable, STModelFileName);

            STModelFileMap = MakeHolder<TFileMap>(STModelFileName);
            LoadModel(ModelSTTable, *STModelFileMap);
        }

    private:
        TString ModelFileName;
        TString STModelFileName;
        THolder<TFileMap> STModelFileMap;


        THashVecTable ModelTable;
        const TSTHashVecTable *ModelSTTable;

        UNIT_TEST_SUITE(TModelSTTableTest);
        UNIT_TEST(ModelTablesEqualSizeTest);
        UNIT_TEST(ModelTablesEqualTest);
        UNIT_TEST(CreateModelTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TModelSTTableTest);

}
