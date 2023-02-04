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

  struct InnerRaggedTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());

      Dims = TDims(8, 64, 8, 128);
      Dims.SetRaggedDimension(3);
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
      int itemsCount = Dims.Dims[2];
      Offsets = {0};
      for (int i = 0; i != itemsCount; ++i)
        Offsets.push_back(Offsets.back() + (i + 1) % Dims.Dims[3]);

      Input.resize(Dims.Dims[0] * Dims.Dims[1] * Offsets.back());
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

    void PerformInference()
    {
      auto session = Inference->StartSession();
      SetRaggedInputs(session);
      session->Run();
      session->Finalize();
      CastOutput(Output);
    }

    TDims Dims;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<int> Offsets;
  };
}

TEST_P(InnerRaggedTest, SingleInner)
{
  Dims.SetInnerDims(-1);

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

TEST_P(InnerRaggedTest, MultipleInner)
{
  Dims.SetInnerDims(-2);

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

INSTANTIATE_TEST_SUITE_P(InnerRaggedTest, InnerRaggedTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
