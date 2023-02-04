#include <gtest/gtest.h>
#include "sis/program/instruction_templates.h"
#include "sis/cuda/language/base_cuda_instruction.h"
#include "sis/cuda/kernel_source.h"
#include "sis/cuda/kernel_ptx.h"
#include "sis/cuda/language/language.h"
#include "sis/cuda/standalone/deployment.h"
#include "sis/graph/tensor.h"
#include "sis/dims.h"
#include <sstream>

using namespace NSis;

namespace
{
  using namespace NSis::NProgram::NCuda;

  struct TTestParam
  {
    DataType TensorType;
    const char* TypeName;
    int InnerDim;
    int SliceDim;
  };

  struct TTestParamsInstruction : NProgram::NCuda::TBaseCudaInstruction
  {
    TTestParamsInstruction(const char* name)
    : TBaseCudaInstruction(name)
    {
    }

    int GetGridSize() const override
    {
      return 0;
    }

    TTensors GetInputs() const override
    {
      return Inputs;
    }

    IInstruction::TTensors GetOutputs() const override
    {
      return Outputs;
    }

    NProgram::IInstructionParamsPtr GetParams() const override
    {
      if (UseParams)
        return std::make_shared<TTestParams>();
      return {};
    }

    ETensorFormat GetCudaTensorFormat(const ITensor* tensor) const override
    {
      auto found = TensorFormats.find(tensor);
      return found != TensorFormats.end() ? found->second : ETensorFormat::eDense;
    }

    TTensors GetNeedMaterialization() const override
    {
      return Materializations;
    }

    struct TTestParams : NProgram::IInstructionParams
    {
      TParamsPtr Instantiate(NGraph::IRaggedState &, const NProgram::IParamsInstantiation&) const override
      {
        return {};
      }
    };

    bool UseParams = false;
    TTensors Inputs;
    TTensors Outputs;
    TTensors Materializations;
    std::map<const ITensor*, ETensorFormat> TensorFormats;
  };

  struct InOutKernelTest : ::testing::TestWithParam<TTestParam>
  {
    InOutKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
    }

    void SetUp() override
    {
      Param = GetParam();

      InputTensor = std::make_shared<NGraph::TTensor>("input", Param.TensorType, TDims2(64, Param.InnerDim));
      OutputTensor = std::make_shared<NGraph::TTensor>("output", Param.TensorType, TDims2(64, Param.InnerDim));

      auto op = std::make_shared<TTestParamsInstruction>("test_op_t");
      auto inputBind = Code.Load(InputTensor.get(), op);
      Code.Store(inputBind, OutputTensor.get());
    }

    TTestParam Param;

    ITensorSharedPtr InputTensor;
    ITensorSharedPtr OutputTensor;

