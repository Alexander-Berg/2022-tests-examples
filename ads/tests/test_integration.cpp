#include "compiler_test_setup.h"
#include "integration_graph.h"
#include "sis/inference.h"
#include "sis/inference_session.h"
#include "sis/graph/graph_dot.h"
#include "sis/network_calibration.h"
#include "sis/cuda/compiler.h"
#include "sis/int8_inference.h"
#include "sis/tensor.h"
#include <gtest/gtest.h>
#include <numeric>
#include <fstream>

namespace
{
  using namespace NSis;
  struct IntegrationTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());

      SetDumpKernelSource(true);

      Subject.FillData(SubjectData);
      Subject.Full.Type = GetValueType();
      Subject.Full.Data = CastInput(SubjectData.FullWeights);
      Subject.Full.ElementsCount = SubjectData.FullWeights.size();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      Subject.Build(*Network, GetValueType());
      return Network;
    }
    NTest::TIntegrationGraph Subject;
    NTest::TIntegrationGraph::TData SubjectData;
  };

  struct Int8IntegrationTest : IntegrationTest
  {
    void SetInputs(IInferencePtr inference, IInferenceSessionPtr session) override
    {
      auto inputDims = inference->GetInput(0)->GetDims();
      auto input = CastInput(SubjectData.Input);
      session->SetInput(0, inputDims.TotalSize(), input);
    }
  };
}

TEST_P(IntegrationTest, Pass)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  auto inputDims = inference->GetInput(0)->GetDims();
  auto outputDims = inference->GetOutput(0)->GetDims();

  session->SetInput(0, inputDims.TotalSize(), CastInput(SubjectData.Input));
  session->Run();
  session->GetOutput(0, outputDims.TotalSize(), SubjectData.Fully1Output.data());
  session->GetOutput(1, outputDims.TotalSize(), SubjectData.Fully2Output.data());
  session->GetOutput(2, outputDims.TotalSize(), SubjectData.ActivationOutput.data());
  session->GetOutput(3, outputDims.TotalSize(), SubjectData.Output.data());
  session->Finalize();
  CastOutput(SubjectData.Fully1Output);
  CastOutput(SubjectData.Fully2Output);
  CastOutput(SubjectData.ActivationOutput);
  CastOutput(SubjectData.Output);

  auto precision = GetTestTypePrecision();
  if (GetEngineType() != NTest::EEngine::eTensorRT && GetGemmType() == NSis::NCuda::eGemmTypeTensorCore)
    precision = 1e-3;
  for (size_t i = 0; i != SubjectData.Fully1Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Fully1Test[i], SubjectData.Fully1Output[i], precision);

  for (size_t i = 0; i != SubjectData.Fully2Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Fully2Test[i], SubjectData.Fully2Output[i], precision);

  for (size_t i = 0; i != SubjectData.ActivationTest.size(); ++i)
    ASSERT_NEAR(SubjectData.ActivationTest[i], SubjectData.ActivationOutput[i], precision);

  for (size_t i = 0; i != SubjectData.Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Test[i], SubjectData.Output[i], precision);
}

TEST_P(Int8IntegrationTest, Correct)
{
  BuildNetwork();
  AugmentWithInt8();
  {
    std::ofstream out("network.dot");
    NGraph::TGraphDot graphDot(out);
    graphDot.Build(*Network);
    out.close();
  }

  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  auto inputDims = inference->GetInput(0)->GetDims();
  auto outputDims = inference->GetOutput(0)->GetDims();

  session->SetInput(0, inputDims.TotalSize(), CastInput(SubjectData.Input));
  session->Run();
  session->GetOutput(0, outputDims.TotalSize(), SubjectData.Fully1Output.data());
  session->GetOutput(1, outputDims.TotalSize(), SubjectData.Fully2Output.data());
  session->GetOutput(2, outputDims.TotalSize(), SubjectData.ActivationOutput.data());
  session->GetOutput(3, outputDims.TotalSize(), SubjectData.Output.data());
  session->Finalize();
  CastOutput(SubjectData.Fully1Output);
  CastOutput(SubjectData.Fully2Output);
  CastOutput(SubjectData.ActivationOutput);
  CastOutput(SubjectData.Output);

  auto precision = 4e-3f;
  if (GetValueType() == NSis::eHalf)
    precision = 5e-3f;

  for (size_t i = 0; i != SubjectData.Fully1Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Fully1Test[i], SubjectData.Fully1Output[i], precision);

  for (size_t i = 0; i != SubjectData.Fully2Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Fully2Test[i], SubjectData.Fully2Output[i], precision);

  for (size_t i = 0; i != SubjectData.ActivationTest.size(); ++i)
    ASSERT_NEAR(SubjectData.ActivationTest[i], SubjectData.ActivationOutput[i], precision);

  for (size_t i = 0; i != SubjectData.Test.size(); ++i)
    ASSERT_NEAR(SubjectData.Test[i], SubjectData.Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(IntegrationTest, IntegrationTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::Gemms({"cublas_lt"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);

INSTANTIATE_TEST_SUITE_P(IntegrationTest, Int8IntegrationTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::Gemms({"cublas_lt"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
