import pytest


@pytest.fixture
def model_config():
    return {
        "namespaces": {
            "PageNamespaces": [
                "PageID",
                "ImpID",
                [
                    "ImpID",
                    "PageID"
                ]
            ],
            "UserNamespaces": [
                "KryptaTopDomain",
                "UserCryptaAgeSegment",
                "UserCryptaGender",
                "UserCryptaIncome",
                "UserRegionID",
                "UserBestInterests",
                "UserLast10CatalogiaCategories",
                "UserTop3BmCategoriesByInterests",
                "UserTop3BmCategoriesByUpdateTime",
                "UserTop3BmCategoriesByEventTime",
                "MaxCreationTimeQueryWords",
                "UserCryptaYandexLoyalty",
                "UserCryptaAffinitiveSitesIDs",
                "UserCryptaLongInterests",
                "UserCryptaShortInterestsAll",
                "UserCryptaCommonSegments",
                "UserTop1BmCategoriesByInterests",
                "UserTop1BmCategoriesByUpdateTime",
                "UserTop1BmCategoriesByEventTime",

                "UserTop5AppsByActiveMonthFrequency",
                "UserTop5AppsByLastActiveTime"
            ],
            "BannerNamespaces": [
                "BannerID",
                "BannerBMCategoryID",
                "BannerTitleLemmaH",
                "OrderID",
                "TargetDomainID",
                "BannerTextLemmaH",
                "LandingURLPathH",
                "LandingURLQueryH",
                "LandingPageTitleLemmaH",
                "LandingURLNetLocPathH",

                "BannerTextLemmaHBigrams",
                "BannerTitleLemmaHBigrams",
                "LandingPageTitleLemmaHBigrams"
            ]
        },

        "loss": "logistic",
        "verbose_progress": False,
        "embedding": {
            "type": "adam",
            "delay_compensation": False,
            "dim": 90,
            "kernel": "combined",
            "degree": 3,
            "gamma": 0.1,
            "coef0": 0.1,
            "layer_normalization": False,
            "params": {
                "lr": 0.001,
                "beta1": 0.9,
                "beta2": 0.999,
                "eps": 0.00000001,
                "ttl": 168,
                "delay_compensation": 0.0
            }
        },
        "residual_connection": True,
        "learn_projectors": True,
        "deep_part": {
            "use_layernorm": False,
            "use_batchnorm": False,
            "deep_dims": [
                900,
                900
            ],
            "output_dim": 50,
            "l2_normalization": True,
            "params": {
                "lr": 0.00007,
                "betas": [0.9, 0.999],
                "eps": 0.00000001,
                "amsgrad": True
            }
        }
    }
