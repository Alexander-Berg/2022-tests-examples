#include <laas/lib/ip_properties/proto/ip_properties.pb.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>
#include <library/cpp/string_utils/base64/base64.h>

#include <util/stream/output.h>

#define Y_GXIP_OPTION(opts, name, arg) opts.AddLongOption(#name).RequiredArgument(arg).StoreResult(&name);
#define Y_GXIP_OPT_SET(pb, name) if (name) { pb.Set ## name(*name); }

namespace NLastGetopt::NPrivate {
    template <>
    inline TMaybe<TString> OptFromStringImpl<TMaybe<TString>>(const TStringBuf& value) {
        return TString(value);
    }

    template <>
    inline TMaybe<i32> OptFromStringImpl<TMaybe<i32>>(const TStringBuf& value) {
        return FromString<i32>(value);
    }

    template <>
    inline TMaybe<bool> OptFromStringImpl<TMaybe<bool>>(const TStringBuf& value) {
        return FromString<bool>(value);
    }
}

class TMain: public TMainClassArgs {
    bool Verbose_ = false;

    TMaybe<TString> GsmOperatorIfIpIsMobile;
    TMaybe<i32> CountryIdByIp;
    TMaybe<bool> IsAnonymousVpn;
    TMaybe<bool> IsPublicProxy;
    TMaybe<bool> IsTor;
    TMaybe<bool> IsHosting;
    TMaybe<bool> IsGdpr;
    TMaybe<bool> IsMobile;
    TMaybe<bool> IsYandexNet;
    TMaybe<bool> IsYandexStaff;
    TMaybe<bool> IsSerpTrustedNet;

protected:
    void RegisterOptions(NLastGetopt::TOpts& opts) override {
        opts.SetTitle("gen_is_gdpr_b -- generates is_gdpr_b cookie");
        opts.AddHelpOption('h');
        opts.AddCharOption('V').NoArgument().StoreValue(&Verbose_, true).Help("verbose");
        Y_GXIP_OPTION(opts, GsmOperatorIfIpIsMobile, "STR");
        Y_GXIP_OPTION(opts, CountryIdByIp, "INT");
        Y_GXIP_OPTION(opts, IsAnonymousVpn, "BOOL");
        Y_GXIP_OPTION(opts, IsPublicProxy, "BOOL");
        Y_GXIP_OPTION(opts, IsTor, "BOOL");
        Y_GXIP_OPTION(opts, IsHosting, "BOOL");
        Y_GXIP_OPTION(opts, IsGdpr, "BOOL");
        Y_GXIP_OPTION(opts, IsMobile, "BOOL");
        Y_GXIP_OPTION(opts, IsYandexNet, "BOOL");
        Y_GXIP_OPTION(opts, IsYandexStaff, "BOOL");
        Y_GXIP_OPTION(opts, IsSerpTrustedNet, "BOOL");

        opts.SetFreeArgsMax(0);
    }

    int DoRun(NLastGetopt::TOptsParseResult&&) override {
        NLaas::TIpProperties props;
        Y_GXIP_OPT_SET(props, GsmOperatorIfIpIsMobile);
        Y_GXIP_OPT_SET(props, CountryIdByIp);
        Y_GXIP_OPT_SET(props, IsAnonymousVpn);
        Y_GXIP_OPT_SET(props, IsPublicProxy);
        Y_GXIP_OPT_SET(props, IsTor);
        Y_GXIP_OPT_SET(props, IsHosting);
        Y_GXIP_OPT_SET(props, IsGdpr);
        Y_GXIP_OPT_SET(props, IsMobile);
        Y_GXIP_OPT_SET(props, IsYandexNet);
        Y_GXIP_OPT_SET(props, IsYandexStaff);
        Y_GXIP_OPT_SET(props, IsSerpTrustedNet);
        if (Verbose_) {
            Cout << props.ShortDebugString() << Endl;
        }
        Cout << Base64Encode(props.SerializeAsString()) << Endl;
        return 0;
    }
};

int main(int argc, const char** argv) {
    TMain().Run(argc, argv);
}
