from ads_pytorch.nn.module.base_embedding_model import (
    EmbeddingDescriptor,
    EmbeddingComputeDescriptor,
    read_embedding_descriptors,
    build_embedding_optimizers,
    build_embedding_model
)


###################################################################
#                          FACTORIES TESTING                      #
###################################################################


CONFIG = {
    "default_algo": "rmsprop_norm",
    "default_algo_params": {
        "lr": 0.02,
        "ttl": 336,
        "old_style_update": False
    },
    "features": [
        {
            "dim": 64,
            "name": "UserRegionID"
        },
        {
            "dim": 64,
            "name": "UserBestInterests",
            "algo_params": {
                "ttl": 712
            }
        },
        {
            "dim": 64,
            "name": "AffinitiveSites",
            "features": [
                "KryptaTopDomain",
                "UserCryptaAffinitiveSitesIDs"
            ]
        },
        {
            "dim": 64,
            "name": "QueryTexts",
            "features": [
                "BigBQueryTexts",
                "QueryHistoryTexts",
                "SearchQueryTextTokenLemma"
            ],
            "add_prob": 0.1
        },
        {
            "dim": 64,
            "name": "SearchQueryRegion",
            "features": [
                {
                    "name": "SearchQueryRegion",
                    "add_prob": 0.3
                },
                "QueryHistoryRegions"
            ],
            "add_prob": 0.2
        },
        {
            "dim": 64,
            "name": "Url",
            "algo": "adam",
            "algo_params": {
                "ttl": 712,
                "lr": 0.003
                # here we must check that no old_style_check used
            }
        },
        {
            "dim": 73,
            "name": "BannerID",
            "algo_params": {
                "lr": 0.0001
            }
        },
    ]
}


def test_read_embedding_descriptors():
    REFERENCE = [
        EmbeddingDescriptor(
            name="UserRegionID",
            dim=64,
            algo_type="rmsprop_norm",
            features=["UserRegionID"]
        ),
        EmbeddingDescriptor(
            name="UserBestInterests",
            dim=64,
            algo_type="rmsprop_norm",
            features=["UserBestInterests"]
        ),
        EmbeddingDescriptor(
            name="AffinitiveSites",
            dim=64,
            algo_type="rmsprop_norm",
            features=[
                "KryptaTopDomain",
                "UserCryptaAffinitiveSitesIDs"
            ]
        ),
        EmbeddingDescriptor(
            name="QueryTexts",
            dim=64,
            algo_type="rmsprop_norm",
            features=[
                EmbeddingComputeDescriptor(feature="BigBQueryTexts", add_prob=0.1),
                EmbeddingComputeDescriptor(feature="QueryHistoryTexts", add_prob=0.1),
                EmbeddingComputeDescriptor(feature="SearchQueryTextTokenLemma", add_prob=0.1)
            ]
        ),
        EmbeddingDescriptor(
            name="SearchQueryRegion",
            dim=64,
            algo_type="rmsprop_norm",
            features=[
                EmbeddingComputeDescriptor(feature="SearchQueryRegion", add_prob=0.3),
                EmbeddingComputeDescriptor(feature="QueryHistoryRegions", add_prob=0.2)
            ]
        ),
        EmbeddingDescriptor(
            name="Url",
            dim=64,
            algo_type="adam",
            features=["Url"]
        ),
        EmbeddingDescriptor(
            name="BannerID",
            dim=73,
            algo_type="rmsprop_norm",
            features=["BannerID"]
        )
    ]
    res = read_embedding_descriptors(config=CONFIG)
    assert res == REFERENCE


def test_build_optimizers():
    full_cfg = {
        "embeddings": CONFIG,
        "external_factors": [
            "x1", "x2"
        ]
    }
    embedding_model = build_embedding_model(features_config=full_cfg)
    # FIXME no tests on real optim's values.
    optimizers = build_embedding_optimizers(
        embedding_model=embedding_model,
        features_config=full_cfg,
        verbose=True
    )
