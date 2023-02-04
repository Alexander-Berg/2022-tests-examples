#include "page_proto_helpers.h"


namespace NProfilePreprocessing {

    yabs::proto::TPageContext* TPageProtoBuilder::GetProfile() {
        return &Profile;
    }

    TString TPageProtoBuilder::GetDump() {
        TString protoString;
        Y_PROTOBUF_SUPPRESS_NODISCARD Profile.SerializeToString(&protoString);
        return protoString;
    }

    void TPageProtoBuilder::AddBaseFields(
        ui64 pageId, ui64 impId, ui64 sspId,
        ui64 pageDomainMd5, ui64 sppPageTokenTag,
        const TString& url, const TString& reReferer
    ) {
        Profile.SetPageID(pageId);
        Profile.SetImpID(impId);
        Profile.SetSSPID(sspId);
        Profile.SetPageDomainMD5(pageDomainMd5);
        Profile.SetSSPPageTokenTag(sppPageTokenTag);
        Profile.SetUrl(url);
        Profile.SetReReferer(reReferer);
    }

    void TPageProtoBuilder::AddUAFields(
        ui64 osNameId, ui64 browserEngineId, ui64 browserNameId,
        ui64 deviceVendorId, ui64 x64, ui64 browserBaseID,
        ui64 multiTouch, ui64 isTv, ui64 inAppBrowser,
        ui64 isMobile
    ) {
        auto ua = Profile.MutableUserAgentInfo();

        ua->SetUAOSNameID(osNameId);
        ua->SetUABrowserEngineID(browserEngineId);
        ua->SetUABrowserNameID(browserNameId);
        ua->SetUADeviceVendorID(deviceVendorId);
        ua->SetUAx64(x64);
        ua->SetUABrowserBaseID(browserBaseID);
        ua->SetUAMultiTouch(multiTouch);
        ua->SetUAIsTV(isTv);
        ua->SetUAInAppBrowser(inAppBrowser);
        ua->SetUAIsMobile(isMobile);
    }

    void TPageProtoBuilder::AddLayout(
        i64 block, i64 top, i64 left, 
        ui64 width, ui64 height,
        ui64 winWidth, ui64 winHeight,
        ui64 adNo, ui64 limit, ui64 visible,
        ui64 titleSize, ui64 titleBold
    ) {
        Profile.SetLayoutConfig(
            TStringBuilder{} << "{"
            << "\"block\": " << block << ","
            << "\"top\": " << top << ","
            << "\"left\": " << left << ","
            << "\"width\": " << width << ","
            << "\"height\": " << height << ","
            << "\"win_width\": " << winWidth << ","
            << "\"win_height\": " << winHeight << ","
            << "\"ad_no\": " << adNo << ","
            << "\"limit\": " << limit << ","
            << "\"visible\": " << visible << ","
            << "\"title_size\": " << titleSize << ","
            << "\"title_bold\": " << titleBold
            << "}"
        );
    }

    void TPageProtoBuilder::AddGrab(
        TString grab
    ) {
        Profile.SetGrab(grab);
    }

}
