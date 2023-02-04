#pragma once

#include "hostname.h"
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/string/cast.h>

struct TBackendProxyConfig {
    TBackendProxyConfig()
        : Backend("rtyserver")
    {}

    struct TAddress {
        TAddress() : Host(TestsHostName()), Port(0) {}
        TString Host;
        TString UriPrefix;
        ui16 Port;
        TString GetString() const {
            return Host + ":" + ToString(Port) + UriPrefix;
        }
    };

    struct TIndexer : public TAddress {
        TIndexer()
            : Protocol("default")
            , PackSend(true)
        {}
        TString Protocol;
        bool PackSend;
    };
    ui32 FindController(const TString& host, ui16 port) const {
        for (ui32 i = 0; i < Controllers.size(); ++i)
            if (Controllers[i].Host == host && Controllers[i].Port == port)
                return i;
        return Max<ui32>();
    }
    TVector<TAddress> Controllers;
    TAddress Searcher;
    TAddress DeployManager;
    TAddress CommonProxy;
    TIndexer Indexer;
    TAddress Export;
    TMap<TString, TAddress> Emulators;
    TString Backend;
    bool HasSearchproxy = false;
    bool HasIndexerproxy = false;
    bool HasCommonProxy = false;
    bool ControllingDeployManager = true;
};
