from __future__ import print_function

import logging
import os
import signal
import sys
import time
from contextlib import contextmanager

import requests
import ujson
from ads.bsyeti.tests.test_lib.data_collector.config import ALL_YT_TABLES_CONFIG
from library.python.sanitizers import asan_is_on, msan_is_on
from yatest.common import binary_path, execute, gdb_path, work_path

from ads.bsyeti.samogon.balancer.balancer import gen_rr_balancer_conf


def get_wait_time_seconds():
    if asan_is_on() or msan_is_on():
        return 600
    else:
        return 300


def dump_cpuinfo():
    with open("/proc/cpuinfo") as fd:
        cpuinfo = fd.read()

    logging.info(cpuinfo)
    with open("cpuinfo", "w") as fd:
        fd.write(cpuinfo)


def kill_process(proc, sig=None):
    if proc is None:
        logging.info("no proc to kill")
        return
    if proc.running:
        try:
            if sig is None:
                proc.kill()
            else:
                os.kill(proc.process.pid, sig)
        except Exception as e:
            logging.error("Failed to kill process %s: %s", proc.process.pid, e)

    try:
        proc.wait(check_exit_code=False)
        logging.info("process %s finished with exit code %d", proc.process.pid, proc.exit_code)
    except Exception as e:
        logging.error("failed to wait processs %s: %s", proc.process.pid, e)


def wait_eagle_started(port, proc=None):
    resp = None
    wait_until = time.time() + get_wait_time_seconds()
    while time.time() < wait_until:
        time.sleep(1)
        try:
            resp = requests.get("http://localhost:{port}/status".format(port=port))
        except requests.ConnectionError:
            continue

        if resp is not None and resp.ok:
            break
    else:
        raise RuntimeError("failed to ping eagle")

    if resp is None or not resp.ok:
        if proc:
            logging.info("Checking pid still exists")
            if not proc.running:
                proc.wait(check_exit_code=False)
                logging.info("Backtrace of eagle proc: %s", proc.backtrace)
                logging.info("There is coredumps: %s", proc.verify_no_coredumps())
                assert False, "Eagle process exited with code {0}!".format(proc.exit_code)

            pid = proc.process.pid
            logging.info("Preparing to run gdb with current eagle process: %d", pid)
            cmd = [
                gdb_path(),
                "-p",
                str(pid),
                "--eval-command",
                "set print thread-events off",
                "--eval-command",
                "thread apply all backtrace full",
                "--batch",
                "--quiet",
            ]
            logging.error("Backtrace:\n%s", execute(cmd, check_exit_code=False).std_out)
            kill_process(proc, signal.SIGQUIT)
            assert (
                False
            ), "Eagle is stuck, the tests will not run (check stderr for gdb output)"  # if you see this error, test is not stable
        assert (
            False
        ), "Eagle still not ready for requests after many retries, the tests will not run"  # if you see this error, test is not stable


def get_tsar_models_configs():
    return {
        "JamshidDssmModel": {
            "VectorIds": [1, 3],
            "Target": [0],
            "DssmModel": {
                "OutputVariables": ["query_embedding#proj_ctr", "query_embedding#proj_bc"],
                "ModelPath": "./jamshid_dssm_pack/jruziev_dssmtsar2_ctr_bc_50_2l.appl.query",
            },
        },
        "TsarDiFactoCompressedUserModel": {
            "VectorIds": [2],
            "DmlcModel": {"ModelPath": "./tsar_difacto_models/robdrynkin_bsdev_75076_prod_v3_vinyl_uint16"},
            "Target": [0],
            "NeedLockResources": True,
        },
        "PytorchTsarModel": {
            "VectorIds": [4],
            "TorchV1Model": {"ModelPath": "./pytorch_tsar_models/UserNamespaces_3"},
            "Target": [0],
        },
        "FuturePrGGDssmModel": {
            "VectorIds": [6],
            "Target": [0, 1],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./future_prgg_pack/future_prgg_v2.appl.query",
            },
        },
        "ABDssmModel": {
            "VectorIds": [12],
            "Target": [0, 1],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./future_prgg_pack/ab_tsar.appl.query",
            },
        },
        "AlbFreshDssmModel": {
            "VectorIds": [13],
            "Target": [0],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./alb_fresh_dssm_pack/alb_fresh.appl.query",
            },
        },
        "BasylBCRsyaDssmModel": {
            "VectorIds": [16],
            "Target": [0],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./basyl_dssm_pack/model_rsya_apply",
            },
        },
        "TorchV2ModelFloat16": {
            "VectorIds": [26],
            "TorchV2Model": {"ModelPath": "./torch_v2_model_float16/UserNamespaces_3"},
            "Target": [0],
        },
        "JamshidSearchDssmModel": {
            "VectorIds": [5],
            "Target": [1],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./jamshid_dssm_pack/jruziev_tsar_searchBC1.appl.query",
            },
        },
        "OrganicConvSearchDssmModel": {
            "VectorIds": [14],
            "Target": [1],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./tsar_organic_conv_search/tsar_mixagol_search_bc_organic_conv",
            },
        },
        "QueryBannerDssmModel": {
            "VectorIds": [11],
            "Target": [1],
            "DssmModel": {
                "OutputVariables": ["query_embedding"],
                "ModelPath": "./query_banner_dssm/assessor.appl.scale_before_dot",
            },
        },
    }


