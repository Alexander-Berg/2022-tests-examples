#include "sis/unary_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace
{
  using namespace NSis;

  struct UnaryLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    using TAlgorithm = std::function<float(float)>;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Type = DecodeUnaryOp(GetCustom("unary"));
      Dims = TDims(2, 32);

      BuildNetwork(Type);
      CompileTestNetwork();
      auto algorithm = SelectAlgorithm(Type);
      PrepareData(algorithm);
      PerformInference();
    }

    TAlgorithm SelectAlgorithm(EUnaryOp op) const
    {
      switch (op)
      {
        case EUnaryOp::eAbs:
          return fabsf;

        case EUnaryOp::eExp:
          return expf;

        case EUnaryOp::eLog:
          return logf;

        case EUnaryOp::eSin:
          return sinf;

        case EUnaryOp::eCos:
          return cosf;

        case EUnaryOp::eSqrt:
          return sqrtf;

        case EUnaryOp::eRSqrt:
          return [](float v) {
            return 1.f / sqrtf(v);
          };

        case EUnaryOp::eReciprocal:
          return [](float v) {
            return 1.f / v;
          };

        default:
          SIS_FAIL("Not supported");
      }
    }

    void BuildNetwork(EUnaryOp type)
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddUnary(input, type);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void PrepareData(TAlgorithm algo)
    {
      Input.resize(Dims.TotalSize());
      std::iota(Input.begin(), Input.end(), 0);
      for (float& v: Input)
      {
        v = sinf(v);
        if (!IsValidValue(algo(v)))
        {
          v = -v + 1e-4;
          SIS_ENSURE(IsValidValue(algo(v)));
        }
      }

      Output.resize(Input.size());

      Test = Input;
      for (float& v: Test)
        v = algo(v);
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
    EUnaryOp Type;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(UnaryLayerTest, Ops)
{
  auto precision = GetTestTypePrecision();
  if (GetValueType() == NSis::eHalf && Type == EUnaryOp::eReciprocal)
    precision = 2.4e-2;
  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(UnaryLayerTest, UnaryLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "kernel barrier"}),
                                         NTest::TTestSetup::Custom(
                                                 "unary",
                                                 {
                                                         EUnaryOp::eExp,
                                                         EUnaryOp::eAbs,
                                                         EUnaryOp::eLog,
                                                         EUnaryOp::eSqrt,
                                                         EUnaryOp::eRSqrt,
                                                         EUnaryOp::eReciprocal,
                                                         EUnaryOp::eSin,
                                                         EUnaryOp::eCos,
                                                 }
                                                 )
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
