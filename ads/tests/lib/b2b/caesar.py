import logging
import os

import yatest.common

from ads.bsyeti.big_rt.py_test_lib import BulliedProcess

from . import schema

log = logging.getLogger()

CAESAR_BINARY_PATH = "ads/bsyeti/caesar/bin/caesar_worker/caesar_worker"
# small timeout timeouts on slow servers
CAESAR_WAITING_TIMEOUT = 600
EXPIRATION_DAYS = 333


class CaesarProcess(BulliedProcess):
    def __init__(self, config_path, datetime):
        assert datetime
        env = os.environ.copy()
        env["Y_TEST_FIXED_TIME"] = datetime.isoformat()
        env["YT_LOG_LEVEL"] = "debug"
        log.info("launching caesar with env=%s", env)
        super(CaesarProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path(CAESAR_BINARY_PATH),
                "--config-json",
                config_path,
            ],
            env=env,
        )


def gen_config(port, workers):
    config = {
        "HttpServer": {"Port": port},
        "Logs": {
            "Rules": [
                {
                    "FilePath": os.path.join(yatest.common.output_path(), "caesar.log"),
                    "MinLevel": "Debug",
                }
            ]
        },
        "UseReplicasClient": True,
        "ReplicasRetryingConfig": {
            "RetryingExecutorConfig": {
                "Attempts": 10,
                "MinIntervalMs": 1000,
                "MaxIntervalMs": 60000,
                "SlowdownCoeff": 2,
            },
            "RetryAllErrors": True,
            "InnerRequestTimeoutMs": 5000,
        },
        "Workers": {},
        "ScatterFactoryConfig": {"RunnersPoolSize": 3},
        "DataReloaderConfig": {
            "MultikPackages": [
                {
                    "ModelID": 0,
                    "PackagePath": yatest.common.work_path("multik_package_folder/data"),
                },
                {
                    "ModelID": 1,
                    "PackagePath": yatest.common.work_path("multik_package_folder_exp/data"),
                },
            ],
            "BoughtNTimesBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "bought_n_times_v1.vinyl"),
            "BrowserRatingBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "browser_rating_v1.vinyl"),
            "CustomersChoiceBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "customers_choice_v1.vinyl"
            ),
            "DictVendorBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "dict_vendor_v1.vinyl"),
            "DomainMarketRatingBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "domain_market_rating_v1.vinyl"
            ),
            "ExternalDataExclusionBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "external_data_exclusion_v1.vinyl"
            ),
            "ModelCategoryBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "model_category_v1.vinyl"),
            "ModelRatingBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "model_rating_v1.vinyl"),
            "NearStationBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "nearest_station_v1.vinyl"),
            "OrgAspectsBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "orgs_aspects_v1.vinyl"),
            "OrgsRegDateBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "orgs_reg_date_v1.vinyl"),
            "PageInfoBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "page_info_v1.vinyl"),
            "RealtyBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "realty_v1.vinyl"),
            "SummaryReviewsBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "summary_reviews_v1.vinyl"),
            "TargetDomainBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "target_domain_v1.vinyl"),
            "ViewedNTimesBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "viewed_n_times_v1.vinyl"),
            "TaxHistoryBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "tax_history_v1.vinyl"),
            "HolidayBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "holiday_v1.vinyl"),
            "GreatHolidayBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "great_holiday_v1.vinyl"),
            "AutobudgetCoefBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "autobudget_coef_v1.vinyl"),
            "InterfaceTagBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "interface_tag_v1.vinyl"),
            "TurboShopIdToCounterIdFile": os.path.join(
                yatest.common.work_path("turbo_shop_id_2_counter_id"),
                "turbo_shop_id_2_counter_id.bin.gz",
            ),
            "AutobudgetBanSettingsBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "autobudget_ban_settings_v2.vinyl"
            ),
            "AutobudgetModelsBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "autobudget_models_v1.vinyl"
            ),
            "AutobudgetMultipliersBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "autobudget_multipliers_v1.vinyl"
            ),
            "GoodsRatingBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "goods_rating_v1.vinyl"),
            "SSPInfoBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "ssp_info_v1.vinyl"),
            "ForcedOrderTypeBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "forced_order_type_v1.vinyl"),
            "CurrencyDictsFolder": yatest.common.work_path("currency_dicts"),
            "NeedLockResources": False,
            "Geodata6File": yatest.common.work_path("geodata6.bin"),
            "CatEngineIdfsFolder": yatest.common.work_path("cat_engine_idfs"),
            "BigbAbExperimentsConfigFile": yatest.common.work_path("bigb_ab_production_config.json"),
            "AutobudgetDefaultConfigFile": os.path.join(
                yatest.common.work_path("bidder-config-package"), "resource/config/default.conf"
            ),
            "AutobudgetPreProdConfigFile": os.path.join(
                yatest.common.work_path("bidder-config-package"), "resource/config/preprod.conf"
            ),
            "AutobudgetExperimentConfigFile": os.path.join(
                yatest.common.work_path("bidder-config-package"), "resource/config/bidder_experiment_settings.conf"
            ),
            "AutobudgetExperimentPreProdConfigFile": os.path.join(
                yatest.common.work_path("bidder-config-package"), "resource/config/bidder_experiment_settings_preprod.conf"
            ),
            "HostClusterDictBaseFile": os.path.join(
                yatest.common.work_path("caesar-bases"), "host_cluster_dict_v1.vinyl"
            ),
            "DssmModelContentDataMarkupFile": yatest.common.work_path("web-model-dt-300.dssm"),
            "BrandSafetyConfigFile": os.path.join(yatest.common.work_path("brand_safety_web"), "thresholds.json"),
            "ContentMarkupDirectory": yatest.common.work_path("brand_safety_web"),
            "InventoryBookingsBaseFile": os.path.join(yatest.common.work_path("caesar-bases"), "inventory_bookings_v1.vinyl"),
        },
        "WaitDataReloaderAtStart": False,
        "ProcessingThreads": 1,
        "LogWriterConfig": {},
        "GeminiClientConfig": {
            "Enabled": True,
            "Mode": "LOCAL",
        },
    }
    if "OBJECT_API_ADDRESS_0" in os.environ and os.environ["OBJECT_API_ADDRESS_0"]:
        config["GrutSyncProcessorConfig"] = {
            "GrutClientConfig": {
                "Address": os.environ["OBJECT_API_ADDRESS_0"],
                "Secure": False
            },
            "TypeFilter": [
                {
                    "ObjectType": 7,  # campaign_v2
                    "EnabledPercent": 100,
                },
                # ad_groups will be added later
                # {
                #    "ObjectType": 8,  # ad_group_v2
                #    "EnabledPercent": 100,
                # },
            ],
        }
    for worker in workers:
        config["Workers"][worker.name] = {
            "StateTable": worker.state_table_path,
            "Suppliers": [],
            "ConsumingSystem": {
                "Cluster": worker.yt_cluster,
                "MainPath": "//tmp/test_consuming_system_{}".format(worker.name),
                "Shards": {"List": {"Values": [0]}},
                "MaxShards": 1,
                "MaxShardsToCheck": 1,
                "WorkerMinorName": worker.name,
                "EnableFakeBalancer": True,  # instead of heavy MasterBalancing
            },
            "StatefulShardProcessorConfig": {
                "ParsingThreads": 1,
                "MaxEpochsInFlight": 1,
            },
            "SubShards": 10,
            "MaxTasksInQueue": 20,
        }
        supplier = {
            "Alias": "{}.resharded_yt_log".format(worker.name),
            "YtSupplier": {
                "QueueConsumer": worker.consumer,
                "QueuePath": worker.input_queue,
                "Cluster": worker.yt_cluster,
                "CommitPeriodMs": 100,  # fast committing
                "ChunkSize": 10000,  # try read all in one chunk
                "MaxOutChunkSize": 10000,  # try to push to processing as one chunk
                "FlushChunkIntervalMs": 1,  # push data faster
                "DelayAfterEmptyReadMs": 10,  # read faster after data becomes available
            },
        }
        if worker.is_swift:
            supplier = {
                "Alias": supplier.pop("Alias"),
                "SwiftQueueSupplier": {"BaseYtQueue": supplier["YtSupplier"]},
            }
        config["Workers"][worker.name]["Suppliers"].append(supplier)

    # should it be in BannerGraphConfig?
    config["Workers"]["Banners"]["BannerGraphConfig"] = {
        "NormalizedUrlsTable": config["Workers"]["NormalizedUrls"]["StateTable"],
        "NormalizedHostsTable": config["Workers"]["NormalizedHosts"]["StateTable"],
        "OrdersTable": config["Workers"]["Orders"]["StateTable"],
        "AdGroupsTable": config["Workers"]["AdGroups"]["StateTable"],
        "GoalsTable": config["Workers"]["Goals"]["StateTable"],
        "EnableGoalsFetch": True,
        "TurboUrlDictTable": schema.TURBOURL_PATH,
        "MultikConfig": {"PeriodMinutes": 60, "MaxCategoriesNumber": 3},
        "UrlsLogWriterConfig": {
            "ClientKey": "samovar_feed_ext",
            "FeedName": "direct-new-ext",
            "PossibilityToWrite": 1,
        },
        "TsarModelServiceConfig": {
            "Url": "http://127.0.0.1:{0}/tsar".format(port),
            "EnableScatter": True,
            "AddressSuffix": "tsar",
            "ScatterClientConfig": {
                "SourceType": "SIMPLE",
                "SimpleSourceScript": "http://127.0.0.1:{0}".format(port),
                "SourceOptionsConfig": {
                    "TaskOptions": {"ConnectTimeouts": ["1s"], "SendingTimeouts": ["1s"]},
                    "TimeOut": "1s",
                    "AllowConnStat": False,
                },
                "InflightLimit": 3,
            },
        },
        "MultikModelServiceConfig": {"Enabled": False},
        "GeminiConfig": {
            "Enabled": True,
            "PossibilityToNormalize": 1,
        },
        "DynamicBannerModerationConfig": {
            "EnableRetry": False,
            "Enabled": False,
        },
        "SmartBannerModerationConfig": {
            "EnableRetry": False,
            "Enabled": False,
        },
        "CatMachineConfig": {
            "Enabled": True,
            "Profiles": [
                {
                    "Profile": "BannerSmoothing",
                    "MergeOpts": {"SmoothingCoefficient": 0.01},
                    "FilterOpts": {
                        "MaxIDF": 20,
                        "MaxItemsPerType": 256,
                        "AcceptTypes": [
                            "KryptaTopDomains",
                            "DayOfAWeek",
                            "HourOfDay",
                            "PageID",
                            "PageQtailID",
                            "HourOfWeek",
                        ],
                    },
                    "IdfsFilePath": "idfs.bin",
                }
            ],
        },
        "AvatarsMetaAutoUpdateConfig": {"Enabled": False, "UpdateProbability": 1},
        "MdsMetaConfig": {"ClearEnabled": True},
        "FetchCandidatsLogStatistic": False,
    }
    config["Workers"]["AdGroups"]["AdGroupGraphConfig"] = {
        "CatMachineConfig": {
            "Enabled": True,
            "Profiles": [
                {
                    "Profile": "GroupExportSmoothing",
                    "MergeOpts": {"SmoothingCoefficient": 0.01},
                    "FilterOpts": {
                        "MaxIDF": 20,
                        "MaxItemsPerType": 256,
                        "AcceptTypes": ["KryptaTopDomains", "PageID", "HourOfWeek"],
                    },
                    "IdfsFilePath": "idfs.bin",
                }
            ],
        }
    }
    config["Workers"]["Offers"]["OfferGraphConfig"] = {
        "NormalizedUrlsTable": config["Workers"]["NormalizedUrls"]["StateTable"],
        "MultikConfig": {
            "PeriodMinutes": 60,
            "MaxCategoriesNumber": 5,
        },
        "CounterOffersTable": config["Workers"]["CounterOffers"]["StateTable"],
        "SaasConfig": {
            "MessageConfig": {"MainProfile": {"ObjectType": "PN_Ecom#OT_TurboOffer"}},
            "Enabled": False,
        },
        "ShubertExpireThresholdHours": EXPIRATION_DAYS * 24,
        "UpdateListingsConfig": {
            "Table": schema.OFFER_LISTINGS_PATH,
        },
        "MultikModelServiceConfig": {"Enabled": False},
        "GeminiConfig": {
            "Enabled": True,
            "PossibilityToNormalize": 1,
        },
    }
    config["Workers"]["Orders"]["OrderGraphConfig"] = {
        "AutobudgetConfig": {
            "GoalsTable": config["Workers"]["Goals"]["StateTable"]
        }
    }
    config["Workers"]["MarketModels"]["MarketModelGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS},
        "SaasConfig": {"MessageConfig": {"MainProfile": {"ObjectType": "PN_Market#OT_Product"}}},
    }
    config["Workers"]["MarketCategories"]["MarketCategoryGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS}
    }
    config["Workers"]["MarketVendors"]["MarketVendorGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS}
    }
    config["Workers"]["MarketFeshes"]["MarketFeshGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS}
    }
    config["Workers"]["MarketReviews"]["MarketReviewGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS}
    }
    config["Workers"]["MarketVideoReviews"]["MarketVideoReviewGraphConfig"] = {
        "DjProfiles": {"ProfileExpirationDays": EXPIRATION_DAYS}
    }
    config["Workers"]["NormalizedHosts"]["HostGraphConfig"] = {"RobotFactorsExpireThresholdDays": EXPIRATION_DAYS}
    config["Workers"]["CounterOffers"]["CounterOfferGraphConfig"] = {"CountersExpireThresholdDays": EXPIRATION_DAYS}
    config["Workers"]["Goals"]["GoalGraphConfig"] = {
        "GoalsExpireThresholdDays": EXPIRATION_DAYS,
        "CountersTable": config["Workers"]["Counters"]["StateTable"],
    }
    config["Workers"]["Pages"]["PageGraphConfig"] = {
        "CatMachineConfig": {
            "Enabled": True,
            "Profiles": [
                {
                    "Profile": "PageSmoothing",
                    "MergeOpts": {"SmoothingCoefficient": 0.01},
                    "FilterOpts": {
                        "MaxIDF": 20,
                        "MaxItemsPerType": 256,
                        "AcceptTypes": ["KryptaTopDomains", "DayOfAWeek", "HourOfDay"],
                    },
                    "IdfsFilePath": "idfs.bin",
                }
            ],
        },
        "EnableCSPageClearing": False,
        "EnableProcessingOldCSPage": False,
    }
    config["Workers"]["Communications"]["CommunicationsGraphConfig"] = {
        "ChannelTable": schema.COMMUNICATIONS_CHANNEL_PATH,
    }
    return config
