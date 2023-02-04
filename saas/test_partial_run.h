#pragma once

#include <saas/rtyserver_jupi/library/extbuilder/tester_lib.h>

#include <saas/protos/rtyserver.pb.h>

#include <robot/jupiter/library/rtdoc/protos/builder_task.pb.h>

#include <library/cpp/testing/unittest/registar.h>

namespace NFusion {
    //
    // TTestPartialRunTestBase is an Extbuilder's analog to TestRtyJupiIndexDump.
    // Because Extbuilder, by design, is a SaaS-free library, we cannot reuse saas/rtyserver_test here - devtools' YaTest is used instead
    // See also: https://goals.yandex-team.ru/filter?group=24768&importance=2,1,0&goal=9121
    //
    class TTestPartialRunTestBase: public NUnitTest::TTestBase {
    protected:
        NFusion::TPartialBundleConfig BundleFilter;
        TSet<TString> IncomingLumpsFilter;
        NRtDoc::TBuilderTask AllStagesInputs;
        TVector<NRtDoc::TBuilderTask> Tasks;

        static constexpr ui32 NumMerges = 3;
        const TString TestWorkDir = "test_wd";

    protected:
        using TRawDocVector = TVector<NJupiter::TMercuryLumps>;

    private:
        static inline TFsPath MkDirTrg(TFsPath dir);

        static inline TFsPath MkDirTemp(TFsPath dir);

    public:
        static TString GetTestResource(const TFsPath& resource);

        static TRawDocVector ReadProtobin(const TFsPath& path, const TSet<TString>& lumpsFilter);

        static ui32 WritePreps(const TFsPath& prepArchive, TRawDocVector::const_iterator begin, TRawDocVector::const_iterator end);

        static TFsPath WriteFakeMergerMap(const TFsPath& dir, const ui32 mergerTaskNo, const ui32 segmentId, const ui32 docCount, ui32& newDocIdBegin);

        void MakeAllStagesInputs(const TFsPath& messagesDump, const TFsPath& workDir);

    public:
        TTestPartialRunTestBase();

        virtual void FillTaskConfig(NRtDoc::TBuilderConfig* taskConfig) const;

        virtual void OnTaskAdded(NRtDoc::TBuilderTask&) {
        }

        TString GetFinalDir() const;

        ui32 GetFinalDocCount() const;

        virtual void Build(const NRtDoc::TBuilderTask& task);

        virtual void BuildAll();

        virtual void Cleanup();
    };
}
