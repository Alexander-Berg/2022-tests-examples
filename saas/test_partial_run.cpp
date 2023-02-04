#include "test_partial_run.h"

#include <google/protobuf/messagext.h>

#include <robot/jupiter/library/rtdoc/file/docidmap_io.h>
#include <robot/jupiter/library/rtdoc/file/writer.h>
#include <robot/jupiter/library/rtdoc/file/model/files.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/stream/file.h>
#include <util/string/printf.h>


namespace {
    class TTestMessageValidator {
    //TODO(REFRESH-391): the production needs a device like TTestMessageValidator. This should not be test-specific code
    private:
        THashMap<TString, ui32> RequiredLumps;
        TVector<bool> CurrentDoc;

    public:
        inline void AddRequired(const TString& prepId) {
            Y_VERIFY_DEBUG(CurrentDoc.empty(), "do this before Init()");
            ui32 cnt = RequiredLumps.size();
            if (!RequiredLumps.contains(prepId)) {
                RequiredLumps[prepId] = cnt;
            }
        }

        inline void Init() {
            CurrentDoc.resize(RequiredLumps.size(), false);
        };

        inline void StartDoc() {
            std::fill(CurrentDoc.begin(), CurrentDoc.end(), false);
        }

        inline void Count(const TString& prepId) {
            auto lookup = RequiredLumps.find(prepId);
            if (lookup != RequiredLumps.end()) {
                CurrentDoc[lookup->second] = true;
            }
        }

        inline bool IsValid() const {
            return std::find(CurrentDoc.begin(), CurrentDoc.end(), false) == CurrentDoc.end();
        }

    public:
        TTestMessageValidator() = default;

        TTestMessageValidator(const TSet<TString>& lumpsFilter) {
            // auto-detect _lumps tables for tests, require them
            static constexpr TStringBuf plutonumLumpSuffix = "_lumps";
            // this one is required because NFusion::MergeFrq uses inv_url_hash to check if document exists (not obvious, I know)
            static const TSet<TString> knownMandatoryPrepIds = { "inv_url_hash" };
            for (const auto& prepId: lumpsFilter) {
                if (prepId.EndsWith(plutonumLumpSuffix) || knownMandatoryPrepIds.contains(prepId)) {
                    AddRequired(prepId);
                }
            }
            Init();
        }

        inline TString DebugGetMissing() const {
            TStringStream s;
            for (const auto& kv: RequiredLumps) {
                if (!CurrentDoc.at(kv.second)) {
                    if (!s.empty()) {
                        s << " ";
                    }
                    s << kv.first;
                }
            }
            return s.Str();
        }
    };

    bool InitLogging() {
         InitGlobalLog2Console(TLOG_DEBUG);
         return true;
    }
}

namespace NFusion {
    TTestPartialRunTestBase::TTestPartialRunTestBase() {
        static bool logInitializator = InitLogging();
        Y_UNUSED(logInitializator);
    }

    TTestPartialRunTestBase::TRawDocVector TTestPartialRunTestBase::ReadProtobin(const TFsPath& path, const TSet<TString>& lumpsFilter) {
        TRawDocVector result;
        using TMessage = NRTYServer::TMessage;
        using TProtoReader = ::google::protobuf::io::TProtoReader;
        TFileInput fin(path);
        TProtoReader reader(&fin);
        TTestMessageValidator validator(lumpsFilter);
        for (;;) {
            TMessage msg;
            if (!reader.Load(msg))
                break;
            if (!msg.HasDocument() || !msg.GetDocument().HasIndexedDoc())
                continue;

            result.emplace_back();
            NJupiter::TMercuryLumps& doc = result.back();
            const NRealTime::TIndexedDoc& indexedDoc = msg.GetDocument().GetIndexedDoc();

            validator.StartDoc();
            for (size_t i = 0; i < indexedDoc.WadLumpsSize(); ++i) {
                const auto& kv = indexedDoc.GetWadLumps(i);
                if (lumpsFilter.contains(kv.GetName())) {
                    auto& lump = *doc.AddLumps();
                    lump.SetName(kv.GetName());
                    lump.SetData(kv.GetData());
                    validator.Count(kv.GetName());
                }
            }
            Y_ENSURE(validator.IsValid(), "Missing required lumps " << validator.DebugGetMissing() << " for NRTYServer::TMessage item #" << result.size() - 1);
        }
        return result;
    }

    ui32 TTestPartialRunTestBase::WritePreps(const TFsPath& prepArchive, TRawDocVector::const_iterator begin, TRawDocVector::const_iterator end) {
        TBuffer buffer;
        NRtDoc::TPrepWriter writer(prepArchive);
        ui32 docId = 0;
        for (TRawDocVector::const_iterator p = begin; p != end; ++p) {
            buffer.Resize(p->ByteSize());
            Y_VERIFY(p->SerializeToArray(buffer.Data(), buffer.Size()));
            writer.AddData(docId++, TBlob::NoCopy(buffer.Data(), buffer.Size()));
        }
        writer.Finish();
        const ui32 totalDocsInSegment = docId;
        return totalDocsInSegment;
    }

    TFsPath TTestPartialRunTestBase::WriteFakeMergerMap(const TFsPath& dir, const ui32 mergerTaskNo, const ui32 segmentId, const ui32 docCount, ui32& newDocIdBegin) {
        NRtDoc::TDocIdMap m;
        m.SetZeroMap(docCount);
        const ui32 endDocId = newDocIdBegin + docCount;
        auto& data = *m.MutableData();
        for (ui32 oldDocId = 0; oldDocId < docCount; ++oldDocId) {
            data[oldDocId] = newDocIdBegin + oldDocId;
        }

        const TFsPath path = dir / Sprintf("test_merge_%d.segment_%d.mapping", mergerTaskNo, segmentId);
        NRtDoc::TDocIdMapIo::Save(path, &m);
        newDocIdBegin = endDocId;
        return path;
    }