    TKernelSourceCode Code;
  };

  struct ParamsKernelTest : ::testing::Test
  {
    ParamsKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
      TLanguageParams params = {};
      TLanguage language(params);
      auto op = std::make_shared<TTestParamsInstruction>("test_params_op_t");
      op->UseParams = true;
      auto outs = Code.Execute(op, {});
    }

    TKernelSourceCode Code;
  };

  struct SharedMemKernelTest : ::testing::Test
  {
    SharedMemKernelTest()
      : Code({"TestKernel", {64, 4}})
    {
      TLanguageParams params = {};
      TLanguage language(params);
      auto op = std::make_shared<TTestParamsInstruction>("shared_mem_op_t");
      op->SetSharedMemSize(1);
      auto outs = Code.Execute(op, {});
    }

    TKernelSourceCode Code;
  };

  struct ZeroOutputsKernelTest : ::testing::Test
  {
    ZeroOutputsKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
      TLanguageParams params = {};
      TLanguage language(params);
      auto op = std::make_shared<TTestParamsInstruction>("zero_op_t");
      auto outs = Code.Execute(op, {});
    }

    TKernelSourceCode Code;
  };

  struct One2OneInstructionKernelTest : ::testing::Test
  {
    One2OneInstructionKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
      InputTensor = std::make_shared<NGraph::TTensor>("input", eFloat, TDims2(64, 64));
      OutputTensor = std::make_shared<NGraph::TTensor>("output", eFloat, TDims2(64, 64));

      auto op = std::make_shared<TTestParamsInstruction>("identity_op_t");
      op->Inputs = {InputTensor.get()};
      op->Outputs = {OutputTensor.get()};

      auto inputBind = Code.Load(InputTensor.get(), op);
      auto outs = Code.Execute(op, {inputBind});
      Code.Store(outs[0], OutputTensor.get());
    }

    ITensorSharedPtr InputTensor;
    ITensorSharedPtr OutputTensor;

    TKernelSourceCode Code;
  };

  struct Two2OneInstructionKernelTest : ::testing::Test
  {
    Two2OneInstructionKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
      Input0Tensor = std::make_shared<NGraph::TTensor>("input0", eFloat, TDims2(64, 64));
      Input1Tensor = std::make_shared<NGraph::TTensor>("input1", eFloat, TDims2(64, 64));
      OutputTensor = std::make_shared<NGraph::TTensor>("output", eFloat, TDims2(64, 64));

      auto op = std::make_shared<TTestParamsInstruction>("test_op_t");
      op->Inputs = {Input0Tensor.get(), Input1Tensor.get()};
      op->Outputs = {OutputTensor.get()};

      auto input0Bind = Code.Load(Input0Tensor.get(), op);
      auto input1Bind = Code.Load(Input1Tensor.get(), op);

      auto outs = Code.Execute(op, {input0Bind, input1Bind});

      Code.Store(outs[0], OutputTensor.get());
    }

    ITensorSharedPtr Input0Tensor;
    ITensorSharedPtr Input1Tensor;
    ITensorSharedPtr OutputTensor;

    TKernelSourceCode Code;
  };

  struct SequentialInstructionKernelTest : ::testing::Test
  {
    SequentialInstructionKernelTest()
            : Code({"TestKernel", {64, 4}})
    {
      InputTensor = std::make_shared<NGraph::TTensor>("input", eFloat, TDims2(64, 64));
      IntermediateTensor = std::make_shared<NGraph::TTensor>("intermediate", eFloat, TDims2(64, 64));
      OutputTensor = std::make_shared<NGraph::TTensor>("output", eFloat, TDims2(64, 64));

      auto op = std::make_shared<TTestParamsInstruction>("first_op_t");
      op->Inputs = {InputTensor.get()};
      op->Outputs = {IntermediateTensor.get()};

      auto next = std::make_shared<TTestParamsInstruction>("second_op_t");
      next->Inputs = {IntermediateTensor.get()};
      next->Outputs = {OutputTensor.get()};

      auto inputBind = Code.Load(InputTensor.get(), op);
      auto nested = Code.Execute(op, {inputBind});
      auto outs = Code.Execute(next, {nested[0]});
      Code.Store(outs[0], OutputTensor.get());
    }

    ITensorSharedPtr InputTensor;
    ITensorSharedPtr IntermediateTensor;
    ITensorSharedPtr OutputTensor;

    TKernelSourceCode Code;
  };
}

std::string EncodeTensorRepresentation(ETensorFormat type)
{
  switch (type)
  {
    case ETensorFormat::eDense:
      return "dense";
    case ETensorFormat::eBroadcast:
      return "broadcast";
    case ETensorFormat::eTyped:
      return "typed";
    default:
      SIS_FAIL("Unknown tensor format");
  }
}

struct TensorRepresentationTest : ::testing::TestWithParam<ETensorFormat>
{
  TensorRepresentationTest()
          : Code({"TestKernel", {64, 4}})
  {
  }

