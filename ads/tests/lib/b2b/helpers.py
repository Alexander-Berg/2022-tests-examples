import contextlib
import datetime
import itertools
import logging
import pprint
import os
import time
import sys

import yt
import ujson as json
import yatest.common
import yt.wrapper
import yt.yson as yson
import yatest

from ads.bsyeti.big_rt.py_lib import YtQueuePath
from ads.bsyeti.big_rt.py_test_lib import helpers as test_lib_helpers
from ads.bsyeti.big_rt.py_test_lib import make_json_file
from ads.bsyeti.big_rt.py_test_lib.logsampler.samplers import utils as samplers_utils
from ads.bsyeti.big_rt.py_test_lib.resharder import ReshardingProcess
from ads.bsyeti.caesar.tests.lib import create_profile_table
from ads.bsyeti.caesar.tools.logtests.config.config import config as samplers_config
from ads.bsyeti.big_rt.lib.events.proto.event_pb2 import TEventMessage
from ads.bsyeti.big_rt.py_test_lib.helpers import QueueClient
from ads.bsyeti.caesar.tests.lib.b2b.utils import QUEUE_CONSUMER

from . import caesar, resharder, utils

log = logging.getLogger(__name__)

RETRY_SECONDS = 1
RETRY_ATTEMPTS = 5


def read_sample(fname):
    return samplers_utils.read_sample(samplers_config, fname)


@contextlib.contextmanager
def launch_resharder(stand):
    config = resharder.gen_config(port=stand.resharder_port)
    process = ReshardingProcess(resharder.RESHARDER_BINARY_PATH, config.path)
    with process:
        yield process


@contextlib.contextmanager
def launch_caesar(stand, problems_interval_seconds, problems_max_count):
    conf = caesar.gen_config(port=stand.caesar_port, workers=stand.caesar_workers)
    path = make_json_file(conf, name_template="caesar_config_{json_hash}.json")
    process = caesar.CaesarProcess(path, stand.now)
    with process:
        if problems_interval_seconds is not None:
            process.emulate_problems(interval_seconds=problems_interval_seconds, problems_max_count=problems_max_count)
        yield process


@contextlib.contextmanager
def prepare_queues(stand):
    # get worker input data from sb resourses
    file = open(os.path.join(yatest.common.work_path(), 'worker_input_sample/worker_input_sample.yson'), 'rb')
    worker_data = yson.load(file)
    file.close()

    queues = stand.queues
    for queue in worker_data:
        queue_client = QueueClient(queues[f'//tmp/{queue}'], QUEUE_CONSUMER)
        queue_writer = queue_client.create_writer()
        for row in worker_data[queue]:
            event = TEventMessage().FromString(yson.get_bytes(row))
            queue_writer.write(event)
        queue_writer.flush()


@contextlib.contextmanager
def prepate_grut(stand):
    yt_client = yt.wrapper.YtClient(proxy=os.environ['YT_PROXY'])
    # every operation must be done under single transaction
    tran_id = yt_client.start_transaction(parent_transaction=None, timeout=None, attributes=None)

    # todo make this task async
    for table in utils.get_grut_tables_list():
        with open(os.path.join(yatest.common.work_path(), f'worker_input_sample/grut_data/{table}.yson'), 'rb') as f:
            raw_table_rows = yson.load(f)
            table_rows = []
            for row in raw_table_rows:
                table_rows.append(row)
            yt_client.insert_rows(f"//grut/db/{table}", table_rows, format=yt.wrapper.YsonFormat())

    yt_client.commit_transaction(tran_id)


def get_sensors(port):
    data = test_lib_helpers.get_raw_sensors(port)
    parsed_sensors = {}
    for sensor in data["sensors"]:
        # unfortunately "value" is missing sometimes, don't know better way to fix it
        if sensor["kind"] != "HIST" and "value" in sensor:
            sensor_name = sensor["labels"]["sensor"]
            topic_name = sensor["labels"].get("src_topic")
            parsed_sensors.setdefault(sensor_name, {})
            parsed_sensors[sensor_name][topic_name] = sensor["value"]
    return parsed_sensors