    inline TFsPath TTestPartialRunTestBase::MkDirTrg(TFsPath dir) {
        dir.MkDirs();
        return dir;
    }

    inline TFsPath TTestPartialRunTestBase::MkDirTemp(TFsPath dir) {
        return MkDirTrg(dir);
    }

    void TTestPartialRunTestBase::MakeAllStagesInputs(const TFsPath& messagesDump, const TFsPath& workDir) {
        constexpr ui32 NumSegments = NumMerges;

        Tasks.clear();

        TVector<NJupiter::TMercuryLumps> rawData = ReadProtobin(messagesDump, IncomingLumpsFilter);
        const size_t docsCount = rawData.size();
        const size_t docsPerSegment = 1 + docsCount / NumSegments;

        TBuffer buffer;
        ui32 segmentId = 0;
        for (size_t beginDocId = 0; beginDocId < docsCount; ++segmentId, beginDocId += docsPerSegment) {
            const size_t endDocId = Min(beginDocId + docsPerSegment, docsCount);

            TString segmentFolder = Sprintf("prep_index_%d", segmentId);
            TFsPath segmentPath = workDir / segmentFolder;
            segmentPath.MkDirs();

            const ui32 docsInSegment = WritePreps(segmentPath / NRtDoc::TBuilderFiles::MainPrepFile, rawData.cbegin() + beginDocId, rawData.cbegin() + endDocId);

            NRtDoc::TBuilderTask::TBuilderInput& input = *AllStagesInputs.AddInputs();
            input.SetSrcDir(segmentPath);
            input.SetDocCount(docsInSegment);
            input.SetIsFinalIndex(false);
        }

        NRtDoc::TBuilderTask::TBuilderOutput mergerOutput;
        ui32 prepSegmentNo = 0;
        for (ui32 mergerTaskNo = NumSegments; mergerTaskNo < NumSegments + NumMerges; ++mergerTaskNo) {
            Tasks.emplace_back();
            NRtDoc::TBuilderTask& task = Tasks.back();
            FillTaskConfig(task.MutableConfig());

            ui32 newDocIdBegin = 0;

            if (mergerOutput.HasTrgDir()) { //all tasks except first
                NRtDoc::TBuilderTask::TBuilderInput& prev = *task.AddInputs();
                prev.SetSrcDir(mergerOutput.GetTrgDir());
                prev.SetDocCount(mergerOutput.GetDocCountStat());
                prev.SetIsFinalIndex(true);

                const TFsPath mergerMap = WriteFakeMergerMap(workDir, mergerTaskNo, mergerTaskNo - 1, prev.GetDocCount(), newDocIdBegin);
                prev.SetSrcMapping(mergerMap);
            }

            {
                NRtDoc::TBuilderTask::TBuilderInput& cur = *task.AddInputs();
                cur.CopyFrom(AllStagesInputs.GetInputs(prepSegmentNo));
                const TFsPath mergerMap = WriteFakeMergerMap(workDir, mergerTaskNo, prepSegmentNo, cur.GetDocCount(), newDocIdBegin);
                cur.SetSrcMapping(mergerMap);
                ++prepSegmentNo;
            }

            {
                mergerOutput.SetTrgDir(MkDirTrg(workDir / Sprintf("index_%d", mergerTaskNo)));
                mergerOutput.SetTempDir(MkDirTemp(workDir / Sprintf("merger_temp_%d", mergerTaskNo)));
                mergerOutput.SetDocCountStat(newDocIdBegin);

                task.MutableOutput()->CopyFrom(mergerOutput);
            }

            OnTaskAdded(task);
        }
    }

    void TTestPartialRunTestBase::FillTaskConfig(NRtDoc::TBuilderConfig* taskConfig) const {
        taskConfig->SetDebugInfo(true);
        taskConfig->SetBundleVersion("99999"); //TODO(yrum): better be "HEAD" or something
        taskConfig->SetDisableShardsPrepare(true);
    }

    TString TTestPartialRunTestBase::GetFinalDir() const {
        Y_ENSURE(!Tasks.empty());
        return Tasks.back().GetOutput().GetTrgDir();
    }

    ui32 TTestPartialRunTestBase::GetFinalDocCount() const {
        Y_ENSURE(!Tasks.empty());
        return Tasks.back().GetOutput().GetDocCountStat();
    }

    void TTestPartialRunTestBase::Build(const NRtDoc::TBuilderTask& task) {
        NFusion::TAssertPolicy pol;
        pol.VerifyBundleDependencies_ = true; //has close to no effect
        NFusion::TExtBuilderTaskDebug t(task, &pol, MakeHolder<NFusion::TPartialBundleConfig>(BundleFilter));

        NRtDoc::TBuilderTaskResult mergeResult;
        bool success = t.Run(mergeResult);
        Y_ENSURE(success && mergeResult.GetErrorCode() == 0);
    }

    void TTestPartialRunTestBase::BuildAll() {
        for (ui32 i = 0; i < Tasks.size(); ++i) { // not foreach, to ease the debug
            const NRtDoc::TBuilderTask& task = Tasks[i];
            Build(task);
        }
    }

    void TTestPartialRunTestBase::Cleanup() {
        if (NFs::Exists(TestWorkDir)) {
            NFs::RemoveRecursive(TestWorkDir);
        }
    }

    TString TTestPartialRunTestBase::GetTestResource(const TFsPath& resource) {
        return TFsPath(GetWorkPath()) / resource;
    }
}