  void SetUp() override
  {
    TensorFormat = GetParam();

    InputTensor = std::make_shared<NGraph::TTensor>("input", eFloat, TDims2(64, 64));
    OutputTensor = std::make_shared<NGraph::TTensor>("output", eFloat, TDims2(64, 64));

    auto op = std::make_shared<TTestParamsInstruction>("test_op_t");
    op->Inputs = {InputTensor.get()};
    op->Outputs = {OutputTensor.get()};
    op->TensorFormats[InputTensor.get()] = TensorFormat;

    auto inputBind = Code.Load(InputTensor.get(), op);
    auto outs = Code.Execute(op, {inputBind});
    Code.Store(outs[0], OutputTensor.get());
  }

  ETensorFormat TensorFormat;
  ITensorSharedPtr InputTensor;
  ITensorSharedPtr OutputTensor;

  TKernelSourceCode Code;
};

struct OutputFormatMismatchTest : ::testing::Test
{
  OutputFormatMismatchTest()
          : Code({"TestKernel", {64, 4}})
  {
  }

  void SetUp() override
  {
    InputTensor = std::make_shared<NGraph::TTensor>("input", eFloat, TDims2(64, 64));
    ConflictTensor = std::make_shared<NGraph::TTensor>("conflict", eFloat, TDims2(64, 64));
    OutputTensor = std::make_shared<NGraph::TTensor>("output", eFloat, TDims2(64, 64));

    auto op = std::make_shared<TTestParamsInstruction>("test_op_t");
    op->Inputs = {InputTensor.get()};
    op->Outputs = {ConflictTensor.get()};

    auto conflictOp = std::make_shared<TTestParamsInstruction>("mismatch_op_t");
    conflictOp->Inputs = {ConflictTensor.get()};
    conflictOp->TensorFormats[ConflictTensor.get()] = ETensorFormat::eBroadcast;
    conflictOp->Outputs = {OutputTensor.get()};;

    auto inputBind = Code.Load(InputTensor.get(), op);
    auto outs = Code.Execute(op, {inputBind});
    Code.Store(outs[0], ConflictTensor.get());
    Code.Barrier();
    auto resolvedInput = Code.Load(ConflictTensor.get(), conflictOp);
    auto next = Code.Execute(conflictOp, {outs[0]});
    Code.Store(next[0], OutputTensor.get());
  }

  ITensorSharedPtr InputTensor;
  ITensorSharedPtr ConflictTensor;
  ITensorSharedPtr OutputTensor;

  TKernelSourceCode Code;
};

TEST_P(InOutKernelTest, Signature)
{
  auto kernelSignature = Code.GenerateSignature();
  const char testSignature[] =
          "extern \"C\" __global__\n"
          "void TestKernel(\n"
          "  const const_tensor_t tensor_0,\n"
          "  tensor_t tensor_1)"
          ;

  ASSERT_STREQ(testSignature, kernelSignature.c_str());
}

TEST_F(ParamsKernelTest, Signature)
{
  auto kernelSignature = Code.GenerateSignature();
  const char testSignature[] =
          "extern \"C\" __global__\n"
          "void TestKernel(\n"
          "  const test_params_op_t::params_t params0)"
  ;

  ASSERT_STREQ(testSignature, kernelSignature.c_str());
}

