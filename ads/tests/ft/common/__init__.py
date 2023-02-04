import itertools
import importlib
import os
import random
import re
import time
from datetime import datetime

import pytest

import yatest.common
from copy import deepcopy
from functools import partial

from yt.recipe.multi_cluster.lib import _YtClusterGroup, get_yt_cluster as _get_yt_cluster

from library.python.sanitizers import asan_is_on

from ads.bsyeti.big_rt.py_test_lib import helpers
from ads.bsyeti.big_rt.py_test_lib.helpers import QueueClient

from ads.bsyeti.caesar.tests.lib import create_profile_table
from ads.bsyeti.caesar.tests.lib.b2b.helpers import select_rows
from ads.bsyeti.caesar.tests.lib.b2b.utils import make_queue, QUEUE_CONSUMER
from .caesar import run_caesar
from .parser import parse_profile, to_snake_case


random.seed(0xDEADBEEF)


def get_yt_cluster():
    yt_cluster = _get_yt_cluster()
    for node in (yt_cluster.primary, yt_cluster.secondary):
        node.get_yt_client().set("//sys/accounts/tmp/@resource_limits/tablet_count", 100500)
    return yt_cluster


def get_yt_cluster_from_docker():

    cluster_settings_dict = {
        "id" : "primary",
        "proxy_address" : os.environ['YT_PROXY']
    }

    yt_cluster = _YtClusterGroup([cluster_settings_dict])
    yt_cluster.primary.get_yt_client().set("//sys/accounts/tmp/@resource_limits/tablet_count", 100500)
    return yt_cluster


def table_path(category, profile_name, uniq_id):
    return "//tmp/%s_%s_%s" % (category, profile_name, uniq_id)


def create_tables(yt_cluster, tables):
    for profile, path in tables.items():
        create_profile_table(yt_cluster, profile, path, enable_balancer=True)


def create_queues(yt_cluster, queues):
    yt_client = yt_cluster.primary.get_yt_client()

    queues = queues.copy()
    input_queue = make_queue(
        yt_client,
        queues["input"],
        queues["shard_count"],
    )
    queues["input"] = QueueClient(input_queue, QUEUE_CONSUMER)

    if queues["watcher"]:
        queues["watcher"] = make_queue(
            yt_client,
            queues["watcher"],
            queues["shard_count"],
        )
    return queues


def make_stand(request, table, extra_profiles, shard_count, enable_watcher, extra_config_args, now):
    test_id = re.sub(r"[^\w\d]", "_", request.node.nodeid)
    table_path_factory = partial(table_path, uniq_id=test_id)

    tables = {t: table_path_factory(t, "state") for t in [table] + list(extra_profiles.keys())}
    queues = {
        "shard_count": shard_count,
        "input": table_path_factory(table, "input_queue"),
        "watcher": table_path_factory(table, "watcher_queue") if enable_watcher else None,
    }

    return helpers.make_namedtuple(
        "Stand",
        table=table,
        extra_profiles=extra_profiles,
        tables=tables,
        queues=queues,
        extra_config_args=extra_config_args,
        table_path_factory=table_path_factory,
        test_id=test_id,
        now=now,
    )


def gen_shard_testing_schemas(length):
    for j in range(1, 1000):
        cnt = int(2.5**j)
        yield [i % cnt for i in range(length)]
        yield [random.randint(0, cnt - 1) for i in range(length)]


def generate_test_ids(shards_count, profiles_count=500):
    if asan_is_on():
        profiles_count = min(profiles_count, 100)

    schemas = itertools.islice(gen_shard_testing_schemas(profiles_count), shards_count)

    data = {shard: [((u + 1) * shards_count + shard) for u in schema] for shard, schema in enumerate(schemas)}

    return data


def import_all_cases(src_path: str):
    cases = []
    cases_dir = yatest.common.source_path(src_path)
    modules = (x[:-3] for x in os.listdir(cases_dir) if x.endswith(".py") and x != "__init__.py")
    for m in modules:
        module_path = ".".join(src_path.split("/")) + "." + m
        pytest.register_assert_rewrite(module_path)
        try:
            module = importlib.import_module(module_path)
        except ModuleNotFoundError:
            raise Exception("cases/%s.py is missing in ya.make" % m) from None

        for x in (getattr(module, x) for x in dir(module)):
            if isinstance(x, type) and x.__name__.startswith("TestCase"):
                cases.append(x)
    return cases


def generate_events(profile_ids, event_generator):
    now = int(time.time())
    events = {}
    for shard, ids in profile_ids.items():
        events[shard] = []
        for profile_id in ids:
            for event in event_generator(now, shard, profile_id):
                events[shard].append(deepcopy(event))
        assert events[shard]
    assert events
    return events


def update_tables(yt_cluster, tables, extra_profiles):
    yt_client = yt_cluster.primary.get_yt_client()
    for table, rows in extra_profiles.items():
        if rows:
            yt_client.insert_rows(tables[table], rows)


def select_profiles(yt_cluster, tables, table_name, profile_class=None):
    yt_client = yt_cluster.primary.get_yt_client()
    profiles = []
    rows = select_rows(yt_client, "* from [%s]" % tables[table_name])
    for row in rows:
        profiles.append(parse_profile(table_name, row, profile_class))
    return profiles


def run_test_case(request, case_class, port_manager, model_service_port=0, from_docker=False):
    if getattr(case_class, "skipped", False):
        pytest.skip()

    case = case_class()

    stand = make_stand(
        request,
        case.table,
        getattr(case, "extra_profiles", {}),
        getattr(case, "SHARDS_COUNT", 3),
        getattr(case, "check_watcher_events", None) is not None,
        getattr(case, "extra_config_args", {}),
        getattr(case, "NOW", datetime.now()),
    )

    generate_profile_ids = getattr(case, "generate_profile_ids", generate_test_ids)
    generated_profile_ids = generate_profile_ids(stand.queues["shard_count"])

    events = generate_events(generated_profile_ids, case.make_events)

    if from_docker is True:
        yt_cluster = get_yt_cluster_from_docker()
    else:
        yt_cluster = get_yt_cluster()
    queues = create_queues(yt_cluster, stand.queues)
    create_tables(yt_cluster, stand.tables)
    update_tables(yt_cluster, stand.tables, stand.extra_profiles)

    queue_writer = queues["input"].create_writer()
    queue_writer.write(events)

    yt_client = yt_cluster.primary.get_yt_client()
    with run_caesar(stand, yt_cluster, port_manager.get_port(), model_service_port):
        queue_writer.flush(wait=True)

        profile_class = getattr(case, "profile_class", None)
        profiles = select_profiles(yt_cluster, stand.tables, stand.table, profile_class)

        if getattr(case, "assert_empty_profiles", True):
            assert 0 != len(profiles)
        case.check_profiles(profiles)

        if queues["watcher"]:
            watcher_events = []
            for shard_id in range(queues["watcher"]["shards"]):
                parsed_events = []
                max_profiles = len(generated_profile_ids[shard_id])
                for event in queues["watcher"]["queue"].read_messages(shard_id, 0, max_profiles):
                    p = case.profile_class()
                    p.ParseFromString(event)
                    parsed_events.append(p)
                watcher_events.append(parsed_events)

            case.check_watcher_events(watcher_events)

        if hasattr(case, "extra_profiles"):
            for table in case.extra_profiles:
                method = "check_%s_table" % to_snake_case(table)
                if hasattr(case, method):
                    rows = list(select_rows(yt_client, "* from [%s]" % stand.tables[table]))
                    getattr(case, method)(rows)
