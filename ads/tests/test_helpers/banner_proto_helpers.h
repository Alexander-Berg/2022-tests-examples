#pragma once

#include <ads/bigkv/preprocessor_primitives/base_preprocessor/base_preprocessor.h>

#include <yabs/proto/user_profile.pb.h>
#include <yabs/models_services/page_context/proto/page_context.pb.h>
#include <library/cpp/yson/node/node.h>
#include <library/cpp/testing/unittest/registar.h>


namespace NProfilePreprocessing {

    class TBannerProtoBuilder {
    public:
        TBannerProtoBuilder() = default;

        NCSR::TBannerProfileProto* GetProfile();
        TString GetDump();

        void AddBaseFields(
            ui64 bannerID, ui64 orderID, ui64 targetDomainID,
            TVector<ui64> bmCats,
            TString bannerTitle, TString bannerText, TString landingPageTitle,
            TString landingUrl, TString bannerUrl
        );

        void AddAppData(
            TString RegionName, TString LocaleName, TString BundleId, ui64 SourceID, 
            ui64 ContentAdvisoryRating, TVector<ui64> BMCategories, TVector<ui64> MobileInterests,
            TString Description, TString Title, TString MinOsVersion, TString VendorNameRaw, TString VendorWebsite,
            bool HasAds, bool HasPurchases, float PriceValueRUB, float Rating, ui64 SizeBytes, ui64 UsersCountByBigB, ui64 Votes,
            ui64 UpdateTimestamp
        );

    private:
        NCSR::TBannerProfileProto Profile;
    };

}
