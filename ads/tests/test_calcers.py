import json
import logging
import tempfile

import pytest
import yatest.common

import yt.wrapper

import logos.libs.testy as testy


logger = logging.getLogger(__name__)


def get_binary(srcs, pools, dst, yt_server, calcers_config):
    cmd = [yatest.common.binary_path("ads/factor_check/features/calc_features/calc_features")]
    cmd.extend(['--yt-server', yt_server])
    for src in srcs:
        cmd.extend(['--src-tables', src])
    for src in pools:
        cmd.extend(['--pool-tables', src])
    cmd.extend(['--dst-table', dst])
    cmd.extend(['--dst-table-settings-erasure-codec', 'none'])
    cmd.extend(['--calcers-config', calcers_config])
    cmd.append('--full')
    cmd.append('--is-test')
    return cmd

BROAD_PHRASE_NORM = "//home/advquality/adv_machine/linear_models/BroadPhraseNorm.dict"
TEST_POOL_FOR_CAT = "//home/factor_check/tests/test_pool2"


@pytest.fixture(scope='module')
def ty(global_yt, local_yt):
    """
    > ya make -ttt . --test-param download --test-stderr --- only regenerated input tables
    > ya make -ttt . -Z --test-stderr --- generate output canonized table with the same input
    > ya make -ttt . -Z --test-param download --test-stderr --- generate output canonized table with regenerated input
    > ya make -ttt . -Z --test-param download=LogType1,LogType2 --test-stderr --- generate output canonized table with regenerated input of pointed log types
    """
    ty = testy.Testy.create_from_clients_yson(global_yt, local_yt)


    # TODO: normal generate input tables(prepare filtration inside code, don't use external tables)

    # how generate some input tables
    # https://yql.yandex-team.ru/Operations/X4WArBpqv9NOOJrFqRP-YWCgRHG1gM4Eu3xghwfJHpA=
    # https://yql.yandex-team.ru/Operations/X4XwOVPzVLwjavbAe7YYHKmLDVNFmqIuTZzFTyVcvwA=
    ty.prepare_dummy_environment([
            "//home/factor_check/levsky/calcer_tests/bs-watch-log",
            "//home/factor_check/levsky/calcer_tests/visit-v2-log",
            "//home/factor_check/levsky/calcer_tests/bs-chevent-log",
            "//home/factor_check/levsky/calcer_tests/bid-correction-pool",
            "//home/factor_check/levsky/tests/PositionalModel.sthash",
            "//home/factor_check/levsky/tests/MetrikaDomainMeans",
            "//home/factor_check/levsky/bsyeti_resources/keyword_info.json",
            "//home/factor_check/levsky/bsyeti_resources/counter_info.json",
            "//home/factor_check/kmekhovich/bigb-bases/uniword.vinyl",
            "//home/factor_check/levsky/bsyeti_resources/wordstat_hash.bin",
            "//home/factor_check/levsky/bsyeti_resources/page.json",
            "//home/factor_check/levsky/bsyeti_resources/banner_apps.vinyl",
            "//home/factor_check/levsky/bsyeti_resources/tag_info.json",
            "//home/factor_check/levsky/bsyeti_resources/sthash",
            "//home/factor_check/levsky/bsyeti_resources/active_select_types.json",
            "//home/factor_check/levsky/bsyeti_resources/domain_means.json",
            "//home/factor_check/kmekhovich/bigb-bases/goal_quality.vinyl",
            "//home/factor_check/levsky/nn_catalogia_log32_BSDEV65713.applier",
            "//home/factor_check/levsky/categories",
            "//home/yabs/dict/BMCategory",
            "//home/factor_check/levsky/calcer_tests/MeaningfulGoalsHistory",
            "//home/factor_check/levsky/rmp_banner_apps_info_1percent",
            TEST_POOL_FOR_CAT,
            BROAD_PHRASE_NORM,
        ],
        download=yatest.common.get_param("download"),
        check_sizes=False
    )
    return ty


@pytest.yield_fixture()
def calcers_config():
    with tempfile.NamedTemporaryFile(suffix='calcers_conf.json') as f:
        json.dump(
            {
                "QueryCalcers": [
                    {
                        "CategoricalNamespaces": [
                            "SearchQueryWords",
                            "SearchQueryPairs"
                        ],
                        "QueryCalcer": {}
                    }
                ],
                "UserCalcers": [
                    {
                        "CategoricalNamespaces": [
                            "STWords",
                            "Words",
                            "Goals",
                            "Socdem",
                            "Location",
                            "Categories",
                            "STCategories",
                            "InstalledSoft",
                            "CryptaSegments",
                            "DeviceFeatures",
                            "ClickedCategories",
                            "WeightedCategoryProfiles"
                        ],
                        "ProfileCalcer": {
                            "WordsMapFile": BROAD_PHRASE_NORM,
                        }
                    }
                ]
            },
            f
        )
        f.flush()
        yield f.name


