#include "sis/element_wise_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace
{
  using namespace NSis;

  struct ElementWiseLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    using TAlgorithm = std::function<float(float, float)>;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Operation = DecodeElementWiseOp(GetCustom("element wise"));
      Dims = TDims(2, 32);

      BuildNetwork(Operation);
      CompileTestNetwork();
      auto algorithm = SelectAlgorithm(Operation);
      PrepareData(algorithm);
      PerformInference();
    }

    TAlgorithm SelectAlgorithm(EElementWiseOp op) const
    {
      switch (op)
      {
        case EElementWiseOp::eSum:
          return [](float a, float b) {
            return a + b;
          };
        case EElementWiseOp::eSub:
          return [](float a, float b) {
            return a - b;
          };
        case EElementWiseOp::eProd:
          return [](float a, float b) {
            return a * b;
          };
        case EElementWiseOp::eDiv:
          return [](float a, float b) {
            return a / b;
          };
        case EElementWiseOp::eMax:
          return [](float a, float b) {
            return std::max<float>(a, b);
          };
        case EElementWiseOp::eMin:
          return [](float a, float b) {
            return std::min<float>(a, b);
          };
        case EElementWiseOp::ePow:
          return [](float a, float b) {
            return powf(a, b);
          };

        default:
          SIS_FAIL("Not supported");
      }
    }

    void BuildNetwork(EElementWiseOp type)
    {
      auto A = Network->AddInput("A", GetValueType(), Dims);
      auto B = Network->AddInput("B", GetValueType(), Dims);
      auto layer = Network->AddElementWise(A, B, type);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void PrepareData(TAlgorithm algo)
    {
      A.resize(Dims.TotalSize());
      Output.resize(A.size());
      Test.resize(A.size());
      std::iota(A.begin(), A.end(), 0);
      B = A;
      for (size_t i = 0; i != A.size(); ++i)
      {
        auto& a = A[i];
        auto& b = B[i];
        a = sinf(a);
        b = cosf(b);
        auto result = algo(a, b);
        if (!IsValidValue(result))
        {
          a = -a + 1e-3;
          b += 1e-3;
          result = algo(a, b);
          SIS_ENSURE(IsValidValue(result));
        }

        Test[i] = result;
      }
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, A.size(), CastInput(A));
      session->SetInput(1, B.size(), CastInput(B));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    TDims Dims;
    EElementWiseOp Operation;
    std::vector<float> A;
    std::vector<float> B;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(ElementWiseLayerTest, Ops)
{
  auto precision = GetTestTypePrecision();
  if (Operation == EElementWiseOp::ePow)
  {
    if (GetValueType() == NSis::eFloat)
      precision = 2e-5;
    else if (GetValueType() == NSis::eHalf)
      precision = 4e-2;
  }
  else if (Operation == EElementWiseOp::eDiv)
  {
    if (GetValueType() == NSis::eHalf)
      precision = 5e-2;
  }

  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision) << i;
}

INSTANTIATE_TEST_SUITE_P(ElementWiseLayerTest, ElementWiseLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "kernel barrier"}),
                                         NTest::TTestSetup::Custom(
                                                 "element wise",
                                                 {
                                                   EElementWiseOp::eSum,
                                                   EElementWiseOp::eSub,
                                                   EElementWiseOp::eProd,
                                                   EElementWiseOp::eDiv,
                                                   EElementWiseOp::eMax,
                                                   EElementWiseOp::eMin,
                                                   EElementWiseOp::ePow,
                                                 }
                                                 )
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
