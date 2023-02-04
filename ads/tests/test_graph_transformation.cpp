#include "compiler_test_setup.h"
#include "sis/graph/graph_transformation.h"
#include "sis/identity_layer.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/activation_layer.h"
#include "sis/inference.h"
#include "sis/cuda/compiler.h"
#include "sis/tensor.h"
#include "sis/data/fill_sin.h"
#include "sis/data/mma.h"
#include <gtest/gtest.h>
#include <numeric>
#include <fstream>

namespace {
  using namespace NSis;

  struct GraphTransformationTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());

      Network = Builder->CreateNetwork();
      Dims = TDims(32, 32);

      Input.resize(Dims.TotalSize());
      NData::FillSin(32, 32, Input.data());

      Output.resize(Input.size());
    }

    void BuildIdentity()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddIdentity(input);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);

      Test = Input;
    }

    void BuildGemmActivation()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto mma = Network->AddMatrixMultiply(input, input);
      auto activation = Network->AddActivation(mma->GetOutput(0), NSis::EActivationType::eRelu);

      auto output = activation->GetOutput(0);
      Network->MarkOutput(output);

      Test.resize(32 * 32);
      NData::MMA(32, 32, 32, Input.data(), Input.data(), Test.data());
      for (auto& v: Test)
        v = std::max<float>(0, v);
    }

    void Simplify()
    {
      NGraph::TGraphTransformation transform(*Network);
      Network = transform.Simplify();
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
    std::vector<float> Test;
  };
}

TEST_P(GraphTransformationTest, Identity)
{
  BuildIdentity();
  Simplify();

  ASSERT_EQ(0, Network->GetLayersCount());

  CompileTestNetwork();
  PerformInference();
  auto precision = GetTestTypePrecision();

  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}


TEST_P(GraphTransformationTest, GemmActivation)
{
  BuildGemmActivation();
  Simplify();

  ASSERT_EQ(1, Network->GetLayersCount());

  CompileTestNetwork();
  PerformInference();
  auto precision = GetTestTypePrecision();

  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(Elimination, GraphTransformationTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage"}),
                                         NTest::TTestSetup::Gemms({"cublas_lt"}),
                                         NTest::TTestSetup::ValueTypes({"float"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
