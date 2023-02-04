#include "compiler_test_setup.h"
#include "sis/inference.h"
#include <gtest/gtest.h>
#include <numeric>
#include "sis/network_definition.h"
#include "sis/transpose_layer.h"

#include <cmath>
#include <numeric>
#include "sis/sis_assert.h"

namespace
{
  using namespace NSis;
  struct TransposeTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      SetDumpKernelSource(true);

      FillData();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      auto indices = Network->AddInput("input", GetValueType(), InputDims);
      auto transposed = Network->AddTranspose(indices, 1, 2)->GetOutput(0);
      Network->MarkOutput(transposed);
      return Network;
    }

    void FillData() {
      InputDims = TDims(2, 3, 2, InnerDim);

      for (int i = 0; i < InputDims.TotalSize(); ++i)
        Input.push_back(i);

      Output.resize(2 * 3 * 2 * InnerDim);
      Expected.resize(2 * 3 * 2 * InnerDim);
      for (int a = 0; a < 2; ++a)
        for (int b = 0; b < 3; ++b)
          for (int c = 0; c < 2; ++c)
            for (int d = 0; d < InnerDim; ++d)
              Expected[a * 3 * 2 * InnerDim + c * 3 * InnerDim + b * InnerDim + d] = 
                 Input[a * 3 * 2 * InnerDim + b * 2 * InnerDim + c * InnerDim + d];
    }

    TDims InputDims;
    int InnerDim = 128;

    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Expected;
  };
}

TEST_P(TransposeTest, Pass)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  session->SetInput(0, Input.size(), CastInput(Input));

  session->Run();
  session->GetOutput(0, inference->GetOutput(0)->GetDims().TotalSize(), Output.data());
  session->Finalize();

  CastOutput(Output);
  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Expected.size(); ++i) {
    ASSERT_NEAR(Expected[i], Output[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(TransposeTest, TransposeTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
