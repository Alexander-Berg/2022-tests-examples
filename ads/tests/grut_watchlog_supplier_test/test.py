import grut.libs.proto.objects.autogen.schema_pb2 as grut_schema
import grut.libs.object_api.proto.object_api_service_pb2 as grut_api
import grut.libs.proto.event_type.event_pb2 as event_types
import grut.libs.proto.watch.watch_pb2 as watch
import grut.libs.proto.transaction_context.transaction_context_pb2 as transaction_context
from grut.python.object_api.client import ObjectApiClient
from grut.python.object_api.client import objects
from grut.tools.admin.commands.data_traits import Traits
from grut.tools.admin.settings.clusters import CLUSTERS

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    launch_bullied_processes_reading_queue,
    make_json_file,
)

import yatest.common

from yt.yson import YsonUint64, get_bytes as yt_get_bytes

import jinja2
import pytest

import math
import os
import random
import re
from typing import Any, Callable, Dict, Iterable, List, NamedTuple, Set


class SimpleShardingTestStand(NamedTuple):
    test_id: str
    consuming_system_path: str
    master_path: str
    port_manager: Any
    output_yt_queue: Any


class ShardingConfig(NamedTuple):
    path: str


class ShardingProcess(BulliedProcess):
    def __init__(self, config_path):
        super(ShardingProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path("ads/bsyeti/big_rt/demo/sharding/binary_with_grut/sharder_with_grut"),
                "--config-json",
                config_path,
            ]
        )


def to_unix_timestamp(timestamp) -> int:
    _TIMESTAMP_COUNTER_WIDTH = 30
    return timestamp >> _TIMESTAMP_COUNTER_WIDTH


@pytest.fixture()
def stand(request, yt_client, port_manager, config_test_default_enabled) -> SimpleShardingTestStand:
    output_shards_count = 3
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id
    master_path = "//tmp/master_state_" + test_id

    output_yt_queue = create_yt_queue(yt_client, output_queue_path, output_shards_count)

    return SimpleShardingTestStand(
        test_id=test_id,
        consuming_system_path=consuming_system_path,
        master_path=master_path,
        port_manager=port_manager,
        output_yt_queue=output_yt_queue,
    )


def generate_id() -> YsonUint64:
    return YsonUint64(random.randint(1000, 1000000000))


def create_campaigns_with_context(
    grut_client: ObjectApiClient,
    client_id: int,
    campaign_ids: Iterable[YsonUint64],
    operator_uid: int,
):
    with grut_client.get_transacted_client() as transacted_client:
        campaigns_to_create = list()
        for campaign_id in campaign_ids:
            campaigns_to_create.append({
                "meta": {
                    "client_id": client_id,
                    "id": campaign_id,
                    "campaign_type": grut_schema.CT_TEXT,
                },
                "spec": {}
            })
        objects.create_objects(transacted_client, "campaign", campaigns_to_create)
        context = transaction_context.TTransactionContext(operator_uid=operator_uid)
        transacted_client.commit(context)


def generate_campaigns(grut_client: ObjectApiClient, objects_count: int, operator_uid: int) -> Iterable[YsonUint64]:
    client_id = generate_id()
    objects.create_objects(
        grut_client,
        "client",
        [{"/meta/id": client_id, "/spec/name": str(client_id)}]
    )

    object_ids: Set[YsonUint64] = set()
    while len(object_ids) < objects_count:
        object_ids.add(generate_id())

    batch: List[YsonUint64] = list()
    for campaign_id in object_ids:
        if len(batch) >= 100:
            create_campaigns_with_context(grut_client, client_id, batch, operator_uid)
            batch.clear()
        batch.append(campaign_id)

    if batch:
        create_campaigns_with_context(grut_client, client_id, batch, operator_uid)

    return object_ids


def generate_timestamp(grut_client: ObjectApiClient) -> int:
    return grut_client.generate_timestamp(grut_api.TReqGenerateTimestamp()).timestamp


def make_grut_supplier_config(params: Dict[str, Any]) -> ShardingConfig:
    with open(yatest.common.source_path("ads/bsyeti/big_rt/demo/sharding/read_from_grut_watchlog.conf")) as f:
        conf_s = jinja2.Template(f.read()).render(params)

    return ShardingConfig(
        path=str(make_json_file(conf_s, name_template="sharding_config_{json_hash}.json")),
    )


def sharder_launch_k_process(
    stand: SimpleShardingTestStand,
    shards_count: int,
    object_type: str,
    event_count_limit: int,
    time_limit: int,
    consumer_id_prefix: str,
    processes_count: int,
    worker_name: str,
    check_func: Callable,
    grut_address: str,
    use_object_identity_as_id: bool,
    filter_context: bool,
    timeout: int = 600,
) -> Iterable[ShardingProcess]:
    max_shards = int(math.ceil(shards_count / float(processes_count)))

    configs = [
        make_grut_supplier_config(
            dict(
                shards_count=shards_count,
                object_type=object_type,
                event_count_limit=event_count_limit,
                time_limit=time_limit,
                max_shards=max_shards,
                grut_address=grut_address,
                grut_secure="false",
                consumer_id_prefix=consumer_id_prefix,
                port=stand.port_manager.get_port(),
                consuming_system_main_path=stand.consuming_system_path,
                output_queue=stand.output_yt_queue["path"],
                output_shards_count=stand.output_yt_queue["shards"],
                yt_master_cluster=os.environ["YT_HTTP_PROXY_ADDR"],
                global_log=os.path.join(
                    yatest.common.output_path(),
                    "global_{}.log".format("{}-{}".format(worker_name, i)),
                ),
                worker_minor_name="{}-{}".format(worker_name, i),
                master_path=stand.master_path,
                random_shard_on_parse_error="true",
                filter_context=filter_context,
                use_object_identity_as_id="true" if use_object_identity_as_id else "false",
            )
        )
        for i in range(processes_count)
    ]
    sharders = [ShardingProcess(config.path) for config in configs]
    launch_bullied_processes_reading_queue(
        sharders, data_or_check_func=check_func, restart_randmax=None, timeout=timeout
    )
    return sharders


