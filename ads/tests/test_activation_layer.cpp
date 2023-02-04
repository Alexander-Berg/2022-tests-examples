#include "sis/activation_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace
{
  struct TSigmoid
  {
    template<typename T>
    T operator()(T v) const
    {
      return T(1) / (T(1) + exp(-v));
    }
  };

  struct TRelu
  {
    template<typename T>
    T operator()(T v) const
    {
      return v > 0 ? v : T(0);
    }
  };

  template <typename TOp1, typename TOp2>
  struct TChain
  {
    template <typename T>
    T operator()(T v) const
    {
      const TOp1 op1;
      const TOp2 op2;
      return op2(op1(v));
    }
  };

  using namespace NSis;

  struct ActivationLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
    }

    void BuildNetwork(EActivationType type)
    {
      auto input = Network->AddInput("input", GetValueType(), TDims3(101, 11, 128));
      auto layer = Network->AddActivation(input, type);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildNetwork(EActivationType type1, EActivationType type2)
    {
      auto input = Network->AddInput("input", GetValueType(), TDims3(101, 11, 128));
      auto layer1 = Network->AddActivation(input, type1);
      auto layer2 = Network->AddActivation(layer1->GetOutput(0), type2);
      auto output = layer2->GetOutput(0);
      Network->MarkOutput(output);
    }

    template <typename TActivation>
    void PrepareData()
    {
      auto inputDims = Inference->GetInput(0)->GetDims();
      auto outputDims = Inference->GetOutput(0)->GetDims();

      ASSERT_EQ(inputDims, outputDims);

      Input.resize(inputDims.InnerSize() * inputDims.OuterSize());
      std::iota(Input.begin(), Input.end(), 0);
      for (float& v: Input)
        v = (int)(10 + v) % 20 - 10;

      Output.resize(Input.size());

      Test = Input;
      const TActivation act;
      for (float& v: Test)
        v = act(v);
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

    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(ActivationLayerTest, Sigmoid)
{
  BuildNetwork(EActivationType::eSigmoid);
  CompileTestNetwork();
  PrepareData<TSigmoid>();
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

TEST_P(ActivationLayerTest, Relu)
{
  BuildNetwork(EActivationType::eRelu);
  CompileTestNetwork();
  PrepareData<TRelu>();
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

TEST_P(ActivationLayerTest, ReluSigmoid)
{
  BuildNetwork(EActivationType::eRelu, EActivationType::eSigmoid);
  CompileTestNetwork();
  PrepareData<TChain<TRelu, TSigmoid>>();
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Test.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(ActivationLayerTest, ActivationLayerTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