@contextmanager
def launch_eagle(
    port,
    apphost_port=81,
    kv_saas_port=None,
    market_dj_port=None,
    yt_cluster=None,
    env=None,
    filter_dssm=None,
    enable_profiles_loader=True,
    enable_kv_saas=False,
    enable_market_dj=False,
    remote_profiles=False
):
    print("Current working directory: ", os.getcwd(), file=sys.stderr)
    proc = None
    try:
        passed_env = {"OMP_NUM_THREADS": "1", "YT_USER": "root", "YT_TOKEN": "yt_token"}

        data_config = {
            "NeedLockResources": False,
            "ClientConfigFile": work_path("clients_generated.pb"),
            "EagleDynamicConfigFile": work_path("eagle.cnf"),
            "CryptaShortInterestsFile": "./bsyeti-configs/crypta_short_interests.json",
            "GeobaseFile": "./geodata6.bin",
            "NeedLockGeobaseMemory": False,
            "LemmerCacheSize": 8192,
            "BmCategoryFile": "./bsyeti-bases/bm_category_data.json",
            "AbExperimentsLongConfig": "./bigb_ab_production_config.json",
            "LinearDumpPath": "./bsyeti-bases/lm_dumps",
            "MxModelsFolder": "./bsyeti-bases/mx-models/",
            "MatrixnetDumpsFolder": "./bsyeti-bases/mx_dumps/",
            "WordStatFile": "./bsyeti-bases/wordstat_hash.bin",
            "BayesianMediationModelPriorFile": "./bsyeti-bases/mediation_prior.json",
            "CodecFolder": "./dicts_cache/",
            "CounterIdfBaseFile": "./bsyeti-counters-idf-base/idf_base.bin",
            "CryptaRtSocdemModelsFolder": "./rt_socdem/",
            "CryptaLookalikeParentToLalMapping": "./mapping.json",
            "AudienceSegmentsPriorities": "./priorities.vinyl",
            "TsarModels": {},
            "MobileAppCategoriesBaseFile": "./mobile_app_categories/mobile_app_categories_v1.vinyl",
            "CryptaPrismRTModel": "./prism_rt_model/",
            "CryptaKeywordsToDmpMappingFile": "./keyword_to_dmp_mapping_v1.vinyl",
        }

        tsar_models_config = get_tsar_models_configs()

        def add_dssm(name):
            if filter_dssm is None or name in filter_dssm:
                data_config["TsarModels"][name] = tsar_models_config[name]

        add_dssm("JamshidDssmModel")
        add_dssm("JamshidSearchDssmModel")
        add_dssm("FuturePrGGDssmModel")
        add_dssm("ABDssmModel")
        add_dssm("TsarDiFactoCompressedUserModel")
        add_dssm("PytorchTsarModel")
        add_dssm("QueryBannerDssmModel")
        add_dssm("OrganicConvSearchDssmModel")
        add_dssm("AlbFreshDssmModel")
        add_dssm("BasylBCRsyaDssmModel")
        add_dssm("TorchV2ModelFloat16")

        flat_tsar_models_config = [data_config["TsarModels"][x] for x in sorted(data_config["TsarModels"])]
        data_config["TsarModels"] = flat_tsar_models_config

        conf = {
            "DataReloaderConfig": data_config,
            "LogWriter": {},
            "AppHostConfig": {"TvmEnabled": False, "Port": apphost_port},
            "GlobalLog": "cerr",
            "AccessLog": "cerr",
            "ServiceThreads": 8,
            "HeavyWorkerThreads": 4,
            "ApphostGrpcThreads": 0,
            "IsTvmRequired": False,
            "Port": port,
            "VerboseLevel": 8,
            "BackendsPath": "",
            "VectorCacheConfig": {"MaxCacheSize": 0, "MaxCacheDuration": 0},
            "SoftInflightLimit": 45,
            "HardInflightLimit": 55,
            "MinerConfig": {
                "Timeout": 3000,
                "ProfileLoaderConfig": {
                    "Enabled": enable_profiles_loader,
                    "Table": ALL_YT_TABLES_CONFIG["RemoteTestProfiles"][0] if remote_profiles else ALL_YT_TABLES_CONFIG["Profiles"][0],
                    "CacheConfig": {
                        "Size": 4 * 10**9,
                        "Lifetime": 5,
                    },
                    "Timeout": 1000,
                    "HedgingClientConfig": {
                        "Clients": [{"ClientConfig": {"ClusterName": yt_cluster}, "InitialPenalty": 0}],
                    },
                },
                "VultureLoaderConfig": {
                    "Table": ALL_YT_TABLES_CONFIG["VultureCrypta"][0],
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 600,
                    },
                    "Timeout": 1000,
                    # Keep VultureCrypta for simplicity, not used in ft-tests.
                    "ExperimentalTable": ALL_YT_TABLES_CONFIG["VultureCrypta"][0],
                    "HedgingClientConfig": {
                        "Clients": [{"ClientConfig": {"ClusterName": yt_cluster}, "InitialPenalty": 0}],
                    },
                },
                "UserShowsLoaderConfig": {
                    "Table": ALL_YT_TABLES_CONFIG["UserShows"][0],
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 2,
                    },
                    "Timeout": 1000,
                    "HedgingClientConfig": {
                        "Clients": [{"ClientConfig": {"ClusterName": yt_cluster}, "InitialPenalty": 0}],
                    },
                },
                "CookiesLoaderConfig": {
                    "Table": ALL_YT_TABLES_CONFIG["Cookies"][0],
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 2,
                    },
                    "Timeout": 1000,
                    "HedgingClientConfig": {
                        "Clients": [{"ClientConfig": {"ClusterName": yt_cluster}, "InitialPenalty": 0}],
                    },
                },
                "SearchPersLoaderConfig": {
                    "Table": ALL_YT_TABLES_CONFIG["SearchPers"][0],
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 2,
                    },
                    "Timeout": 1000,
                    "HedgingClientConfig": {
                        "Clients": [{"ClientConfig": {"ClusterName": yt_cluster}, "InitialPenalty": 0}],
                    },
                },
                "LTSearchProfileLoaderConfig": {
                    "Enabled": enable_kv_saas,
                    "EnableScatter": enable_kv_saas,
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 2,
                    },
                    "RequestParams": {
                        "Url": "http://127.0.0.1:{port}/ltp_profile".format(port=kv_saas_port),
                        "QueryText": "service=ltp_profile&sp_meta_search=multi_proxy&ms=proto&gta=_Body&timeout=45000",
                        "Sgkps": 0,
                        "RequestTimeoutMs": 1000,
                    },
                },
                "LTAdvProfileLoaderConfig": {
                    "Enabled": enable_kv_saas,
                    "EnableScatter": enable_kv_saas,
                    "CacheConfig": {
                        "Size": 2 * 10**9,
                        "Lifetime": 2,
                    },
                    "RequestParams": {
                        "Url": "http://127.0.0.1:{port}/ltp_profile".format(port=kv_saas_port),
                        "QueryText": "service=ltp_profile&sp_meta_search=multi_proxy&ms=proto&gta=_Body&timeout=45000",
                        "Sgkps": 1,
                        "RequestTimeoutMs": 1000,
                    },
                },
                "MarketDJKvSaasLoaderConfig": {
                    "Enabled": enable_kv_saas,
                    "EnableScatter": enable_kv_saas,
                    "CacheConfig": {
                        "Size": 1 * 10**9,
                        "Lifetime": 2,
                    },
                    "RequestParams": {
                        "Url": "http://127.0.0.1:{port}/market_dj".format(port=kv_saas_port),
                        "QueryText": "service=market_dj&sp_meta_search=multi_proxy&ms=proto&timeout=45000",
                        "Sgkps": 0,
                        "Attempts": 2,
                        "RequestTimeoutMs": 1000,
                    },
                },
                "MarketDJLoaderConfig": {
                    "Enabled": enable_market_dj,
                    "Url": "http://127.0.0.1:{port}".format(port=market_dj_port),
                    "RequestTimeoutMs": 1000,
                },
                "ClientCommonConfig": {"ClusterName": yt_cluster},
                "MasterClientConfig": {"ClusterName": yt_cluster},
                "KVSaasScatterClientConfig": {
                    "SimpleSourceScript": "http://127.0.0.1:{port}/ltp_profile".format(port=kv_saas_port),
                    "SourceType": "SIMPLE",
                    "SourceOptionsConfig": {
                        "TaskOptions": {
                            "ConnectTimeouts": "1s",
                            "SendingTimeouts": "1s",
                        },
                        "TimeOut": "1s",
                        "AllowConnStat": False,
                    },
                    "InflightLimit": 10,
                },
                "ScatterFactoryConfig": {
                    "RunnersPoolSize": 3,
                },
            },
            "Logs": {"Rules": [{"IncludeCategories": ["Main", "Miner", "Http"], "MinLevel": "Debug"}]},
            "SearchPersProfilesMergeConfig": {
                "RecordsFilters": [
                    {
                        "ContainerId": 1,
                        "MaxRecords": 3,
                        "StoreOptions": {
                            "StoreUrl": True,
                        },
                    }
                ]
            },
            "LTSearchProfilesMergeConfig": {
                "RecordsFilters": [
                    {
                        "ContainerId": 1,
                        "MaxRecords": 3,
                        "StoreOptions": {
                            "StoreUrl": True,
                        },
                    }
                ]
            },
            "HttpServer": {"Port": port},
            "HttpThreads": 4,
        }

        with open("eagle.cnf", "w") as f_p:
            ujson.dump(conf, f_p, indent=4)

        if env is not None:
            passed_env.update(env)

        # save cpuinfo before launching eagle
        dump_cpuinfo()

        proc = execute(
            [
                binary_path("ads/bsyeti/eagle/bin/eagle/eagle"),
                "--config-json",
                "eagle.cnf",
            ],
            wait=False,
            env=passed_env,
        )
        yield proc
    finally:
        kill_process(proc)


