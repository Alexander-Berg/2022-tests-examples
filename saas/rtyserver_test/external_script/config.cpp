#include "config.h"
#include <util/string/split.h>

namespace NRTYExternalScript {
    void TConfig::TRunShellConfig::Init(const TYandexConfig::Directives& directives) {
        if (!directives.GetValue("Command", Command))
            ythrow yexception() << "there is no Command";
        directives.GetValue("StopCommand", StopCommand);
        directives.GetValue("StdOutFile", StdOutFile);
        directives.GetValue("StdErrFile", StdErrFile);
        if (!directives.GetValue("WaitCommandOnStart", WaitCommandOnStart))
            WaitCommandOnStart = true;
        if (!directives.GetValue("WaitCommandOnStop", WaitCommandOnStop))
            WaitCommandOnStop = false;
    }

    void TConfig::TRunShellConfig::ToString(TStringStream& ss) const {
        ss << "<RunShell>" << Endl;
            ss << "Command:" << Command << Endl;
            ss << "StopCommand:" << StopCommand << Endl;
            ss << "StdOutFile: " << StdOutFile << Endl;
            ss << "StdErrFile: " << StdErrFile << Endl;
            ss << "WaitCommandOnStart: " << WaitCommandOnStart << Endl;
            ss << "WaitCommandOnStop: " << WaitCommandOnStop << Endl;
        ss << "</RunShell>" << Endl;
    }

    void TConfig::TControlConfig::Init(const TYandexConfig::Directives& directives) {
        if (!directives.GetValue("StartCommand", StartCommand))
            ythrow yexception() << "there is no StartCommand";
        if (!directives.GetValue("StopCommand", StopCommand))
            ythrow yexception() << "there is no StopCommand";
        if (!directives.GetValue("PingCommand", PingCommand))
            ythrow yexception() << "there is no PingCommand";
        if (!directives.GetValue("PingOkValue", PingOkValue))
            ythrow yexception() << "there is no PingOkValue";
    }

    void TConfig::TControlConfig::ToString(TStringStream& ss) const {
        ss << "<Control>" << Endl;
            ss << "StartCommand:" << StartCommand << Endl;
            ss << "StopCommand:" << StopCommand << Endl;
            ss << "PingCommand:" << PingCommand << Endl;
            ss << "PingOkValue:" << PingOkValue << Endl;
        ss << "</Control>" << Endl;
    }

    TConfig::TConfig(const TServerConfigConstructorParams& params)
        : Preprocessor(params.Preprocessor)
        , DaemonConfig(*params.Daemon)
    {
        TAnyYandexConfig yandexConfig;
        if (!yandexConfig.ParseMemory(params.Text.data())) {
            TString errorMessage;
            yandexConfig.PrintErrors(errorMessage);
            ERROR_LOG << "Cannot parse config. Errors: " << errorMessage << Endl;
        }

        TYandexConfig::Section* scriptSection = yandexConfig.GetFirstChild("Script");
        if (!scriptSection)
            ythrow yexception() << "There is no Script section in config";
        Init(*scriptSection);
    }

    void TConfig::Init(const TYandexConfig::Section& root) {
        Init(root.GetDirectives());
        TYandexConfig::TSectionsMap sections = root.GetAllChildren();
        TYandexConfig::TSectionsMap::const_iterator iter;
        switch (Type) {
        case NRTYExternalScript::TConfig::RUN_SHELL:
            iter = sections.find("RunShell");
            if (iter == sections.end())
                ythrow yexception() << Name << "RunShell section required";
            RunShellConfig.Init(iter->second->GetDirectives());
            break;
        case NRTYExternalScript::TConfig::CONTROL:
            iter = sections.find("Control");
            if (iter == sections.end())
                ythrow yexception() << Name << "Control section required";
            ControlConfig.Init(iter->second->GetDirectives());
            break;
        default:
            FAIL_LOG("unknown type: %i", (int)Type);
        }
    }

    void TConfig::Init(const TYandexConfig::Directives& directives) {
        directives.GetValue("Name", Name);
        TString type;
        if (!directives.GetValue("Type", type))
            ythrow yexception() << Name << ": there is no Type";
        if (type == "RUN_SHELL")
            Type = RUN_SHELL;
        else if (type == "CONTROL")
            Type = CONTROL;
        else
            ythrow yexception() << Name << ": unknown Type: " << type;
        TString files;
        directives.GetValue("FilesForPreprocess", files);
        FilesForPreprocess = StringSplitter(files).SplitBySet(", ").SkipEmpty();
    }

    TString TConfig::ToString() const {
        TStringStream ss;
        ss << DaemonConfig.ToString("DaemonConfig");
        ss << "<Script>" << Endl;
        ss << "Name: " << Name << Endl;
        ss << "FilesForPreprocess: " << JoinStrings(FilesForPreprocess, ",") << Endl;
        switch (Type) {
        case NRTYExternalScript::TConfig::RUN_SHELL:
            ss << "Type: SHELL_RUN" << Endl;
            RunShellConfig.ToString(ss);
            break;
        case NRTYExternalScript::TConfig::CONTROL:
            ss << "Type: CONTROL" << Endl;
            ControlConfig.ToString(ss);
            break;
        default:
            FAIL_LOG("unknown type: %i", (int)Type);
        }
        ss << "</Script>" << Endl;
        return ss.Str();
    }

}