@pytest.yield_fixture()
def counter_calcers_config():
    with tempfile.NamedTemporaryFile(suffix='calcers_conf.json') as f:
        json.dump(
            {
                "PreservePoolColumns": True,
                "UserCalcers": [
        {
            "ProfileCounters": {
                "Ids": [105,106,93,94,204,205, 136,137,138,139],
                "NamePrefix": "FC_Counter_",
                "KeywordInfoFile": "//home/factor_check/levsky/bsyeti_resources/keyword_info.json",
                "CounterInfoFile": "//home/factor_check/levsky/bsyeti_resources/counter_info.json",
                "UniwordFile": "//home/factor_check/kmekhovich/bigb-bases/uniword.vinyl",
                "WordStatFile": "//home/factor_check/levsky/bsyeti_resources/wordstat_hash.bin",
                "PagesFile": "//home/factor_check/levsky/bsyeti_resources/page.json",
                "TagFile": "//home/factor_check/levsky/bsyeti_resources/tag_info.json",
                "PositionalModelFile": "//home/factor_check/levsky/bsyeti_resources/sthash",
                "ActiveSelectTypeFile": "//home/factor_check/levsky/bsyeti_resources/active_select_types.json",
                "DomainMeansFile": "//home/factor_check/levsky/bsyeti_resources/domain_means.json",
                "GoalQualityFile": "//home/factor_check/kmekhovich/bigb-bases/goal_quality.vinyl",
                "BannerAppsFile": "//home/factor_check/levsky/bsyeti_resources/banner_apps.vinyl"
            }
        },
  {
        "NeuralWatchCategoryWrapperCalcer":  {
         "SourceType": "Title",
        "NeiroCatalogiaModel": "//home/factor_check/levsky/nn_catalogia_log32_BSDEV65713.applier",
        "NeiroCategories": "//home/factor_check/levsky/categories",
        "DomainMeansData": {
            "DomainMeansTable": "//home/factor_check/levsky/tests/MetrikaDomainMeans"
        },
        "InnerCalcers": [
        {
            "RealValueNamespaces": [
                "TitleCategory",
                "TitleCategoryAge"
             ],
        "WatchCategoryMetrikaCalcer": {
            "CounterCalcer": {
                "Keys": ["BMCategory1ID"],
                "DecayMultiplierOverWeek": 0.9,
                "Limit": 30,
                "MaxAgeDays": 30
            },
            "CountedValue": "TitleCategory",
            "WatchCategoriesLimit": 500,
            "WatchCategoriesMaxAgeHour": 1
     }
     }
     ]
   }
   },
                    {
                        "RealValueNamespaces": [
                            "UserAdd",
                            "UserAddAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Offer": {"CountedValue": "Add"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserDetail",
                            "UserDetailAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Offer": {"CountedValue": "Detail"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserPurchase",
                            "UserPurchaseAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Offer": {"CountedValue": "Purchase"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserClicks",
                            "UserClicksAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Event": {"UseClicks": True}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserShows",
                            "UserShowsAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Event": {"UseShows": True}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserEClicks",
                            "UserEClicksAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Event": {"UseShows": True, "CountEClicks": True, "PositionalModelSthashPath": "//home/factor_check/levsky/tests/PositionalModel.sthash"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserDomainShows",
                            "UserDomainShowsAge"
                        ],
                        "CounterCalcer": {
                            "Keys": ["DomainID"],
                            "Event": {"UseShows": True}
                        }
                    }, {
                        "RealValueNamespaces": [
                            "UserVisits",
                            "UserVisitsAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Visit": {}
                        }
                    }, {
                        "RealValueNamespaces": [
                            "UserDuration",
                            "UserDurationAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "CountedField": "Duration",
                            "Visit": {}
                        }
                    }, {
                        "RealValueNamespaces": [
                            "UserMetrikaNotBounces",
                            "UserMetrikaNotBouncesAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Visit": {"CountedValue": "NotBounce"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "Prices",
                            "PricesAge"
                        ],
                        "MeaningFullGoalsCalcer": {
                        "CounterCalcer": {
                            "Keys": [],
                            "DecayMultiplierOverWeek": 0.9,
                            "Limit": 1,
                            "MaxAgeDays": 14,
                            "Visit": {
                            }
                        },
                        "DomainMeansData": {
                            "DomainMeansTable": "//home/factor_check/levsky/tests/MetrikaDomainMeans"
                        },
                        "MeaningGoalsPath": "//home/factor_check/levsky/calcer_tests/MeaningfulGoalsHistory"
                        }
                    },
                   {
                            "RealValueNamespaces": [
                                "RMPClicks",
                                "RMPClicksAge"
                            ],
                            "CounterCalcer": {
                                "Keys": [],
                                "DecayMultiplierOverWeek": 0.8,
                                "Limit": 90,
                                "MaxAgeDays": 60,
                                "Event": {
                                    "BannerAppData": {
                                         "BanneAppInfoTable": "//home/factor_check/levsky/rmp_banner_apps_info_1percent"
                                    },
                                    "UseClicks": True,
                                    "UseOnlyRMP": True
                                }
                            }
                    },
                   {
                            "RealValueNamespaces": [
                                "RMPShows",
                                "RMPShowsAge"
                            ],
                            "CounterCalcer": {
                                "Keys": [],
                                "DecayMultiplierOverWeek": 0.8,
                                "Limit": 90,
                                "MaxAgeDays": 60,
                                "Event": {
                                    "BannerAppData": {
                                         "BanneAppInfoTable": "//home/factor_check/levsky/rmp_banner_apps_info_1percent"
                                    },
                                    "UseShows": True,
                                    "UseOnlyRMP": True
                                }
                            }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserDomainPrgg",
                            "UserDomainPrggAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Limit": 10,
                            "MaxAgeDays": 2,
                            "DecayMultiplierOverWeek": 0.8,
                            "Visit": {
                                "CountedValue": "Prgg",
                                "Threshold": 0.1,
                                "DomainMeansData": {
                                    "DomainMeansTable": "//home/factor_check/levsky/tests/MetrikaDomainMeans"
                                }
                            }
                        }
                     },
                    {
                        "RealValueNamespaces": [
                            "UserPrgg",
                            "UserPrggAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Visit": {"CountedValue": "Prgg"}
                        }
                    },
                    {
                        "RealValueNamespaces": [
                            "UserPrggThreshold",
                            "UserPrggThresholdAge"
                        ],
                        "CounterCalcer": {
                            "Keys": [],
                            "Visit": {"CountedValue": "Prgg", "Threshold": 0.01}
                        }
                    }
                ]
            },
            f
        )
        f.flush()
        yield f.name


def canonize_table(yt_client, table, filename):
    read_format = yt.wrapper.YsonFormat(attributes={"format": "text"})
    res = yt_client.read_table(table, format=read_format, raw=True).read()
    with open(filename, "w") as out:
        out.write(res)

    return yatest.common.canonical_file(
        filename,
        diff_tool=[
            yatest.common.binary_path("quality/ytlib/tools/canonical_tables_diff/bin/canonical_tables_diff"),
        ],
        diff_tool_timeout=600
    )

import os


@pytest.fixture
def calcer_files(yt_stuff):
    wrapper = yt_stuff.yt_wrapper
    d = './yt/files'
    for fname in os.listdir(d):
        logger.info('uploading file %s', fname)
        wrapper.smart_upload_file(os.path.join(d, fname), '//files/' + fname, placement_strategy="replace")
    return


@pytest.mark.usefixtures("ty")
@pytest.mark.parametrize("mode", ["download" if yatest.common.get_param("download") else "run"])
def test_counters(local_yt, counter_calcers_config, mode):  # , calcer_files, tables): yt_stuff
    if mode == "download":
        return

    yt_client = local_yt.get_client()
    dst = "//tmp/levsky/bc_dst2"
    logs, pool = ["//home/factor_check/levsky/calcer_tests/bs-watch-log", "//home/factor_check/levsky/calcer_tests/bs-chevent-log", "//home/factor_check/levsky/calcer_tests/visit-v2-log"], ["//home/factor_check/levsky/calcer_tests/bid-correction-pool"]
    cmd = get_binary(
        srcs=logs,
        pools=pool,
        dst=dst,
        yt_server=yt_client.config["proxy"]["url"],
        calcers_config=counter_calcers_config
    )
    logger.info('running %s', cmd)
    yatest.common.execute(cmd)
    return canonize_table(yt_client, dst, "bc_records.yson")


# @pytest.mark.usefixtures("ty")
# @pytest.mark.parametrize("mode", ["download" if yatest.common.get_param("download") else "run"])
# def test_calcers(local_yt, calcers_config, mode):
#     if mode == "download":
#         return
#     yt_client = local_yt.get_client()
#     dst = "//home/factor_check/mstebelev/test_pool.with_features"
#     cmd = get_binary(
#          srcs=[],
#          pools=[TEST_POOL_FOR_CAT],
#          dst=dst,
#          yt_server=yt_client.config["proxy"]["url"],
#          calcers_config=calcers_config
#      )
#      yatest.common.execute(cmd)
#      return canonize_table(yt_client, dst, "records.yson")