def get_rows_in_queue(stand: SimpleShardingTestStand, row_count_to_read: int = 100000) -> List[Any]:
    rows = []
    for shard in range(stand.output_yt_queue["shards"]):
        shard_result = stand.output_yt_queue["queue"].read(shard, 0, row_count_to_read)
        assert shard_result["offset_from"] == 0
        rows += shard_result["rows"]
    return rows


def get_data_from_out_queue(stand: SimpleShardingTestStand):
    return [yt_get_bytes(d).rsplit(b" ", 1)[0] for d in get_rows_in_queue(stand)]


def get_events_from_queue(stand: SimpleShardingTestStand, start_timestamp: int):
    data_from_out_queue = get_data_from_out_queue(stand)
    parsed_events: List[watch.TBigRtEvent] = list()
    for row in data_from_out_queue:
        event = watch.TBigRtEvent()
        event.ParseFromString(row)
        if event.timestamp >= start_timestamp:
            parsed_events.append(event)
    return parsed_events


def create_read_complete_checker(stand: SimpleShardingTestStand, objects_count: int, start_timestamp: int) -> Callable:
    def check_data_read():
        events_from_queue = get_events_from_queue(stand, start_timestamp)
        return len(events_from_queue) >= objects_count

    return check_data_read


def get_shards_count(yt_client, table: str) -> int:
    traits = Traits(CLUSTERS["local"])
    watch_log_tablets = os.path.join(traits.get_path(), "db", "campaigns_watch_log", "@tablet_count")
    return yt_client.get(watch_log_tablets)


@pytest.mark.parametrize("processes_count", [3])
@pytest.mark.parametrize("object_type", ["campaign"])
@pytest.mark.parametrize("event_count_limit", [10])
@pytest.mark.parametrize("time_limit", [1000])
@pytest.mark.parametrize("objects_count_v0", [250])
@pytest.mark.parametrize("objects_count_v1", [250])
@pytest.mark.parametrize("consumer_id_prefix", ["campaign_watch_log_consumer"])
@pytest.mark.parametrize("use_object_identity_as_id", [False, True])
@pytest.mark.parametrize("filter_context", [False, True])
def test_grut_watchlog_supplier(
    stand: SimpleShardingTestStand,
    object_api_client: ObjectApiClient,
    yt_client,
    processes_count: int,
    object_type: str,
    event_count_limit: int,
    time_limit: int,
    objects_count_v0: int,
    objects_count_v1: int,
    consumer_id_prefix: str,
    use_object_identity_as_id: bool,
    object_api_grpc_server_address: str,
    filter_context: bool,
) -> None:
    shards_count = get_shards_count(yt_client, "campaigns_watch_log")
    start_timestamp = generate_timestamp(object_api_client)

    object_ids_v0: Set[YsonUint64] = set(generate_campaigns(object_api_client, objects_count_v0, 0))
    object_ids_v1: Set[YsonUint64] = set(generate_campaigns(object_api_client, objects_count_v1, 1))

    if filter_context:
        expected_ids = object_ids_v0
    else:
        expected_ids = object_ids_v0.union(object_ids_v1)

    sharder_launch_k_process(
        stand=stand,
        shards_count=shards_count,
        object_type=object_type,
        event_count_limit=event_count_limit,
        time_limit=time_limit,
        consumer_id_prefix=consumer_id_prefix,
        processes_count=processes_count,
        worker_name=stand.test_id,
        check_func=create_read_complete_checker(stand, len(expected_ids), start_timestamp),
        grut_address=object_api_grpc_server_address,
        use_object_identity_as_id=use_object_identity_as_id,
        filter_context=filter_context,
        timeout=300,
    )

    # check events
    parsed_events: List[watch.TBigRtEvent] = get_events_from_queue(stand, start_timestamp)
    assert len(expected_ids) == len(parsed_events)
    for event in parsed_events:
        if use_object_identity_as_id:
            meta = grut_schema.TCampaignMeta()
            meta.ParseFromString(event.object_id)
            assert meta.id in expected_ids
            assert meta.client_id
        else:
            assert YsonUint64(event.object_id) in expected_ids
        assert to_unix_timestamp(event.timestamp) == event.unix_timestamp
        assert event.object_type == grut_schema.OT_CAMPAIGN
        assert event.origin_object_type == grut_schema.OT_CAMPAIGN
        assert event.event_type == event_types.ET_OBJECT_CREATED

    # check watchlog consumers
    consumers = objects.select_objects(
        object_api_client,
        "watch_log_consumer",
        filter="true",  # some filter always required
        attribute_selector=["/meta", "/spec", "/status"],
        as_dict=True,
        allow_full_scan=True,
    )

    assert len(consumers) == processes_count
    for consumer in consumers:
        assert consumer["meta"]["id"].startswith(consumer_id_prefix)
        assert consumer["spec"]["object_type"] == object_type
        assert consumer["status"]["continuation_token"] is not None
