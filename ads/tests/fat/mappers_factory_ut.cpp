#include <library/cpp/testing/unittest/registar.h>
#include <ads/tensor_transport/proto/model.pb.h>
#include <ads/tensor_transport/yt_lib/mappers_factory.h>

using namespace NTsarTransport;

Y_UNIT_TEST_SUITE(TMappersFactoryTest) {

    Y_UNIT_TEST(TSingleModelTest) {
        THashSet<ui64> goodExpGroups;
        bool filterNonActiveBanners = true;
        TMappersFactory factory(std::move(goodExpGroups), filterNonActiveBanners);
        TVector<TensorTransport::TModel> models;
        UNIT_ASSERT_EQUAL(factory.BuildMappers(models).size(), 0);

    }

    Y_UNIT_TEST(TSingleDSSMModel) {
        THashSet<ui64> goodExpGroups;
        bool filterNonActiveBanners = true;
        TMappersFactory factory(std::move(goodExpGroups), filterNonActiveBanners);
        TVector<TensorTransport::TModel> models;
        TensorTransport::TModel dssmModel;
        dssmModel.SetModelType(TensorTransport::EModelType::DSSM);
        dssmModel.SetModelPath("./rsya_ctr50.appl");
        dssmModel.SetProjectorPath("./identity50.proj");
        models.emplace_back(dssmModel);
        UNIT_ASSERT_EQUAL(factory.BuildMappers(models).size(), 1);
    }
}
