#include "compiler_test_setup.h"
#include "sis/inference.h"
#include <gtest/gtest.h>
#include <numeric>
#include "sis/softmax_with_lens_layer.h"
#include "sis/network_definition.h"

#include <cmath>
#include <numeric>
#include "sis/sis_assert.h"
#include <iostream>
#include "sis/data/softmax.h"
namespace
{
  using namespace NSis;
  struct SoftmaxWithLensTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      SetDumpKernelSource(true);

      BatchSize = 3;
      MaxSeqLen = 4;

      InputDims = TDims(BatchSize, MaxSeqLen, MaxSeqLen);
      LensDims = TDims(BatchSize * MaxSeqLen, 1);

      FillData();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", GetValueType(), InputDims);
      auto lens = Network->AddInput("lens", DataType::eInt32, LensDims);

      auto slice = Network->AddSoftmaxWithLens(input, lens);
      Network->MarkOutput(slice->GetOutput(0));

      return Network;
    }

    void FillData() {
      std::vector<int> lens = {2, 3, 4};
      for (size_t i = 0; i < lens.size(); ++i)
        for (int j = 0; j < MaxSeqLen; ++j)
          SeqLens.push_back(lens[i]);

      for (int i = 0; i < BatchSize * MaxSeqLen; ++i)
        for (int j = 0; j < MaxSeqLen; ++j)
          Input.push_back((i * j) / 10.0);

      Output.resize(BatchSize * MaxSeqLen * MaxSeqLen);
      Expected = Input;
      for (int b = 0; b < BatchSize * MaxSeqLen; ++b) {
        int seqLen = SeqLens[b];
        NData::Softmax(seqLen, Expected.data() + b * MaxSeqLen);
        for (int c = seqLen; c < MaxSeqLen; ++c)
          Expected[b * MaxSeqLen + c] = 0.0f;
      }
    }

    int BatchSize = -1;
    int MaxSeqLen = -1;
    TDims InputDims;
    TDims LensDims;

    std::vector<float> Input;
    std::vector<int> SeqLens;
    std::vector<float> Output;
    std::vector<float> Expected;
  };
}

TEST_P(SoftmaxWithLensTest, Pass)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  session->SetInput(0, Input.size(), CastInput(Input));
  session->SetInput(1, SeqLens.size(), SeqLens.data());

  session->Run();
  session->GetOutput(0, inference->GetOutput(0)->GetDims().TotalSize(), Output.data());
  session->Finalize();

  CastOutput(Output);
  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Expected.size(); ++i) 
    ASSERT_NEAR(Expected[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(SoftmaxWithLensTest, SoftmaxWithLensTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
