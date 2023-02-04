#include "sis/identity_layer.h"
#include "sis/reshape_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace
{
  using namespace NSis;

  struct ReshapeLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims(8, 64, 8, 64);
      //Dims.SetRaggedDimension(3);
      Dims.SetInnerDims(-2);

      Reshape = TDims(Dims.OuterSize(), Dims.InnerSize());
      Reshape.SetInnerDims(-1);
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto reshape = Network->AddReshape(input, Reshape);
      auto identity = Network->AddIdentity(reshape->GetOutput(0));
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

    void PrepareRaggedData()
    {
      int itemsCount = Dims.Dims[2];
      Offsets = {0};
      for (int i = 0; i != itemsCount; ++i)
        Offsets.push_back(Offsets.back() + (i + 1) % Dims.Dims[3]);

      Input.resize(Dims.OuterSize() * Offsets.back());
      std::iota(Input.begin(), Input.end(), 0);
      if (GetValueType() != NSis::eFloat)
      {
        for (auto &v: Input)
          v = sinf(v);
      }
      Output.resize(Input.size());
    }

    void SetRaggedInputs(IInferenceSessionPtr session)
    {
      session->SetRaggedInput(0, Offsets.size(), Offsets.data(), Input.size(), CastInput(Input));
      session->GetOutput(0, Output.size(), Output.data());
    }

    void SetInputs(IInferencePtr, IInferenceSessionPtr session) override
    {
      session->SetInput(0, Input.size(), CastInput(Input));
      session->GetOutput(0, Output.size(), Output.data());
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      SetInputs(Inference, session);
      session->Run();
      session->Finalize();
      CastOutput(Output);
    }

    TDims Dims;
    TDims Reshape;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<int> Offsets;
  };
}

TEST_P(ReshapeLayerTest, StepExecution)
{
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

INSTANTIATE_TEST_SUITE_P(ReshapeLayerTest, ReshapeLayerTest,
        ::testing::ValuesIn(
        NTest::Combine(
                NTest::TTestSetup::Engines({"sis"}),
                NTest::TTestSetup::ValueTypes({"float"}),
                NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
        )
),
        NTest::TTestSetup::Description()
);

