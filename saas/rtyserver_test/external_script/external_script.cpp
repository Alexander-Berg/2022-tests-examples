#include "external_script.h"
#include <library/cpp/logger/global/global.h>
#include <library/cpp/regex/pcre/regexp.h>
#include <util/system/shellcommand.h>
#include <util/stream/file.h>

namespace NRTYExternalScript {
    class TServer::IImpl {
    public:
        virtual ~IImpl() {}
        virtual void Start() = 0;
        virtual void Stop(ui32 rigidStopLevel, const TCgiParameters* cgiParams) = 0;
        virtual void WaitStarted() = 0;
        virtual void WaitFinished() = 0;
    protected:
        TString Exec(const TString command) {
            TShellCommand cmd(command);
            cmd.Run();
            switch (cmd.GetStatus()) {
            case TShellCommand::SHELL_FINISHED:
                break;
            case  TShellCommand::SHELL_ERROR:
                ythrow yexception() << command << " error: " << cmd.GetError();
            case  TShellCommand::SHELL_INTERNAL_ERROR:
                ythrow yexception() << command << " internal error: " << cmd.GetInternalError();
            default:
                FAIL_LOG("%s : invalid status %i", command.data(), (int)cmd.GetStatus());
            };
            return cmd.GetOutput();
        }
    };

    class TServer::TShellRunImpl : public TServer::IImpl {
    public:
        TShellRunImpl(const TConfig& config)
            : StopCommand(config.GetRunShellConfig().StopCommand)
            , WaitCommandOnStart(config.GetRunShellConfig().WaitCommandOnStart)
            , WaitCommandOnStop(config.GetRunShellConfig().WaitCommandOnStop)
        {
            TShellCommandOptions opts;
            opts.SetClearSignalMask(true);
            opts.SetCloseAllFdsOnExec(true);
            opts.SetAsync(true);
            opts.SetLatency(200);
            opts.SetDetachSession(true);

            IOutputStream* outputStream = BuildOutputStream(config.GetRunShellConfig().StdOutFile, "INFO_" + config.Name, TLOG_INFO);
            OutputStream.Reset(outputStream);
            opts.SetOutputStream(outputStream);
            IOutputStream* errorStream = BuildOutputStream(config.GetRunShellConfig().StdErrFile, "ERROR_" + config.Name, TLOG_ERR);
            ErrorStream.Reset(errorStream);
            opts.SetErrorStream(errorStream);

            NOTICE_LOG << "Shell command: " << config.GetRunShellConfig().Command << Endl;
            Command.Reset(new TShellCommand(config.GetRunShellConfig().Command, opts));
        }

        void Start() override {
            NOTICE_LOG << "Running main command " << Endl;
            Command->Run();
            NOTICE_LOG << "Main command started" << Endl;
        }

        void Stop(ui32 /*rigidStopLevel*/, const TCgiParameters* /*cgiParams*/) override {
            if (StopCommand){
                NOTICE_LOG << "Executing " << StopCommand << ": "<< Exec(StopCommand);
            }
            WaitFinished();
            Command->Terminate();
        }

        void WaitStarted() override {
            if (WaitCommandOnStart)
                WaitCommand();
        }

        void WaitFinished() override {
            if (WaitCommandOnStop)
                WaitCommand();
        }

        virtual void WaitCommand() {
            NOTICE_LOG << "Waiting main command to stop.." << Endl;
            Command->Wait();
            switch (Command->GetStatus()) {
                case TShellCommand::SHELL_ERROR:
                case TShellCommand::SHELL_INTERNAL_ERROR:
                    ythrow yexception() << "command not started";
                case TShellCommand::SHELL_FINISHED:
                break;
                default:
                    FAIL_LOG("Unexpected value %i", (int)Command->GetStatus());
            }
        }
    private:
        class TLogStream : public IOutputStream {
        public:
            TLogStream(ELogPriority priority, const TString& name)
                : Priority(priority)
                , Name(name)
            {}

        private:
            void DoWrite(const void* buf, size_t len) override {
                TStringBuf str((const char*)buf, len);
                for (size_t pos = str.find('\n'); pos != TStringBuf::npos; pos = str.find('\n')) {
                    SINGLETON_CHECKED_GENERIC_LOG(TGlobalLog, TRTYLogPreprocessor, Priority, Name.data()) << Buffer.Str() << str.substr(0, pos) << Endl;
                    Buffer.clear();
                    str = str.substr(pos + 1);
                }
                Buffer << str;
            }

            void DoFlush() override {
                if (Buffer.Empty())
                    return;
                SINGLETON_CHECKED_GENERIC_LOG(TGlobalLog, TRTYLogPreprocessor, Priority, Name.data()) << Buffer.Str() << Endl;
                Buffer.clear();
            }

            TStringStream Buffer;
            ELogPriority Priority;
            TString Name;
        };

        THolder<TShellCommand> Command;
        TString StopCommand;
        bool WaitCommandOnStart;
        bool WaitCommandOnStop;
        THolder<IOutputStream> ErrorStream;
        THolder<IOutputStream> OutputStream;
    private:
        IOutputStream* BuildOutputStream(const TString& outFile, const TString& defaultPrefix, ELogPriority defaultPrio){
            IOutputStream* outputStream;
            if (outFile != TString() && outFile != "NOTSET"){
                outputStream = new TUnbufferedFileOutput(outFile);
            } else {
                outputStream = new TLogStream(defaultPrio, defaultPrefix);
            }
            return outputStream;
        }
    };

    class TServer::TControlImpl : public TServer::IImpl{
    public:
        TControlImpl(const TConfig& config)
            : Config(config)
        {}

        void Start() override {
            INFO_LOG << Config.Name << ": " << Config.GetControlConfig().StartCommand << " - " << Exec(Config.GetControlConfig().StartCommand) << Endl;
        }

        void Stop(ui32 /*rigidStopLevel*/, const TCgiParameters* /*cgiParams*/) override {
            INFO_LOG << Config.Name << ": " << Config.GetControlConfig().StopCommand << " - " << Exec(Config.GetControlConfig().StopCommand) << Endl;
        }

        void WaitStarted() override {
            TRegExMatch match(Config.GetControlConfig().PingOkValue.data());
            while (!match.Match(Exec(Config.GetControlConfig().PingCommand).data()))
                Sleep(TDuration::MilliSeconds(500));
        }

        void WaitFinished() override {}
    private:
        const TConfig& Config;
    };

    TServer::TServer(const TConfig& config)
        : Config(config)
    {
        switch (Config.Type)
        {
        case TConfig::CONTROL:
            Impl.Reset(new TControlImpl(config));
            break;
        case TConfig::RUN_SHELL:
            Impl.Reset(new TShellRunImpl(config));
            break;
        default:
            FAIL_LOG("unknown type: %i", (int)Config.Type);
        }
    }

    TServer::~TServer() {
    }

    void TServer::Stop(ui32 rigidStopLevel /*= 0*/, const TCgiParameters* cgiParams /*= NULL*/) {
        Impl->Stop(rigidStopLevel, cgiParams);
    }

    void TServer::Run() {
        if (!!Config.Preprocessor && !Config.FilesForPreprocess.empty())
            for (const auto& file : Config.FilesForPreprocess) {
                TString cfg = Config.Preprocessor->ReadAndProcess(file);
                TFixedBufferFileOutput fo(file);
                fo << cfg;
            }
        Impl->Start();
        Impl->WaitStarted();
    }
}
