#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <ads/tensor_transport/user_embedder/user_embedder.h>

#include <library/cpp/testing/unittest/registar.h>


namespace NTsarTransport {
    class TUserEmbedderTest : public TTestBase {
    public:
        void ModelCallOnEmptyProfileTest() {
            NBSYeti::TBigbPublicProto profile;
            auto vec = userEmbedder.Call(profile);

            UNIT_ASSERT_DOUBLES_EQUAL(vec[0], 1, 1e-7);

            for (size_t i = 1; i < vec.size(); ++i) {
                UNIT_ASSERT_DOUBLES_EQUAL(vec[i], 0, 1e-7);
            }
        }

        void SetUp() override {
            TDifactoHashTableReader reader("./example_dump");
            reader.ReadTable(ModelTable);
            userEmbedder = TUserEmbedder<TModel>(std::move(ModelTable));
        }

    private:
        TUserEmbedder<TModel> userEmbedder;
        THashMap<ui64, TVector<float>> ModelTable;
        UNIT_TEST_SUITE(TUserEmbedderTest);
        UNIT_TEST(ModelCallOnEmptyProfileTest);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TUserEmbedderTest);
}