TEST_F(ParamsKernelTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::stringstream test;
  test << "  test_params_op_t op_0(params0);\n";
  test << "  op_0();\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_F(SharedMemKernelTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::stringstream test;
  test << "  shared_mem_op_t::shared_storage_t* shared_storage_0 = reinterpret_cast<shared_mem_op_t::shared_storage_t*>(shared_storage_base);\n";
  test << "  shared_mem_op_t op_0(*shared_storage_0);\n";
  test << "  op_0();\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_F(SharedMemKernelTest, Prolog)
{
  auto kernelBody = Code.GenerateProlog();
  std::stringstream test;
  test << "  extern __shared__ int shared_storage_base[];\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_F(ZeroOutputsKernelTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::stringstream test;
  test << "  zero_op_t op_0;\n";
  test << "  op_0();\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST(AccessSize, Aligned)
{
  ASSERT_EQ(1, CalculateAccessSize(64, 1, 4));
  ASSERT_EQ(2, CalculateAccessSize(64 * 6, 6, 4));
}

TEST(AccessType, AllPresent)
{
  ASSERT_STREQ("short", GetAccessType(2, 1));
  ASSERT_STREQ("int", GetAccessType(2, 2));
  ASSERT_STREQ("int4", GetAccessType(2, 8));
}

TEST_P(InOutKernelTest, Prolog)
{
  auto kernelBody = Code.GenerateProlog();
  int accessSize = CalculateAccessSize(Param.InnerDim, Param.SliceDim, GetDataTypeSize(Param.TensorType));
  const char* accessType = GetAccessType(GetDataTypeSize(Param.TensorType), accessSize);

  std::stringstream test;
  test << "  const dense_tensor_t<const " << Param.TypeName << ", "
    << "dense_slice_shape_t<" << Param.SliceDim  << ", " <<  accessSize << ", " << accessType << ">"
    << "> dense_tensor_0(tensor_0, blockIdx.x, threadIdx.x);" << "\n";

  test << "  dense_tensor_t<" << Param.TypeName << ", "
  << "dense_slice_shape_t<" << Param.SliceDim << ", " << accessSize << ", " << accessType << ">"
  << "> dense_tensor_1(tensor_1, blockIdx.x, threadIdx.x);" << "\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

INSTANTIATE_TEST_SUITE_P(InOutKernelTest, InOutKernelTest,
                        ::testing::Values(
                                TTestParam{eFloat, "float", 64, 1},
                                TTestParam{eFloat, "float", 384, 6}
                                )
);

TEST_F(One2OneInstructionKernelTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::stringstream test;
  test << "  auto tensor_0_dense_slice = dense_tensor_0.read();\n";
  test << "  identity_op_t op_0;\n";
  test << "  auto op_0_out_0 = op_0(tensor_0_dense_slice);\n";
  test << "  dense_tensor_1.write(op_0_out_0);\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_F(SequentialInstructionKernelTest, Layout)
{
  auto layout = Code.GetLayout();
  ASSERT_EQ(layout.Inputs.size(), 1u);
  ASSERT_EQ(layout.Outputs.size(), 1u);
  ASSERT_EQ(layout.Params.size(), 0u);
  ASSERT_EQ(layout.Inputs[0], InputTensor.get());
  ASSERT_EQ(layout.Outputs[0], OutputTensor.get());
}

TEST_F(One2OneInstructionKernelTest, CubinCompiles)
{
  auto source = Code.GenerateFullSource();
  TKernelPtx::TConfig config = {};
  config.CudaContext = std::make_shared<NCE::TCudaContext>();
  config.Packages = DeployPackages();
  TKernelPtx compiler(config);
  auto success = compiler.Compile("test_kernel", source);
  if (!success)
    fprintf(stderr, "Program:\n%s\nLog:\n%s\n", source.c_str(), compiler.GetLog().c_str());
  ASSERT_TRUE(success);
}

TEST_F(Two2OneInstructionKernelTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::stringstream test;
  test << "  auto tensor_0_dense_slice = dense_tensor_0.read();\n";
  test << "  auto tensor_1_dense_slice = dense_tensor_1.read();\n";
  test << "  test_op_t op_0;\n";
  test << "  auto op_0_out_0 = op_0(tensor_0_dense_slice, tensor_1_dense_slice);\n";
  test << "  dense_tensor_2.write(op_0_out_0);\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}


struct TRepresentationDescription
{
  template<typename T>
  std::string operator()(const T &info) const
  {
    return EncodeTensorRepresentation(info.param);
  }
};

TEST_P(TensorRepresentationTest, Prolog)
{
  auto kernelBody = Code.GenerateProlog();
  std::string repr = EncodeTensorRepresentation(TensorFormat);

  std::stringstream test;
  test << "  const " << repr << "_tensor_t<" << "const float, dense_slice_shape_t<1, 1, int>> " << repr << "_tensor_0(tensor_0, blockIdx.x, threadIdx.x);" << "\n";
  test << "  dense_tensor_t<float, dense_slice_shape_t<1, 1, int>> dense_tensor_1(tensor_1, blockIdx.x, threadIdx.x);" << "\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_P(TensorRepresentationTest, Body)
{
  auto kernelBody = Code.GenerateBody();
  std::string repr = EncodeTensorRepresentation(TensorFormat);

  std::stringstream test;
  test << "  auto tensor_0_" << repr << "_slice = " << repr << "_tensor_0.read();\n";
  test << "  test_op_t op_0;\n";
  test << "  auto op_0_out_0 = op_0(tensor_0_" << repr << "_slice);\n";
  test << "  " << "dense" << "_tensor_1.write(op_0_out_0);\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

INSTANTIATE_TEST_SUITE_P(TensorRepresentationTest, TensorRepresentationTest,
                        ::testing::Values(
                                ETensorFormat::eDense,
                                ETensorFormat::eBroadcast,
                                ETensorFormat::eTyped
                        ),
                        TRepresentationDescription()
                        );

TEST_F(OutputFormatMismatchTest, Signature)
{
  auto kernelBody = Code.GenerateSignature();

  std::stringstream test;
  test << "extern \"C\" __global__\n";
  test << "void TestKernel(\n";
  test << "  const const_tensor_t tensor_0,\n";
  test << "  tensor_t tensor_1,\n";
  test << "  tensor_t tensor_2)";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

TEST_F(OutputFormatMismatchTest, Prolog)
{
  auto kernelBody = Code.GenerateProlog();

  std::stringstream test;
  test << "  const dense_tensor_t<const float, dense_slice_shape_t<1, 1, int>> dense_tensor_0(tensor_0, blockIdx.x, threadIdx.x);\n";
  test << "  const broadcast_tensor_t<const float, dense_slice_shape_t<1, 1, int>> broadcast_tensor_1(tensor_1, blockIdx.x, threadIdx.x);\n";
  test << "  dense_tensor_t<float, dense_slice_shape_t<1, 1, int>> dense_tensor_1(tensor_1, blockIdx.x, threadIdx.x);\n";
  test << "  dense_tensor_t<float, dense_slice_shape_t<1, 1, int>> dense_tensor_2(tensor_2, blockIdx.x, threadIdx.x);\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}


TEST_F(OutputFormatMismatchTest, Body)
{
  auto kernelBody = Code.GenerateBody();

  std::stringstream test;
  test << "  auto tensor_0_dense_slice = " << "dense_tensor_0.read();\n";
  test << "  test_op_t op_0;\n";
  test << "  auto op_0_out_0 = op_0(tensor_0_dense_slice);\n";
  test << "  dense_tensor_1.write(op_0_out_0);\n";
  test << "  cooperative_groups::this_grid().sync();\n";
  test << "  auto tensor_1_broadcast_slice = broadcast_tensor_1.read();\n";
  test << "  mismatch_op_t op_1;\n";
  test << "  auto op_1_out_0 = op_1(op_0_out_0);\n";
  test << "  dense_tensor_2.write(op_1_out_0);\n";

  ASSERT_STREQ(test.str().c_str(), kernelBody.c_str());
}

#ifndef ARCADIA_ROOT
TEST(PtxPackages, Compose)
{
  using NProgram::NCuda::TIncludeDirs;
  TIncludeDirs dirs;
  auto packages = dirs.FromSourceTree();
  dirs.Compose(packages, "test_compose.tar", "test_compose.tar.hash");
}
#endif
