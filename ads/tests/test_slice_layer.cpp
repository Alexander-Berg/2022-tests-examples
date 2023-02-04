#include "compiler_test_setup.h"
#include "sis/inference.h"
#include <gtest/gtest.h>
#include <numeric>
#include "sis/slice_layer.h"
#include "sis/network_definition.h"

#include <cmath>
#include <numeric>
#include "sis/sis_assert.h"

namespace
{
  using namespace NSis;
  struct SliceTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      SetDumpKernelSource(true);

      BatchSize = 3;
      InnerDim = 128;

      InputDims = TDims(BatchSize, 5, InnerDim);
      InputDims.SetRaggedDimension(1);

      FillData();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", GetValueType(), InputDims);

      TDims start(0, 0, 0);
      TDims size = InputDims;
      size.Dims[1] = SliceFromBegin;
      auto slice = Network->AddSlice(input, start, size);
      Network->MarkOutput(slice->GetOutput(0));

      return Network;
    }

    void FillData() {
      InputRags = {0, 3, 4, 9};
      for (int i = 0; i < 9 * InnerDim; ++i)
        Input.push_back(i);

      Output.resize(BatchSize * SliceFromBegin * InnerDim, 0);
      Expected.resize(BatchSize * SliceFromBegin * InnerDim);
      for (int i = 0; i < BatchSize; ++i) {
        int ragSize = InputRags[i + 1] - InputRags[i];
        for (int j = 0; j < std::min(SliceFromBegin, ragSize); ++j) {
          for (int k = 0; k < InnerDim; ++k) {
            Expected[i * SliceFromBegin * InnerDim + j * InnerDim + k] = Input[(InputRags[i] + j) * InnerDim + k];
          }
        }
      }
    }

    int BatchSize = -1;
    int InnerDim = -1;
    TDims InputDims;
    int SliceFromBegin = 2;

    std::vector<float> Input;
    std::vector<int> InputRags;
    std::vector<float> Output;
    std::vector<float> Expected;
  };
}

TEST_P(SliceTest, Pass)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  session->SetRaggedInput(0, InputRags.size(), InputRags.data(), Input.size(), CastInput(Input));

  session->Run();
  session->GetOutput(0, inference->GetOutput(0)->GetDims().TotalSize(), Output.data());
  session->Finalize();

  CastOutput(Output);
  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Expected.size(); ++i)
    ASSERT_NEAR(Expected[i], Output[i], precision) << i << " " << i / InnerDim;

}

INSTANTIATE_TEST_SUITE_P(SliceTest, SliceTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
