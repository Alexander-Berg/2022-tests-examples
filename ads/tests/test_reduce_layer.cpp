#include "sis/reduce_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>
#include <bitset>

namespace
{
  using namespace NSis;

  struct ReduceLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    using TAlgorithm = std::function<float(const float*, const float*)>;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Operation = DecodeReduceOp(GetCustom("reduce"));
      Dims = TDims(2, 3, 32);
      Dims.SetInnerDims(-1);
      ReduceAxes = Dims.GetActiveMask().to_ulong();

      BuildNetwork();
      CompileTestNetwork();
      auto algorithm = SelectAlgorithm();
      PrepareData(algorithm);
      PerformInference();
    }

    TAlgorithm SelectAlgorithm() const
    {
      switch (Operation)
      {
        case EReduceOp::eMax:
          return [](const float* begin, const float* end) {
            return *std::max_element(begin, end);
          };

        case EReduceOp::eMin:
          return [](const float* begin, const float* end) {
            return *std::min_element(begin, end);
          };

        case EReduceOp::eSum:
          return [](const float* begin, const float* end) {
            return std::accumulate(begin, end, 0.f);
          };

        default:
          SIS_FAIL("Not supported");
      }
    }

    void BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddReduce(input, Operation, ReduceAxes, KeepDimensions);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    bool IsAll() const
    {
      return Dims.DimsCount == std::popcount(ReduceAxes);
    }

    void PrepareData(TAlgorithm algo)
    {
      Input.resize(Dims.TotalSize());
      std::iota(Input.begin(), Input.end(), 0);
      for (float& v: Input)
        v = sinf(v);

      SIS_ENSURE(IsAll());

      Output.resize(1);
      Test.resize(1);
      Test[0] = algo(Input.data(), Input.data() + Input.size());
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
    EReduceOp Operation;
    uint32_t ReduceAxes = 0;
    bool KeepDimensions = false;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(ReduceLayerTest, Ops)
{
  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(ReduceLayerTest, ReduceLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "kernel barrier"}),
                                         NTest::TTestSetup::Custom(
                                                 "reduce",
                                                 {
                                                   EReduceOp::eMax,
                                                   EReduceOp::eMin,
                                                   EReduceOp::eSum,
                                                 }
                                                 )
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
