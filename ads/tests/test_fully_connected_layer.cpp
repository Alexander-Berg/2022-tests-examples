#include "sis/fully_connected_layer.h"
#include "mma_test_data.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <cmath>

namespace
{
  using namespace NSis;

  struct FullyConnectedLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NSis::NTest::TMmaTestData, NTest::TBaseTestPipeline
  {
  protected:
    FullyConnectedLayerTest(): NSis::NTest::TMmaTestData(32)
    {
    }

    void SetUp() override
    {
      InitSetup(GetParam());
    }

    void BuildNetwork()
    {
      auto A = Network->AddInput("A", GetValueType(), TDims2(Shape.M, Shape.K));
      WeightsB = {GetValueType(), CastInput(B), (int64_t)B.size()};

      auto layer = Network->AddFullyConnected(A, WeightsB);
      auto D = layer->GetOutput(0);
      Network->MarkOutput(D);
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, A.size(), CastInput(A));
      session->Run();
      session->GetOutput(0, D.size(), D.data());
      session->Finalize();
      CastOutput(D);
    }

    TWeights WeightsB;
  };
}

TEST_P(FullyConnectedLayerTest, Correct)
{
  Prepare();
  BuildNetwork();
  CompileTestNetwork();
  PerformInference();

  if (false)
    Print(ShapeD, D.data());

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != TestData.size(); ++i)
    if (TestData[i] != D[i])
    {
      ASSERT_NEAR(TestData[i], D[i], precision) << i;
    }
}

INSTANTIATE_TEST_SUITE_P(FullyConnectedLayerTest, FullyConnectedLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Gemms({"cublas_lt"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
