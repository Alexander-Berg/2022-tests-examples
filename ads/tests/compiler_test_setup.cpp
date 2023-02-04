#include "compiler_test_setup.h"
#include "sis/graph/builder.h"
#include "sis/cuda/compiler.h"
#include "sis/cuda/standalone/deployment.h"
#include "sis/tensorrt/builder.h"
#include "sis/tensorrt/compiler.h"
#include "sis/int8_inference.h"

#include "sis/sis_assert.h"
#include <stdexcept>
#include <map>

namespace NSis {
  namespace NTest {
    EEngine DecodeEngine(const char* _name)
    {
      std::string name(_name);
      if (name == "sis")
        return EEngine::eSis;
      else if (name == "tensor_rt" || name == "tensorrt")
        return EEngine::eTensorRT;
      else
        SIS_FAIL("Unknown engine %s", name.c_str());
    }

    TTestSetups TTestSetup::Engines(std::initializer_list<const char*> engines)
    {
      TTestSetups setups;
      for (auto engine: engines)
        setups.push_back(TTestSetup{.Params={{eEngine, engine}}});
      return setups;
    }

    TTestSetups TTestSetup::Compilers(std::initializer_list<const char*> types)
    {
      TTestSetups setups;
      for (auto type: types)
        setups.push_back(TTestSetup{.Params={{eCompilerType, type}}});
      return setups;
    }

    TTestSetups TTestSetup::Gemms(std::initializer_list<const char*> gemms)
    {
      TTestSetups setups;
      for (auto gemm: gemms)
        setups.push_back(TTestSetup{.Params={{eGemmType, gemm}}});
      return setups;
    }

    TTestSetups TTestSetup::InnerDims(std::initializer_list<int> dims)
    {
      TTestSetups setups;
      for (auto dim: dims)
        setups.push_back(TTestSetup{.Params={{eInnerDimension, std::to_string(dim)}}});
      return setups;
    }

    TTestSetups TTestSetup::ValueTypes(std::initializer_list<const char*> types)
    {
      TTestSetups setups;
      for (auto type: types)
      {
        DecodeTypeName(type);
        setups.push_back(TTestSetup{.Params={{eValueType, type}}});
      }
      return setups;
    }

    TTestSetups TTestSetup::Profile(std::initializer_list<bool> options)
    {
      TTestSetups setups;
      for (auto opt: options)
      {
        setups.push_back(TTestSetup{.Params={{eProfile, std::to_string((int)opt)}}});
      }
      return setups;
    }

    bool TTestSetup::IsValid() const
    {
#ifdef ARCADIA_ROOT
      if (GetEngine() == EEngine::eTensorRT)
        return false;
#endif
      if (GetEngine() != EEngine::eSis)
      {
        return GetProfile() != true;
      }

      if (GetCompilerType() == NCuda::eCompileFusion)
      {
        auto gemmType = GetGemmType();
        return gemmType == NCuda::eGemmTypeInvalid || gemmType == NCuda::eGemmTypeCutlass;
      }

      if (GetProfile())
      {
        return GetGemmType() == NCuda::eGemmTypeCublasLt;
      }

      return true;
    }

    std::string TTestSetup::Describe() const
    {
      std::stringstream str;

      EEngine engine = GetEngine();
      if (engine == EEngine::eTensorRT)
      {
        str << "tensor_rt";
        if (!CustomDescriptions.empty())
        {
          str << " ";
          for (const auto& description: CustomDescriptions)
            str << description << " ";
        }
      }
      else
      {
        if (!CustomDescriptions.empty())
        {
          for (const auto& description: CustomDescriptions)
            str << description << " ";
        }

        SIS_VERIFY(engine == EEngine::eSis);
        NCuda::ECompileType compilerType = GetCompilerType();
        str << NCuda::EncodeCompileType(compilerType);

        auto gemmType = GetGemmType();
        if (gemmType != NCuda::eGemmTypeInvalid)
          str << " " << NCuda::EncodeGemmType(gemmType);
      }

      if (GetValueType() != eFloat)
        str << " " << EncodeTypeName(GetValueType());

      if (int innerDim = GetInnerDim())
        str << " " << innerDim;

      if (GetProfile())
        str << " " << "profiled";

      return GoogleName(str.str());
    }

    EEngine TTestSetup::GetEngine() const
    {
      if (auto found = Get(eEngine))
        return DecodeEngine(found);
      else
        return EEngine::eInvalid;
    }

    NCuda::ECompileType TTestSetup::GetCompilerType() const
    {
      if (auto found = Get(eCompilerType))
        return NCuda::DecodeCompileType(found);
      else
        return NCuda::eCompileInvalid;
    }

    NCuda::EGemmType TTestSetup::GetGemmType() const
    {
      if (auto found = Get(eGemmType))
        return NCuda::DecodeGemmType(found);
      else
        return NCuda::eGemmTypeInvalid;
    }

    DataType TTestSetup::GetValueType() const
    {
      if (auto found = Get(eValueType))
        return DecodeTypeName(found);
      else
        return eFloat;
    }

    int TTestSetup::GetInnerDim() const
    {
      if (auto found = Get(eInnerDimension))
        return atoi(found);
      else
        return 0;
    }

    bool TTestSetup::GetProfile() const
    {
      if (auto found = Get(eProfile))
        return atoi(found);
      else
        return false;
    }

    const char* TTestSetup::Get(const std::string& param) const
    {
      auto found = Params.find(param);
      return found != Params.end() ? found->second.c_str() : nullptr;
    }

