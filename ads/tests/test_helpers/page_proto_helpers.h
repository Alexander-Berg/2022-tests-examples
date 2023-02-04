#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>

#include <yabs/models_services/page_context/proto/page_context.pb.h>
#include <library/cpp/yson/node/node.h>
#include <library/cpp/testing/unittest/registar.h>


namespace NProfilePreprocessing {

    class TPageProtoBuilder {
    public:
        TPageProtoBuilder() = default;

        yabs::proto::TPageContext* GetProfile();
        TString GetDump();

        void AddBaseFields(
            ui64 pageId, ui64 impId, ui64 sspId,
            ui64 pageDomainMd5, ui64 sppPageTokenTag,
            const TString& url, const TString& reReferer
        );

        void AddUAFields(
            ui64 osNameId, ui64 browserEngineId, ui64 browserNameId,
            ui64 deviceVendorId, ui64 x64, ui64 browserBaseID,
            ui64 multiTouch, ui64 isTv, ui64 inAppBrowser,
            ui64 isMobile
        );

        void AddLayout(
            i64 block, i64 top, i64 left, 
            ui64 width, ui64 height,
            ui64 winWidth, ui64 winHeight,
            ui64 adNo, ui64 limit, ui64 visible,
            ui64 titleSize, ui64 titleBold
        );

        void AddGrab(
            TString grab
        );

    private:
        yabs::proto::TPageContext Profile;
    };

}
