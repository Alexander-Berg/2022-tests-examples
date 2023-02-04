#include "compiler_test_setup.h"
#include "sis/constant_layer.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/data/fill_sin.h"
#include "sis/data/mma.h"
#include "sis/data/normalize.h"
#include <gtest/gtest.h>

namespace {
  using namespace NSis;

  struct RaggedMatrixMultiplyTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    void SetUp() override
    {
      InitSetup(GetParam());

      PrepareData();
      BuildNetwork();
      CompileTestNetwork();
    }

    void PrepareData()
    {
      int inputDim = 128;
      int innerDim = 64;
      int maxTokens = 128;
      int batchSize = 3;
      InputDims = TDims(batchSize, maxTokens, inputDim);
      InputDims.SetInnerDims(-1);
      InputDims.SetRaggedDimension(1);

      WeightsDims = TDims(innerDim, inputDim);
      WeightsDims.SetInnerDims(-1);

      Offsets.push_back(0);
      for (int i = 0; i != batchSize; ++i)
        Offsets.push_back(Offsets.back() + ((1 + i) * 15) % maxTokens);

      auto itemsCount = Offsets.back();

      Input.resize(itemsCount * inputDim);
      NData::FillSin(itemsCount, inputDim, Input.data());
      NData::NormalizeL2(itemsCount, inputDim, Input.data());

      Weights.resize(WeightsDims.TotalSize());
      NData::FillCos(innerDim, inputDim, Weights.data());
      NData::NormalizeL2(innerDim, inputDim, Weights.data());

      Test.resize(innerDim * maxTokens);
      NData::MMA(innerDim, itemsCount, inputDim, Weights.data(), Input.data(), Test.data());

      Output.resize(Test.size());
    }

    void BuildNetwork()
    {
       auto testValueType = GetValueType();

       auto makeXWeights = [this, testValueType](const std::vector<float>& values) {
        TWeights weights = {
                .Type=testValueType,
                .Data=CastInput(values),
                .ElementsCount=(int64_t)values.size()
        };
        return weights;
      };

      auto weights = makeXWeights(Weights);
      auto A = Network->AddConstant(WeightsDims, weights)->GetOutput(0);
      auto B = Network->AddInput("B", testValueType, InputDims);
      auto D = Network->AddMatrixMultiply(A, B)->GetOutput(0);
      Network->MarkOutput(D);
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();

      session->SetRaggedInput(0, Offsets.size(), Offsets.data(), Input.size(), CastInput(Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    TDims InputDims;
    TDims WeightsDims;

    std::vector<float> Input;
    std::vector<int> Offsets;
    std::vector<float> Weights;
    std::vector<float> Test;
    std::vector<float> Output;
  };
}

TEST_P(RaggedMatrixMultiplyTest, Valid)
{
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Test.size(); ++i)
  {
    ASSERT_NEAR(Test[i], Output[i], precision) << "i " << i;
  }
}

INSTANTIATE_TEST_SUITE_P(Ragged, RaggedMatrixMultiplyTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis"}),
                                        NTest::TTestSetup::Compilers({"stage"}),
                                        NTest::TTestSetup::ValueTypes({"float"}),
                                        NTest::TTestSetup::Gemms({"cublas_lt"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