# todo: we need datagram_backend_port too
@contextmanager
def launch_balancer(port, admin_port, stat_port, dg_port, backends, backend_port, logs_path=None):
    proc = None
    try:
        conf_dict = {
            "threads": 4,
            "eagle_conf": {
                "dc_order": {"sas": ["sas"]},
                "use_hosts": True,
                "hosts": {"sas": [(h[0], h[1], backend_port) for h in backends]},
                "global_attempts": 3,
                "local_attempts": 3,
                "use_hashing": True,
                "status_handler": "status",
            },
            "datagram_conf": {
                "dc_order": {"sas": ["sas"]},
                "use_hosts": True,
                "hosts": {
                    # should be datagram_backend_port
                    "sas": [(h[0], h[1], backend_port) for h in backends]
                },
                "global_attempts": 3,
                "local_attempts": 3,
                "use_hashing": False,
                "status_handler": "sensors",
            },
        }

        conf = gen_rr_balancer_conf(
            "sas",
            conf_dict,
            ["::"],
            [port],
            admin_port,
            stat_port,
            dg_port,
            "{srv}/logs/access.log",
            "{srv}",
        )
        print(conf)
        conf = conf.replace("{srv}", logs_path)

        config = "balancer_%s.cnf" % port
        with open(config, "w") as f_p:
            f_p.write(conf)

        proc = execute([binary_path("balancer/daemons/balancer/balancer"), config], wait=False)
        yield proc
    finally:
        kill_process(proc)
