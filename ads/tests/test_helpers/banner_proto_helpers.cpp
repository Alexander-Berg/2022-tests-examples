#include "banner_proto_helpers.h"


namespace NProfilePreprocessing {
    NCSR::TBannerProfileProto* TBannerProtoBuilder::GetProfile() {
        return &Profile;
    }

    TString TBannerProtoBuilder::GetDump() {
        TString protoString;
        Y_PROTOBUF_SUPPRESS_NODISCARD Profile.SerializeToString(&protoString);
        return protoString;
    }

    void TBannerProtoBuilder::AddBaseFields(
        ui64 bannerID, ui64 orderID, ui64 targetDomainID,
        TVector<ui64> bmCats,
        TString bannerTitle, TString bannerText, TString landingPageTitle,
        TString landingUrl, TString bannerUrl
    ) {
        Profile.SetBannerID(bannerID);
        Profile.SetOrderID(orderID);

        NCSR::TBannerProfileProto::TResources resource;
        resource.SetTargetDomainID(targetDomainID);
        resource.SetTitle(bannerTitle);
        resource.SetBody(bannerText);
        resource.SetHref(bannerUrl);

        NCSR::TBannerProfileProto::TResources::TUrl url;
        url.SetLandingTitle(landingPageTitle);
        url.SetLandingUrl(landingUrl);
        resource.MutableUrl()->CopyFrom(url);

        auto* multiks = resource.MutableMultiks()->Add();
        for (auto x: bmCats) {
            multiks->AddCategories(x);
        }

        Profile.MutableResources()->CopyFrom(resource);
    }

    void TBannerProtoBuilder::AddAppData(
        TString regionName, TString localeName, TString bundleId, ui64 sourceID,
        ui64 contentAdvisoryRating, TVector<ui64> BMCategories, TVector<ui64> mobileInterests,
        TString description, TString title, TString minOsVersion, TString vendorNameRaw, TString vendorWebsite,
        bool hasAds, bool hasPurchases, float priceValueRUB, float rating, ui64 sizeBytes, ui64 usersCountByBigB, ui64 votes,
        ui64 updateTimestamp
    ) {
        NCSR::TBannerProfileProto::TResources resource;

        NCSR::TBannerProfileProto::TResources::TAppData appData;
        appData.SetRegionName(regionName);
        appData.SetLocaleName(localeName);
        appData.SetBundleId(bundleId);
        appData.SetSourceID(sourceID);
        resource.MutableAppData()->CopyFrom(appData);

        NCSR::TBannerProfileProto::TResources::TMobileAppDataContainer mobileAppDataContainer;
        mobileAppDataContainer.SetUpdateTimestamp(updateTimestamp);
        NCSR::TMobileAppData mobileData;
        mobileData.SetContentAdvisoryRating(contentAdvisoryRating);
        mobileData.SetDescription(description);
        mobileData.SetTitle(title);
        mobileData.SetHasAds(hasAds);
        mobileData.SetMinOsVersion(minOsVersion);
        mobileData.SetPriceValueRUB(priceValueRUB);
        mobileData.SetRating(rating);
        mobileData.SetSizeBytes(sizeBytes);
        mobileData.SetUsersCountByBigB(usersCountByBigB);
        mobileData.SetVendorNameRaw(vendorNameRaw);
        mobileData.SetVersion("");
        mobileData.SetVotes(votes);
        mobileData.SetHasPurchases(hasPurchases);
        mobileData.SetVendorWebsite(vendorWebsite);
        mobileData.SetHasPurchases(hasPurchases);
        for (auto x: BMCategories) {
            mobileData.AddBMCategories(x);
        }
        for (auto x: mobileInterests) {
            mobileData.AddMobileInterests(x);
        }
        mobileAppDataContainer.MutableAppData()->CopyFrom(mobileData);
        resource.MutableMobileAppData()->CopyFrom(mobileAppDataContainer);

        Profile.MutableResources()->CopyFrom(resource);
    }

}
