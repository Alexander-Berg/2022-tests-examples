#include "test_resource_util.h"

namespace NPyTorchTransportTests {
    TVector<TString> GetModelFeatureNames(const TString& modelName) {
        static THashMap<TString, TVector<TString>> modelFeatureNames({
            {"online_densenet",
                {"BannerID", "BannerBMCategoryID", "BMCategoryID", "BannerTitleLemmaH", "GroupBannerID", "OrderID",
                 "ProductType", "TargetDomainID", "BannerTextLemmaH", "LandingURLPathH", "LandingURLQueryH",
                 "LandingPageTitleLemmaH", "LandingURLNetLocPathH", "PageID", "ImpID", "ImpID,PageID", "PageToken",
                 "KryptaTopDomain", "QueryCategories", "QueryMostCommon3Categories", "UserCryptaAgeSegment",
                 "UserCryptaGender", "UserCryptaIncome", "UserRegionID", "UserBestInterests",
                 "UserLast10CatalogiaCategories", "UserTop3BmCategoriesByInterests", "UserTop3BmCategoriesByUpdateTime",
                 "UserTop3BmCategoriesByEventTime", "MaxCreationTimeQueryWords", "MaxCreationTimeQueryCategories",
                 "UserCryptaYandexLoyalty", "UserCryptaAffinitiveSitesIDs", "UserCryptaLongInterests",
                 "UserCryptaShortInterests", "UserCryptaCommonSegments", "UserTop1BmCategoriesByInterests",
                 "UserTop1BmCategoriesByUpdateTime", "UserTop1BmCategoriesByEventTime"}
            },
            {"user_densenet",
                {"KryptaTopDomain", "QueryCategories", "QueryMostCommon3Categories", "UserCryptaAgeSegment",
                 "UserCryptaGender", "UserCryptaIncome", "UserRegionID", "UserBestInterests",
                 "UserLast10CatalogiaCategories", "UserTop3BmCategoriesByInterests", "UserTop3BmCategoriesByUpdateTime",
                 "UserTop3BmCategoriesByEventTime", "MaxCreationTimeQueryWords", "MaxCreationTimeQueryCategories",
                 "UserCryptaYandexLoyalty", "UserCryptaAffinitiveSitesIDs", "UserCryptaLongInterests",
                 "UserCryptaShortInterests", "UserCryptaCommonSegments", "UserTop1BmCategoriesByInterests",
                 "UserTop1BmCategoriesByUpdateTime", "UserTop1BmCategoriesByEventTime"}
            },
            {"banner_densenet",
                {"BannerID", "BannerBMCategoryID", "BMCategoryID", "BannerTitleLemmaH", "GroupBannerID", "OrderID",
                 "ProductType", "TargetDomainID", "BannerTextLemmaH", "LandingURLPathH", "LandingURLQueryH",
                 "LandingPageTitleLemmaH", "LandingURLNetLocPathH"}
            },
            {"page_densenet",
                {
                    "ImpID", "ImpID,PageID", "PageID"
                }
            }
        });
        return modelFeatureNames.at(modelName);
    }
}