    int TTestSetup::GetInt(const std::string &params, int defaultValue) const
    {
      if (auto found = Get(params))
        return atoi(found);
      else
        return defaultValue;
    }

    TTestSetup TTestSetup::AddParams(const TTestSetup& other) const
    {
      TTestSetup r = *this;

      r.CustomDescriptions.insert(r.CustomDescriptions.end(), other.CustomDescriptions.begin(), other.CustomDescriptions.end());

      for (const auto& param: other.Params)
      {
        bool alreadyHas = Get(param.first);
        if (alreadyHas)
          continue;

        if (GetEngine() == EEngine::eTensorRT && param.first == eCompilerType)
          continue;
        if (GetEngine() == EEngine::eTensorRT && param.first == eGemmType)
          continue;

        r.Params[param.first] = param.second;
      }
      return r;
    }

    std::string TTestSetup::GoogleName(std::string name) const
    {
      for (auto& c: name)
        c = c == ' ' ? '_' : c;
      return name;
    }

    INetworkDefinitionPtr TBaseTestPipeline::CreateTestNetwork()
    {
      switch (TestSetup2.GetEngine())
      {
        case EEngine::eSis:
          Builder = NGraph::CreateBuilder();
          break;

        case EEngine::eTensorRT:
          Builder = NTensorRT::CreateBuilder();
          break;

        default:
          SIS_FAIL("Unknown engine");
      }

      Network = Builder->CreateNetwork();

      return Network;
    }

    IInferencePtr TBaseTestPipeline::CompileTestNetwork()
    {
      switch (TestSetup2.GetEngine())
      {
        case EEngine::eSis:
          Inference = CompileGraph(Network);
          break;

        case EEngine::eTensorRT:
          CompileTensorRT();
          break;

        default:
          SIS_FAIL("Unknown engine");
      }

      return Inference;
    }

    IInferencePtr TBaseTestPipeline::CompileGraph(INetworkDefinitionPtr network)
    {
      NCuda::TCompilerConfig config = {};
      config.Type = TestSetup2.GetCompilerType();
      config.GemmType = TestSetup2.GetGemmType();
      config.DumpKernelSource = DumpKernelSource;
      if (config.Type == NCuda::eCompileFusion || config.Type == NCuda::eCompileKernelBarrier)
        config.Packages = NSis::NProgram::NCuda::DeployPackages();

      NCuda::TCompiler compiler;
      return compiler.Compile(config, network);
    }

    void TBaseTestPipeline::CompileTensorRT()
    {
      NTensorRT::TCompilerConfig config = {};

      NTensorRT::TCompiler compiler;
      Inference = compiler.Compile(config, Network);
    }

    void TBaseTestPipeline::SetDumpKernelSource(bool value)
    {
      DumpKernelSource = value;
    }

    void TBaseTestPipeline::InitSetup(const TTestSetup &setup)
    {
      TestSetup2 = setup;
      CreateTestNetwork();
    }

    EEngine TBaseTestPipeline::GetEngineType() const
    {
      return TestSetup2.GetEngine();
    }

    NCuda::ECompileType TBaseTestPipeline::GetCompilerType() const
    {
      return TestSetup2.GetCompilerType();
    }

    NCuda::EGemmType TBaseTestPipeline::GetGemmType() const
    {
      return TestSetup2.GetGemmType();
    }

    DataType TBaseTestPipeline::GetValueType() const
    {
      return TestSetup2.GetValueType();
    }

    int TBaseTestPipeline::GetInnerDim() const
    {
      return TestSetup2.GetEngine() != EEngine::eInvalid ? TestSetup2.GetInnerDim() : -1;
    }

    bool TBaseTestPipeline::NeedProfiling() const
    {
      return TestSetup2.GetProfile();
    }

    float TBaseTestPipeline::GetTestTypePrecision() const
    {
      auto type = GetValueType();
      switch (type)
      {
        case eFloat:
          return 1e-5;

        case eHalf:
          return 1.2f * std::numeric_limits<half_t>::epsilon();

        case eInt32:
          return 0;

        case eInt8:
          return 1;

        default:
          SIS_FAIL("Type %s not supported", EncodeTypeName(type));
      }
    }

    const char *TBaseTestPipeline::GetCustom(const std::string &param) const
    {
      return TestSetup2.Get(param);
    }

    int TBaseTestPipeline::GetInt(const std::string& param, int defaultValue) const
    {
      return TestSetup2.GetInt(param, defaultValue);
    }

    DataType TBaseTestPipeline::GetCastDataType() const
    {
      return GetValueType();
    }

    bool TBaseTestPipeline::IsValidValue(float v)
    {
      return !std::isnan(v) && !std::isinf(v);
    }

    void TBaseTestPipeline::Feed(INetworkDefinitionPtr network, ICalibrationBatch &consumer)
    {
      auto inference = CompileGraph(network);
      auto session = inference->StartSession();
      while (true)
      {
        SetInputs(inference, session);
        if (!consumer.Update(*inference, *session))
          break;
      }
    }

    void TBaseTestPipeline::SetInputs(IInferencePtr inference, IInferenceSessionPtr session)
    {
      (void)inference;
      (void)session;
      SIS_NOT_IMPLEMENTED();
    }

    void TBaseTestPipeline::AugmentWithInt8()
    {
      Network = BuildInt8Network(*Network, *this);
    }
  }
}
