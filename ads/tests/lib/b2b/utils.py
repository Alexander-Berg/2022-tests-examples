import contextlib
import datetime
import json
import logging
import os
import time
from concurrent.futures import ThreadPoolExecutor

import yatest.common

from ads.bsyeti.big_rt.cli import lib as bigrt_cli
from ads.bsyeti.big_rt.py_test_lib import create_yt_queue, make_namedtuple
from ads.bsyeti.big_rt.py_test_lib.logsampler.samplers import utils as samplers_utils
from ads.bsyeti.big_rt.py_test_lib.resharder import format_resharder_sources
from ads.bsyeti.caesar.libs.profiles.python import profiles
from ads.bsyeti.caesar.tools.logtests.config.config import config as samplers_config
from ads.bsyeti.caesar.tools.logtests.config import sources as caesar_sources
from ads.bsyeti.samogon.caesar_resharder.sources import sources

QUEUE_CONSUMER = "test_cow"
# it seems high level of concurrency causes problems on weak machines
CONCURRENCY = 5

log = logging.getLogger(__name__)


profile_type_alias = {
    "Strategy": "Strategies",
    "MarketCategory": "MarketCategories",
    "MarketFesh": "MarketFeshes",
    "ContextUrlMd5": "ContextUrlMd5Dict",
    "Communications": "Communications",
}


def get_profile_type(dest):
    name = profile_type_alias.get(dest)
    if name is not None:
        return name
    return dest + "s"


def get_json_extractor(profile_type):
    return profiles[profile_type]["convert_to_json"]


def fix_queue_name(qname):
    """
    convert qname so that it does not look like production
    """
    qname = qname.strip("/").split("/")[-1]
    qname = "//tmp/{}".format(qname)
    return qname


def get_sources():
    yt_cluster = os.environ["YT_PROXY"]
    return samplers_utils.filter_sources_by_samples(
        samplers_config,
        format_resharder_sources(caesar_sources.get_sources(), queue_consumer=QUEUE_CONSUMER, yt_cluster=yt_cluster)
    )


def get_destinations():
    # TODO: why dev? why not stable?
    _queues = sources.get_destinations("dev", None, os.environ["YT_PROXY"], "//home/bigb_resharder_test/caesar")
    queues = []
    copy_keys = ["DestinationName", "ReshardingHashFunction"]
    for _queue in _queues:
        queue = {k: _queue[k] for k in copy_keys}
        if "Writer" in _queue:
            queue["Writer"] = _queue["Writer"]
            queue["Writer"]["Queues"][0]["Path"] = fix_queue_name(queue["Writer"]["Queues"][0]["Path"])
        queue["ShardsCount"] = 1
        queue["SampleShardsMax"] = 1
        if "Writer" in queue:
            queues.append(queue)
    return queues


def make_queue(yt_client, qname, shards_count):
    assert qname.startswith("//tmp"), "invalid qname"
    queue = create_yt_queue(yt_client, qname, shards_count, swift=True, commit_order="strong")
    bigrt_cli.create_consumer(qname, QUEUE_CONSUMER, ignore_in_trimming=0, client=yt_client)
    return queue


def make_caesar_worker(name, dest):
    swift_writer = dest.get("SwiftWriter")
    if swift_writer:
        path = swift_writer["QueuePath"]
    else:
        path = dest["Writer"]["Queues"][0]["Path"]

    return make_namedtuple(
        "Worker",
        name=name,
        yt_cluster=os.environ["YT_PROXY"],
        state_table_path=f"//tmp/states/State_{name}",
        consumer=QUEUE_CONSUMER,
        input_queue=path,
        is_swift=bool(swift_writer),
    )


def get_test_now_dt():
    metadata = samplers_utils.load_metadata(samplers_config, samplers_utils.get_metadata_path(samplers_config))
    return metadata[samplers_config.METADATA_NOW]


def get_grut_tables_list():
    with open(os.path.join(yatest.common.work_path(), 'worker_input_sample/metadata.json'), 'r') as f:
        metadata = json.load(f)
        return metadata['grut_tables_list']


def get_worker_dt():
    file = open(os.path.join(yatest.common.work_path(), 'worker_input_sample/metadata.json'), 'r')
    metadata = json.load(file)
    file.close()

    return datetime.datetime.fromtimestamp(metadata['now'])


_escape_surrogates_map = {i: hex(i) for i in range(0xDC80, 0xDCFF + 1)}


def escape_surrogates(s):
    return s.translate(_escape_surrogates_map)


def safe_binaryjson_decode(s):
    return escape_surrogates(s.decode(errors="surrogateescape"))


@contextlib.contextmanager
def measure_time(event):
    start = time.time()
    yield
    log.debug("{} took {} sec".format(event, time.time() - start))


def threadpool():
    return ThreadPoolExecutor(max_workers=CONCURRENCY)
