#include "application.h"
#include <library/cpp/yconf/conf.h>

namespace {
    void LoadCommonCfg(TUpdater::TConfig::TCommon& config, const TYandexConfig::Directives& directives) {
        config.Host = directives.Value("Host", config.Host);
        config.Port = directives.Value("Port", config.Port);
    }

    void LoadTeCfg(TUpdater::TConfig::TTE& config, const TYandexConfig::Directives& directives) {
        LoadCommonCfg(config, directives);
        config.WeatherCount = directives.Value("WeatherCount", config.WeatherCount);
        config.RevisionsCount = directives.Value("RevisionsCount", config.RevisionsCount);
    }

    void LoadSvnCfg(TSvnInfo::TConfig& config, const TYandexConfig::Directives& directives) {
        config.UserName = directives.Value("Username", config.UserName);
        config.Password = directives.Value("Password", config.Password);
        config.Url = directives.Value("Url", config.Url);
        config.CertPath = directives.Value("CertPath", config.CertPath);
    }

    class TStringToDbType : public TMap<TCiString, TDbData::TType> {
    public:
        TStringToDbType() {
            insert(value_type("TE", TDbData::TEST_ENVIRONMENT));
            insert(value_type("Lunapark", TDbData::LUNAPARK));
            insert(value_type("Aqua", TDbData::AQUA));
            insert(value_type("Robot", TDbData::ROBOT));
        }
        TDbData::TType Get(const char* status) const {
            const_iterator i = find(status);
            if (i == end())
                return TDbData::TEST_ENVIRONMENT;
            else
                return i->second;
        }
    };
}

TTestModelApplication::TConfig::TConfig(const TServerConfigConstructorParams& params)
: Daemon(*params.Daemon)
{
    TAnyYandexConfig cfg;
    if (!cfg.ParseMemory(params.Text.data())) {
        TString mess = "errors in config: ";
        cfg.PrintErrors(mess);
        FAIL_LOG("%s", mess.data());
    }
    const TYandexConfig::Section* root = cfg.GetFirstChild("Server");
    VERIFY_WITH_LOG(root, "Must be Server section");
    TYandexConfig::TSectionsMap children = root->GetAllChildren();
    TYandexConfig::TSectionsMap::const_iterator i = children.find("Viewer");
    VERIFY_WITH_LOG(i != children.end(), "Must be section Viewer");
    Viewer = TDaemonConfig::ParseHttpServerOptions(i->second->GetDirectives(), "", "0.0.0.0");

    i = children.find("Updater");
    VERIFY_WITH_LOG(i != children.end(), "Must be section Updater");
    TYandexConfig::Section* updater = i->second;
    TYandexConfig::TSectionsMap updaters = updater->GetAllChildren();
    TYandexConfig::TSectionsMap::const_iterator iter = updaters.find("Sandbox");
    if (iter != updaters.end())
        LoadCommonCfg(Updater.Sandbox, iter->second->GetDirectives());

    iter = updaters.find("Lunapark");
    if (iter != updaters.end())
        LoadCommonCfg(Updater.Lunapark, iter->second->GetDirectives());

    iter = updaters.find("Aqua");
    if (iter != updaters.end())
        LoadCommonCfg(Updater.Aqua, iter->second->GetDirectives());

    iter = updaters.find("Robot");
    if (iter != updaters.end())
        LoadCommonCfg(Updater.Robot, iter->second->GetDirectives());

    iter = updaters.find("TE");
    if (iter != updaters.end())
        LoadTeCfg(Updater.TE, iter->second->GetDirectives());

    iter = updaters.find("Svn");
    if (iter != updaters.end())
        LoadSvnCfg(Updater.Svn, iter->second->GetDirectives());
    Singleton<TSvnInfo>()->SetConfig(Updater.Svn);

    iter = updaters.find("DBS");
    if (iter != updaters.end()) {
        TYandexConfig::TSectionsMap dbs = iter->second->GetAllChildren();
        for (std::pair<TYandexConfig::TSectionsMap::const_iterator, TYandexConfig::TSectionsMap::const_iterator> idbs = dbs.equal_range("DB"); idbs.first != idbs.second; ++idbs.first) {
            TString name = idbs.first->second->GetDirectives().Value("Name", TString());
            VERIFY_WITH_LOG(!!name, "DB must has name");
            TDbData::TType type = Singleton<TStringToDbType>()->Get(idbs.first->second->GetDirectives().Value("Type", TString()).data());
            Updater.DbList.push_back(TDbData());
            Updater.DbList.back().Name = name;
            Updater.DbList.back().Type = type;
        }
    }

    Updater.Period = TDuration::Seconds(updater->GetDirectives().Value("PeriodSec", Updater.Period.Seconds()));
    Updater.MainThreads = updater->GetDirectives().Value("MainThreads", 1);
    Updater.SecondaryThreads = updater->GetDirectives().Value("SecondaryThreads", 1);
}

const TDaemonConfig& TTestModelApplication::TConfig::GetDaemonConfig() const {
    return Daemon;
}

TTestModelApplication::TTestModelApplication(const TConfig& config)
: Config(config)
, Updater(Model, config.Updater)
, Viewer(Model, config.Viewer)
{
}

const TTestModelApplication::TConfig& TTestModelApplication::GetConfig() const {
    return Config;
}

void TTestModelApplication::Run() {
    if(!Viewer.Start()) {
        ERROR_LOG << "Cannot start ViewerServer, error: " << Viewer.GetErrorCode() << " (" << Viewer.GetError() << ")"<< Endl;
        return;
    }
    Updater.Start();
    Viewer.Wait();
}

void TTestModelApplication::Stop(ui32 /*rigidStopLevel*/, const TCgiParameters* /*cgiParams*/) {
    Viewer.Shutdown();
    Viewer.Wait();
    Updater.Stop();
}