def trim_lines(lines):
    # too heavy for asan and such
    s = sum(len(line) for line in lines)
    if len(lines) == 1 and s > 1024 * 1024:
        return []
    return lines[: max(1, len(lines) // 10)]


def process_resharding(stand, resharder_process, trim_data):
    sources = utils.get_sources()
    for source in sorted(sources):
        with utils.measure_time("resharding {}".format(source)):
            lines = read_sample(source)
            if trim_data:
                lines = trim_lines(lines)
            data = {0: lines}
            queue = stand.queues[stand.source2queue[source]]

            expected_offsets = test_lib_helpers.retry(test_lib_helpers.get_expected_offsets)(
                queue, utils.QUEUE_CONSUMER, {i: len(lines) for i in range(len(data))}
            )

            log.debug("write to %s: %s", queue, len(lines))
            for _ in range(RETRY_ATTEMPTS):
                try:
                    queue["queue"].write(data, "zstd_6")
                    break
                except RuntimeError as e:
                    log.debug("exception occurs during write to the queue: %s", str(e))
                    time.sleep(RETRY_SECONDS)
            else:
                raise RuntimeError("failed to write to queue")

            assert wait_until_read(
                [queue],
                utils.QUEUE_CONSUMER,
                timeout=resharder.RESHARDER_WAITING_TIMEOUT,
                process=resharder_process,
                expected_offsets=[expected_offsets],
            )

    sensors = get_sensors(stand.resharder_port)
    sensors_file_name = os.path.join(yatest.common.output_path(), "resharder_sensors.txt")
    with open(sensors_file_name, "a") as f:
        f.write("%s - Resharder sensors:\n" % datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        f.write(pprint.pformat(sensors))
        f.write("\n")
    log.info("Resharder sensors dumped to %s", sensors_file_name)

    for topic, v in sensors["rows_count_skipped_by_error"].items():
        assert v == 0, f"topic {topic} has unparsed messages"


def wait_for_caesar(sources, process):
    assert wait_until_read(sources, utils.QUEUE_CONSUMER, timeout=caesar.CAESAR_WAITING_TIMEOUT, process=process)


def wait_until_read(queues, consumer, timeout, process, check_interval=0.2, expected_offsets=None):
    start = time.time()
    while time.time() < start + timeout and process.running():
        if expected_offsets is None:
            if test_lib_helpers.check_queues_read_unsafe(queues, consumer):
                return True
        else:
            if test_lib_helpers.check_queues_read(queues, consumer, expected_offsets):
                return True
        time.sleep(check_interval)
    return False


def select_rows(yt_client, query):
    _format = yt.wrapper.format.YsonFormat(encoding=None)
    return yt_client.select_rows(query, yt.wrapper.SYNC_LAST_COMMITED_TIMESTAMP, format=_format)


def sort_profiles(profiles):
    json.dumps(profiles, sort_keys=True)


def freeze_queues(stand, workers=None):
    if workers is None:
        workers = stand.caesar_workers

    futures = []
    with utils.threadpool() as pool:
        for w in workers:
            futures.append(pool.submit(freeze_queue, stand, w.input_queue))
        for f in futures:
            f.result()


def unfreeze_queues(stand, workers=None):
    if workers is None:
        workers = stand.caesar_workers

    futures = []
    with utils.threadpool() as pool:
        for w in workers:
            futures.append(pool.submit(unfreeze_queue, stand, w.input_queue))
        for f in futures:
            f.result()


def create_profile_tables(yt_cluster, workers):
    futures = []
    with utils.threadpool() as pool:
        for w in workers:
            futures.append(pool.submit(create_profile_table, yt_cluster, w.name, w.state_table_path))
    for f in futures:
        f.result()


def drop_profile_tables(yt_client, workers):
    futures = []
    with utils.threadpool() as pool:
        for w in workers:
            futures.append(pool.submit(yt_client.remove, w.state_table_path))
    for f in futures:
        f.result()


def freeze_queue(stand, queue_name):
    stand.yt_client.unmount_table(YtQueuePath(queue_name).queue_table_path, sync=True)


def unfreeze_queue(stand, queue_name):
    stand.yt_client.mount_table(YtQueuePath(queue_name).queue_table_path, sync=True)


DEFUALT_PRIORITY = 0
QUEUE_PRIORITY = {"NormalizedUrls": -3, "Orders": -2, "Banners": -1}


def get_ordered_worker_batches(stand):
    all_workers = stand.caesar_workers
    all_workers.sort(key=lambda w: w.name)

    name2priority = {w.name: i for i, w in enumerate(all_workers)}
    assert len(set(QUEUE_PRIORITY) - set(name2priority)) == 0
    name2priority.update(QUEUE_PRIORITY)

    def group_key(w):
        return name2priority[w.name]

    all_workers.sort(key=group_key)
    for priority, workers in itertools.groupby(all_workers, key=group_key):
        yield priority, workers


def _log_profiles(profiles):
    # consumes a lot of time
    for k, profiles in profiles.items():
        log.debug("%s profiles of %s:", len(profiles), k)
        for p in profiles:
            log.debug(p)


def read_profile(yt_client, worker):
    query = "* from [{table}]".format(table=worker.state_table_path)
    rows = select_rows(yt_client, query)
    extractor = utils.get_json_extractor(worker.name)
    rows = [json.loads(utils.safe_binaryjson_decode(extractor(row))) for row in rows]
    rows.sort(key=lambda p: json.dumps(p, sort_keys=True))
    return rows


def read_profiles(stand, upload_to_yt):
    rows_futures = {}
    pushed_to_yt = {}
    with utils.threadpool() as pool:
        for w in stand.caesar_workers:
            rows_futures[w.name] = pool.submit(read_profile, stand.yt_client, w)
            if upload_to_yt:
                assert 'YT_TOKEN' in os.environ
                pushed_to_yt[w.name] = pool.submit(push_to_yt, stand.yt_client, w)
    if upload_to_yt:
        for item in pushed_to_yt.items():
            # wait for all data to finish uploading
            pass
    return {k: v.result() for k, v in rows_futures.items()}


def push_to_yt(yt_client, worker):
    yt_profiles_path = yatest.common.get_param("yt_profiles_path")
    # need to create new client for every table cause of singlethread behavior of YtClient
    global_yt_client = yt.wrapper.YtClient(proxy="hahn", token=os.environ['YT_TOKEN'])

    query = "* from [{table}]".format(table=worker.state_table_path)
    local_rows = select_rows(yt_client, query)
    global_rows = []

    for row in local_rows:
        global_rows.append(row)

    schema = yt_client.get_attribute(worker.state_table_path, "schema")
    table_path = os.path.join(yt_profiles_path, worker.name)

    try:
        if global_yt_client.exists(table_path):
            global_yt_client.remove(table_path)

        schema = yt_client.get_attribute(worker.state_table_path, "schema")
        global_yt_client.create("table", table_path, recursive=True, attributes={"schema": schema})

        global_yt_client.write_table(table_path, global_rows, raw=False)
    except:
        sys.stderr.write(f"Cant create table {table_path}\n")

    return worker.name


def clear_table(yt_client, path):
    input_query = "1u as [$change_type], *"
    yt_client.run_merge(
        path,
        yt_client.TablePath(path, append=True, attributes={"schema_modification": "unversioned_update"}),
        mode="ordered",
        spec={"input_query": input_query},
    )
