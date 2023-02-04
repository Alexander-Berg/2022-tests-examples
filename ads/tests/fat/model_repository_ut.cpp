#include <ads/tensor_transport/yt_lib/model_repository.h>
#include <ads/tensor_transport/lib_2/difacto_hash_table_reader.h>
#include <mapreduce/yt/interface/client.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/system/env.h>
#include <util/random/random.h>


namespace NTsarTransport {
    class TModelRepositoryTest : public TTestBase {
    public:
        void SetUp() override {
            Folder = "//tmp/model_folder";
            ModelFileName = "model_2019010521";
            TString ytProxy = GetEnv("YT_PROXY");
            Client = NYT::CreateClient(ytProxy);
            Client->Create(Folder, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true));
        }

        void TearDown() override {
            Client->Remove(Folder, NYT::TRemoveOptions().Recursive(true));
        }

        void NoModelTest() {
            TYtModelRepository modelRepository(Folder, Client);
            auto modelStream = modelRepository.GetLatestModel();
            UNIT_ASSERT_EQUAL(modelStream.Get(), nullptr);
        }

        void SingleModelTest() {
            TString fullPath = Folder + "/" + ModelFileName;
            THashMap<ui64, TVector<float>> inputMap;
            inputMap[0] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, fullPath);
            TYtModelRepository modelRepository(Folder, Client);
            auto modelStream = modelRepository.GetLatestModel();
            UNIT_ASSERT_UNEQUAL(modelStream.Get(), nullptr);
        }


        void TwoModelsReturnLatestTest() {
            TString firstModelPath = Folder + "/" + ModelFileName;
            THashMap<ui64, TVector<float>> inputMap;
            inputMap[0] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, firstModelPath);
            TString newerModelFileName = "model_2019010522";
            TString secondModelPath = Folder + "/" + newerModelFileName;
            inputMap[2] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, secondModelPath);

            TYtModelRepository modelRepository(Folder, Client);
            THolder<IInputStream> stream(modelRepository.GetLatestModel().Release());
            TDifactoHashTableReader reader(std::move(stream));
            THashMap<ui64, TVector<float>> outputMap;
            reader.ReadTable(outputMap);
            UNIT_ASSERT_EQUAL(outputMap.size(), 2);
        }

        void SpecificModelByLastLogDateTest() {
            TString firstModelPath = Folder + "/" + ModelFileName;
            THashMap<ui64, TVector<float>> inputMap;
            inputMap[0] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, firstModelPath);
            TString newerModelFileName = "model_2019010522";
            TString secondModelPath = Folder + "/" + newerModelFileName;
            inputMap[2] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, secondModelPath);

            TYtModelRepository modelRepository(Folder, Client);
            TString lastLogDate = "2019010522";
            THolder<IInputStream> stream(modelRepository.GetSpecificModel(lastLogDate).Release());
            TDifactoHashTableReader reader(std::move(stream));
            THashMap<ui64, TVector<float>> outputMap;
            reader.ReadTable(outputMap);
            UNIT_ASSERT_EQUAL(outputMap.size(), 2);
        }

        void EmptyModelLastLogDateTest() {
            TString firstModelPath = Folder + "/" + ModelFileName;
            THashMap<ui64, TVector<float>> inputMap;
            inputMap[0] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, firstModelPath);
            TString newerModelFileName = "model_2019010522";
            TString secondModelPath = Folder + "/" + newerModelFileName;
            inputMap[2] = {4.0, 5.0, 6.0};
            WriteModel(inputMap, secondModelPath);

            TYtModelRepository modelRepository(Folder, Client);
            TString lastLogDate = "2019010524";
            UNIT_ASSERT_EQUAL(modelRepository.GetSpecificModel(lastLogDate), nullptr);
        }

    private:
        UNIT_TEST_SUITE(TModelRepositoryTest);
        UNIT_TEST(NoModelTest);
        UNIT_TEST(SingleModelTest);
        UNIT_TEST(TwoModelsReturnLatestTest);
        UNIT_TEST(SpecificModelByLastLogDateTest);
        UNIT_TEST(EmptyModelLastLogDateTest);
        UNIT_TEST_SUITE_END();
        TString Folder;
        TString ModelFileName;
        NYT::IClientPtr Client;

        void WriteModel(const THashMap<ui64, TVector<float>>& modelMap, const TString& fullPath) {
            Client->Create(fullPath, NYT::ENodeType::NT_MAP);
            auto writer = Client->CreateFileWriter(fullPath + "/concatenated_dump");

            for (auto& [hash, features]: modelMap) {

                writer->Write((char *) &hash, sizeof(hash));
                int size = features.size();
                writer->Write((char *) &size, sizeof(hash));
                for (auto value: features) {
                    writer->Write((char *) &value, sizeof(value));
                }

            }
            writer->Finish();
        }
    };
    UNIT_TEST_SUITE_REGISTRATION(TModelRepositoryTest);


    class TModelRepositoryWithReadingTest : public TTestBase {
    public:
        void SetUp() override {
            Folder = "//tmp/model_folder";
            ModelFileName = "model_2019010621";
            TString ytProxy = GetEnv("YT_PROXY");
            Client = NYT::CreateClient(ytProxy);
            Client->Create(Folder, NYT::ENodeType::NT_MAP, NYT::TCreateOptions().Recursive(true));
        }

        void TearDown() override {
            Client->Remove(Folder, NYT::TRemoveOptions().Recursive(true));
        }

        void BigModelSizeTest() {
            TString fullPath = Folder + "/" + ModelFileName;
            THashMap<ui64, TVector<float>> inputMap;
            ui64 modelSize = 1222;
            for (ui64 i=0; i < modelSize; ++i ) {
                inputMap[RandomNumber<ui64>()] = {4.0, 5.0, 6.0};
            }
            WriteModel(inputMap, fullPath);
            TYtModelRepository modelRepository(Folder, Client);
            auto modelStream = modelRepository.GetLatestModel();

            THolder<IInputStream> stream(modelStream.Release());
            THolder<TDifactoHashTableReader> modelReader = MakeHolder<TDifactoHashTableReader>(std::move(stream));
            auto table = new THashMap<ui64, TVector<float>>();
            modelReader->ReadTable(*table);
            THashMap<ui64, TVector<float>>::size_type expectedValue = table->size();

            TString message = "Expected table size: " + ToString(modelSize) + " Actual value: " + ToString(expectedValue);
            UNIT_ASSERT_EQUAL_C(table->size(), modelSize, message);
        }

    private:
        UNIT_TEST_SUITE(TModelRepositoryWithReadingTest);
        UNIT_TEST(BigModelSizeTest);
        UNIT_TEST_SUITE_END();
        TString Folder;
        TString ModelFileName;
        NYT::IClientPtr Client;

        void WriteModel(const THashMap<ui64, TVector<float>>& modelMap, const TString& fullPath) {
            Client->Create(fullPath, NYT::ENodeType::NT_MAP);
            auto writer = Client->CreateFileWriter(fullPath + "/concatenated_dump");

            for (auto& [hash, features]: modelMap) {

                writer->Write((char *) &hash, sizeof(hash));
                unsigned size = features.size();
                writer->Write((char *) &size, sizeof(hash));
                for (auto value: features) {
                    writer->Write((char *) &value, sizeof(value));
                }

            }
            writer->Finish();
        }
    };
    UNIT_TEST_SUITE_REGISTRATION(TModelRepositoryWithReadingTest);
}
