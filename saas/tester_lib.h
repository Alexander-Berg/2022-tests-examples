#pragma once

#include <saas/rtyserver_jupi/library/extbuilder/bundle.h>
#include <saas/rtyserver_jupi/library/extbuilder/info.h>
#include <library/cpp/logger/global/global.h>
#include <util/folder/path.h>
#include <util/generic/set.h>
#include <util/string/split.h>

namespace NFusion {
    struct TPartialBundleConfig {
        bool Exclusive; // run only specified processors ("false" negates the filter)
        TSet<TString> DisplayNames;
    };

    class TExtBuilderTaskDebug: public TExtBuilderTask {
    private:
        THolder<TPartialBundleConfig> Filter;

    public:
        TExtBuilderTaskDebug(const NRtDoc::TBuilderTask& task, const TAssertPolicy* assertPolicy, THolder<TPartialBundleConfig>&& filter)
            : TExtBuilderTask(task, MakeIntrusive<NRtDoc::TStopHandle>(), assertPolicy, nullptr)
            , Filter(std::move(filter))
        {
        }

    protected:
        inline bool FilterMatch(const TString displayName) const {
            return Filter->DisplayNames.contains(displayName) ^ !Filter->Exclusive;
        }

        template <typename TCmd>
        void AddFiltered(TVector<TCmd>& dest, TVector<TCmd>& orig, const TString& kindDisplayName) {
            for (auto&& cmd : orig) {
                if (FilterMatch(cmd.DisplayName)) {
                    Cout << "Adding " << kindDisplayName << " to the partial bundle: " << cmd.DisplayName << Endl;
                    dest.emplace_back(std::move(cmd));
                }
            }
        }

        void AddFilteredSpec(THashSet<TString>& dest, THashSet<TString>& orig, const TString kindDisplayName) {
            for (auto&& cmd : orig) {
                if (FilterMatch(cmd)) {
                    Cout << "Adding " << kindDisplayName << " to the partial bundle: " << cmd << Endl;
                    dest.insert(cmd);
                }
            }
        }

        virtual THolder<TExtBuilderBundle> GetBundle(const TBundleVersion& version, const TString& modelsDir, const TString& targetDir) override {
            THolder<TExtBuilderBundle> fullBundle = TExtBuilderTask::GetBundle(version, modelsDir, targetDir);
            if (!Filter) {
                return fullBundle;
            }

            THolder<TExtBuilderBundle> result = MakeHolder<TExtBuilderBundle>();

            AddFiltered(result->PriorPrepCommands, fullBundle->PriorPrepCommands, "PriorPrepCommand");
            AddFiltered(result->PriorCommands, fullBundle->PriorCommands, "PriorCommand");
            AddFiltered(result->PrepCommands, fullBundle->PrepCommands, "PrepCommand");
            AddFiltered(result->Commands, fullBundle->Commands, "Command");
            AddFiltered(result->LateCommands, fullBundle->LateCommands, "LateCommand");
            AddFilteredSpec(result->SpecialCommands, fullBundle->SpecialCommands, "SpecialCommand");
            result->SavedPreparates = fullBundle->SavedPreparates;

            Y_ENSURE(!result->PrepCommands.empty() || !result->Commands.empty() || !result->SpecialCommands.empty(),
                     "No bundle items have passed the filter. Incorrect arguments?");
            return result;
        }

    public:
        static void MkDirs(const NRtDoc::TBuilderTask& task) {
            TSet<TString> inputDirs;
            for (size_t i = 0; i < task.InputsSize(); ++i) {
                const auto& inp = task.GetInputs(i);
                inputDirs.insert(inp.GetSrcDir());
                inputDirs.insert(TFsPath(inp.GetSrcMapping()).Parent());
            }

            TSet<TString> outputDirs;
            if (task.HasOutput()) {
                const auto& outp = task.GetOutput();
                outputDirs.insert(outp.GetTrgDir());
                outputDirs.insert(outp.GetTempDir());
            }

            for (const TString& src : inputDirs) {
                if (!src.empty()) {
                    Y_ENSURE(TFsPath(src).IsDirectory(), src << "is not a directory");
                }
            }

            for (const TString& trg : outputDirs) {
                if (!trg.empty()) {
                    TFsPath(trg).MkDirs();
                }
            }
        }
    };
}
