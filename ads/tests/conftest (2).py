import json
import tempfile
import pytest


CONFIG = {
    "model_yt_dir": "//home/ads/users/alxmopo3ov/bsdev_78621/big_model_try1_debug_wlkckec",
    "ttl_filter": {
        "frequency": 30
    },
    "snapshotter": {
        "frequency": 7200
    },
    "model": {
        "loss": {
            "logistic": 1.0,
            "llmax": 4.0
        },
        "qce_logloss_coef": 0.1,
        "split_depth": 3,
        "nn": {
            "learning_rate": 0.0001,
            "delay_compensation": 0
        },
        "user": {
            "fields": [
                {
                    "dim": 70,
                    "name": "UserRegionID"
                },
                {
                    "dim": 70,
                    "name": "UserBestInterests"
                },
                {
                    "dim": 70,
                    "name": "UserLast10CatalogiaCategories"
                },
                {
                    "dim": 70,
                    "name": "UserCryptaAgeSegment"
                },
                {
                    "dim": 70,
                    "name": "UserCryptaGender"
                },
                {
                    "dim": 70,
                    "name": "UserCryptaIncome"
                },
                {
                    "dim": 70,
                    "name": "UserCryptaYandexLoyalty"
                },
                {
                    "dim": 70,
                    "name": "UserCryptaCommonSegments"
                },
                {
                    "dim": 70,
                    "name": "AffinitiveSites",
                    "features": [
                        "KryptaTopDomain",
                        "UserCryptaAffinitiveSitesIDs"
                    ]
                },
                {
                    "dim": 70,
                    "name": "UserCryptaInterests",
                    "features": [
                        "UserCryptaLongInterests",
                        "UserCryptaShortInterestsAll"
                    ]
                },
                {
                    "dim": 70,
                    "name": "UserTopCategories",
                    "features": [
                        "UserTop3BmCategoriesByInterests",
                        "UserTop1BmCategoriesByInterests",
                        "UserTop3BmCategoriesByUpdateTime",
                        "UserTop1BmCategoriesByUpdateTime",
                        "UserTop3BmCategoriesByEventTime",
                        "UserTop1BmCategoriesByEventTime"
                    ]
                }
            ],
            "realvalue": {
                "CountersAggregatedValues": 256,
                "VisitStatesFeatures": 7
            },
            "realvalue_norm_type": "rescale",
            "depth": 10,
            "width": 64,
            "split_depth": 3
        },
        "query": {
            "embedding_dim": 64,
            "attention_dim_feedforward": 64,
            "attention_dropout": 0,
            "attention_depth": 3,
            "attention_num_heads": 1,
            "features": {
                "Texts": [
                    "QueryTextsTokenLemma0",
                    "QueryTextsTokenLemma1",
                    "QueryTextsTokenLemma2",
                    "QueryTextsTokenLemma3",
                    "QueryTextsTokenLemma4",
                    "QueryTextsTokenLemma5",
                    "QueryTextsTokenLemma6",
                    "QueryTextsTokenLemma7",
                    "QueryTextsTokenLemma8",
                    "QueryTextsTokenLemma9",
                    "QueryTextsTokenLemma10",
                    "QueryTextsTokenLemma11",
                    "QueryTextsTokenLemma12",
                    "QueryTextsTokenLemma13",
                    "QueryTextsTokenLemma14",
                    "QueryTextsTokenLemma15",
                    "QueryTextsTokenLemma16",
                    "QueryTextsTokenLemma17",
                    "QueryTextsTokenLemma18",
                    "QueryTextsTokenLemma19"
                ],
                "SelectTypes": [
                    "QuerySelectTypes0",
                    "QuerySelectTypes1",
                    "QuerySelectTypes2",
                    "QuerySelectTypes3",
                    "QuerySelectTypes4",
                    "QuerySelectTypes5",
                    "QuerySelectTypes6",
                    "QuerySelectTypes7",
                    "QuerySelectTypes8",
                    "QuerySelectTypes9",
                    "QuerySelectTypes10",
                    "QuerySelectTypes11",
                    "QuerySelectTypes12",
                    "QuerySelectTypes13",
                    "QuerySelectTypes14",
                    "QuerySelectTypes15",
                    "QuerySelectTypes16",
                    "QuerySelectTypes17",
                    "QuerySelectTypes18",
                    "QuerySelectTypes19"
                ]
            }
        },
        "embedding": {
            "expiration_ttl": 336,
            "learning_rate": 0.0001,
            "delay_compensation": 0
        },
        "final_dim": 64,
        "banner": {
            "fields": [
                {
                    "dim": 70,
                    "name": "BannerID"
                },
                {
                    "dim": 70,
                    "name": "BannerBMCategoryID"
                },
                {
                    "dim": 70,
                    "name": "OrderID"
                },
                {
                    "dim": 70,
                    "name": "TargetDomainID"
                },
                {
                    "dim": 70,
                    "name": "BannerTexts",
                    "features": [
                        "BannerTitleTokenLemma",
                        "BannerTextTokenLemma",
                        "NewLandingPageTitleTokenLemma"
                    ]
                },
                {
                    "dim": 70,
                    "name": "Urls",
                    "features": [
                        "BannerURL",
                        "DirectURL"
                    ]
                }
            ],
            "depth": 10,
            "width": 64,
            "split_depth": 3
        },
        "page": {
            "fields": [
                {
                    "dim": 70,
                    "name": "PageID"
                },
                {
                    "dim": 70,
                    "name": "ImpID"
                },
                {
                    "dim": 70,
                    "name": [
                        "ImpID",
                        "PageID",
                    ]
                }
            ],
            "depth": 10,
            "width": 16,
            "split_depth": 3
        }
    },
    "evaluation": {
        "eval_confs": []
    },
    "train_data": {
        "batch_size": 1,
        "datetime_format": "%Y%m%d%H%M",
        "features": [
            "HitLogID",
            "LogID",
            "FraudBits",
            "BannerID",
            "BannerBMCategoryID",
            "OrderID",
            "TargetDomainID",
            "BannerTitleTokenLemma",
            "BannerTextTokenLemma",
            "NewLandingPageTitleTokenLemma",
            "BannerURL",
            "DirectURL",
            "PageID",
            "ImpID",
            [
                "ImpID",
                "PageID"
            ],
            "KryptaTopDomain",
            "UserRegionID",
            "UserBestInterests",
            "UserLast10CatalogiaCategories",
            "UserCryptaAgeSegment",
            "UserCryptaGender",
            "UserCryptaIncome",
            "UserCryptaYandexLoyalty",
            "UserCryptaAffinitiveSitesIDs",
            "UserCryptaLongInterests",
            "UserCryptaShortInterestsAll",
            "UserCryptaCommonSegments",
            "UserTop3BmCategoriesByInterests",
            "UserTop1BmCategoriesByInterests",
            "UserTop3BmCategoriesByUpdateTime",
            "UserTop1BmCategoriesByUpdateTime",
            "UserTop3BmCategoriesByEventTime",
            "UserTop1BmCategoriesByEventTime",
            "CountersAggregatedValues",
            "QueryFactors",
            "VisitStatesFeatures",
            "QueryTextsTokenLemma0",
            "QueryTextsTokenLemma1",
            "QueryTextsTokenLemma2",
            "QueryTextsTokenLemma3",
            "QueryTextsTokenLemma4",
            "QueryTextsTokenLemma5",
            "QueryTextsTokenLemma6",
            "QueryTextsTokenLemma7",
            "QueryTextsTokenLemma8",
            "QueryTextsTokenLemma9",
            "QueryTextsTokenLemma10",
            "QueryTextsTokenLemma11",
            "QueryTextsTokenLemma12",
            "QueryTextsTokenLemma13",
            "QueryTextsTokenLemma14",
            "QueryTextsTokenLemma15",
            "QueryTextsTokenLemma16",
            "QueryTextsTokenLemma17",
            "QueryTextsTokenLemma18",
            "QueryTextsTokenLemma19",
            "QuerySelectTypes0",
            "QuerySelectTypes1",
            "QuerySelectTypes2",
            "QuerySelectTypes3",
            "QuerySelectTypes4",
            "QuerySelectTypes5",
            "QuerySelectTypes6",
            "QuerySelectTypes7",
            "QuerySelectTypes8",
            "QuerySelectTypes9",
            "QuerySelectTypes10",
            "QuerySelectTypes11",
            "QuerySelectTypes12",
            "QuerySelectTypes13",
            "QuerySelectTypes14",
            "QuerySelectTypes15",
            "QuerySelectTypes16",
            "QuerySelectTypes17",
            "QuerySelectTypes18",
            "QuerySelectTypes19"
        ],
        "start_date": "202001272100",
        "end_date": "202006022000",
        "table": "//home/ads/users/alxmopo3ov/bigkv/train_data/2020-01-28_2020-06-05_preprocessed_torch_mb_4096",
        "targets": [
            "IsClick",
            "HitLogID"
        ]
    }
}


@pytest.fixture(scope="module")
def model_config():
    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt") as f:
            json.dump(CONFIG, f)
        yield tmp.name
