import os

import yatest.common

from ads.bsyeti.big_rt.py_test_lib import make_json_file, make_namedtuple

from . import schema, utils

RESHARDER_BINARY_PATH = "ads/bsyeti/resharder/bin/resharder"
# timeout for each group
RESHARDER_WAITING_TIMEOUT = 240


def gen_config(port):
    conf = {}
    srcs = utils.get_sources()
    destinations = utils.get_destinations()
    conf["Logs"] = {
        "Rules": [
            {
                "FilePath": os.path.join(yatest.common.output_path(), "resharder.log"),
                "MinLevel": "Debug",
            }
        ]
    }
    conf["Sources"] = list(srcs.values())
    conf["Destinations"] = destinations
    cluster = os.environ["YT_PROXY"]
    conf["YtCluster"] = cluster
    conf["HttpServer"] = {"Port": port}
    for s in conf["Sources"]:
        parser = s["RowsProcessor"]["Parser"]
        if "PhraseIDGenerator" in parser:
            parser["PhraseIDGenerator"]["PhraseIDByMD5Path"] = schema.PHRASEID_PATH
            parser["PhraseIDGenerator"]["YtHedgingClientConfig"] = {
                "BanDuration": 0,
                "BanPenalty": 0,
                "Clients": [
                    {
                        "ClientConfig": {
                            "ChannelPoolSize": 2,
                            "ClusterName": cluster,
                            "EnableRetries": True,
                        },
                        "InitialPenalty": 0,
                    }
                ],
            }
    return make_namedtuple(
        "ShardingConfig",
        path=make_json_file(conf, name_template="resharder_config_{json_hash}.json"),
    )
