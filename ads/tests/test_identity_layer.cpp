#include "sis/identity_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace
{
  using namespace NSis;

  struct IdentityLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims3(101, 11, 128);
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto identity = Network->AddIdentity(input);
      auto output = identity->GetOutput(0);
      Network->MarkOutput(output);
      EXPECT_EQ(GetValueType(), output->GetType());
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      std::iota(Input.begin(), Input.end(), 0);
      if (GetValueType() != NSis::eFloat)
      {
        for (auto &v: Input)
          v = sinf(v);
      }
      Output.resize(Input.size());
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Input.size(), CastInput(Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    TDims Dims;
    std::vector<float> Input;
    std::vector<float> Output;
  };
}

TEST_P(IdentityLayerTest, StepExecution)
{
  // @todo Added data conversion tests.

  PrepareData();
  BuildNetwork();
  CompileTestNetwork();
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Output.size(); ++i)
  {
    if (Input[i] != Output[i])
      ASSERT_NEAR(Input[i], Output[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(IdentityLayerTest, IdentityLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
