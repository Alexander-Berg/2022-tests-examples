import math
import os
import re
from typing import Any, Callable, Dict, Iterable, List, NamedTuple, Set, Optional

import jinja2
import pytest

import yatest.common
from yt.yson import YsonUint64, get_bytes as yt_get_bytes

from ads.bsyeti.big_rt.cli import lib as bigrt_cli
from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    launch_bullied_processes_reading_queue,
    make_json_file,
)
from ads.bsyeti.big_rt.py_test_lib.helpers import QueueClient, check_queue_is_read_unsafe
from ads.bsyeti.caesar.libs.profiles.proto.order_pb2 import TOrderProfileProto

import grut.libs.proto.objects.autogen.schema_pb2 as grut_schema
import grut.libs.object_api.proto.object_api_service_pb2 as grut_api
import grut.libs.proto.event_type.event_pb2 as event_types
import grut.libs.proto.watch.watch_pb2 as watch
from grut.python.object_api.client import ObjectApiClient
from grut.python.object_api.client import objects
from grut.tests.library.helpers import to_campaign_id


_QUEUE_CONSUMER = "test_cow"
_PROFILE_ID_FIELD = "OrderID"


def _make_queue(yt_client, qname, shards_count):
    assert qname.startswith("//tmp"), "invalid qname"
    queue = create_yt_queue(yt_client, qname, shards_count, add_timestamp=True)
    bigrt_cli.create_consumer(qname, _QUEUE_CONSUMER, ignore_in_trimming=0, client=yt_client)
    return queue


class SimpleShardingTestStand(NamedTuple):
    test_id: str
    consuming_system_path: str
    master_path: str
    port_manager: Any
    output_queue: Any
    input_queue: QueueClient


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


class IdGenerator:
    def __init__(self):
        self._id = to_campaign_id(1000)

    def next_id(self) -> int:
        result_id = self._id
        self._id += 1
        return result_id


@pytest.fixture(scope="session")
def id_generator() -> IdGenerator:
    return IdGenerator()


@pytest.fixture()
def stand(request, yt_client, port_manager, config_test_default_enabled) -> SimpleShardingTestStand:
    queue_shards_count = 3
    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    output_queue_path = "//tmp/output_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id
    master_path = "//tmp/master_state_" + test_id
    updated_keys_queue_path = "//tmp/updated_keys_queue_" + test_id

    output_queue = create_yt_queue(yt_client, output_queue_path, queue_shards_count)

    input_queue = _make_queue(yt_client, updated_keys_queue_path, queue_shards_count)

    return SimpleShardingTestStand(
        test_id=test_id,
        consuming_system_path=consuming_system_path,
        master_path=master_path,
        port_manager=port_manager,
        output_queue=output_queue,
        input_queue=QueueClient(input_queue, _QUEUE_CONSUMER, _PROFILE_ID_FIELD),
    )


class GrutObjects:
    FLAGS_COLUMN_ID = 3
    COUNTERS_COLUMN_ID = 4

    def __init__(
        self,
        id_generator: IdGenerator,
        campaign_count: int,
        group_multiplier: int,
        banner_multiplier: int,
        extra_id: bool = False
    ):
        self._id_generator = id_generator
        self._campaign_count = campaign_count
        self._group_multiplier = group_multiplier
        self._banner_multiplier = banner_multiplier
        self._extra_id = extra_id

        self.client_id: YsonUint64 = 0
        self.campaign_ids: List[YsonUint64] = []
        self.group_id_2_campaign_id: Dict[YsonUint64, YsonUint64] = dict()
        self.banner_id_2_group_id: Dict[YsonUint64, YsonUint64] = dict()
        self.banner_id_2_campaign_id: Dict[YsonUint64, YsonUint64] = dict()
        self.text_banner_ids: Set[YsonUint64] = set()
        self.image_banner_id_2_campaign_id: Dict[YsonUint64, YsonUint64] = dict()
        self.campaigns_with_changed_flags: Set[YsonUint64] = set()
        self.bannerland_banner_id_2_campaign_id: Dict[YsonUint64, YsonUint64] = dict()

    def generate(self, grut_client: ObjectApiClient, additional_banners: bool) -> None:
        self._create_client(grut_client)
        self._create_campaigns(grut_client)
        self._create_groups(grut_client)
        self._create_banners(grut_client)
        if additional_banners:
            self._create_bannerland_banners(grut_client)

    def push_campaigns_to_queue(self, queue: QueueClient) -> None:
        queue_writer = queue.create_writer()
        for campaign_id in self.campaign_ids:
            profile = TOrderProfileProto()
            profile.OrderID = campaign_id

            if campaign_id % 2 == 0:
                profile.ServiceFields.ChangedColumns = 1 << self.FLAGS_COLUMN_ID
                self.campaigns_with_changed_flags.add(campaign_id)
            else:
                profile.ServiceFields.ChangedColumns = 1 << self.COUNTERS_COLUMN_ID

            queue_writer.write(profile)
        queue_writer.flush()

    def expected_banner_ids(self) -> Set[YsonUint64]:
        result: Set[YsonUint64] = set()
        for banner_id in self.text_banner_ids:
            campaign_id = self.banner_id_2_campaign_id[banner_id]
            if campaign_id in self.campaigns_with_changed_flags:
                result.add(banner_id)

        for banner_id, campaign_id in self.bannerland_banner_id_2_campaign_id.items():
            if campaign_id in self.campaigns_with_changed_flags:
                result.add(banner_id)

        for banner_id, campaign_id in self.image_banner_id_2_campaign_id.items():
            if campaign_id in self.campaigns_with_changed_flags:
                result.add(banner_id)

        return result

    def _generate_id(self) -> YsonUint64:
        return YsonUint64(self._id_generator.next_id())

    def _generate_ids(self, count: int) -> List[YsonUint64]:
        result: List[YsonUint64] = []
        while len(result) < count:
            result.append(self._generate_id())
        return result

    def _create_client(self, grut_client: ObjectApiClient) -> None:
        self.client_id = self._generate_id()
        objects.create_objects(
            grut_client,
            "client",
            [{"/meta/id": self.client_id, "/spec/name": str(self.client_id)}]
        )

    def _create_campaigns(self, grut_client: ObjectApiClient) -> None:
        self.campaign_ids = self._generate_ids(self._campaign_count)

        campaigns_to_create = list()
        for campaign_id in self.campaign_ids:
            campaigns_to_create.append({
                "meta": {
                    "client_id": self.client_id,
                    "id": campaign_id,
                    "direct_type": grut_schema.CT_TEXT,
                    "source": grut_schema.CSR_DIRECT,
                    "product_id": 1,
                    "agency_client_id": self.client_id,
                    "meta_type": grut_schema.CM_DEFAULT
                },
                "spec": {}
            })

        objects.create_objects(grut_client, "campaign_v2", campaigns_to_create)

    def _create_groups(self, grut_client: ObjectApiClient) -> None:
        for campaign_id in self.campaign_ids:
            for group_id in self._generate_ids(self._group_multiplier):
                self.group_id_2_campaign_id[group_id] = campaign_id

        groups_to_create = list()
        for group_id, campaign_id in self.group_id_2_campaign_id.items():
            groups_to_create.append({
                "meta": {
                    "campaign_id": campaign_id,
                    "id": group_id,
                    "direct_type": grut_schema.AGT_SMART
                },
                "spec": {}
            })

        objects.create_objects(grut_client, "ad_group_v2", groups_to_create)

    def _create_banners(self, grut_client: ObjectApiClient) -> None:
        for group_id, campaign_id in self.group_id_2_campaign_id.items():
            for banner_id in self._generate_ids(self._banner_multiplier):
                self.banner_id_2_group_id[banner_id] = group_id
                self.banner_id_2_campaign_id[banner_id] = campaign_id
                if banner_id % 2 == 0:
                    self.text_banner_ids.add(banner_id)

        banners_to_create = list()
        for banner_id, group_id in self.banner_id_2_group_id.items():
            image_banner_id = self._generate_id() if banner_id % 3 == 0 else 0
            if self._extra_id and image_banner_id > 0 and banner_id in self.text_banner_ids:
                self.image_banner_id_2_campaign_id[image_banner_id] = self.group_id_2_campaign_id[group_id]

            banners_to_create.append({
                "meta": {
                    "ad_group_id": group_id,
                    "id": banner_id,
                    "direct_id": banner_id,
                    "campaign_id": self.group_id_2_campaign_id[group_id],
                    "source": grut_schema.BS_DIRECT,
                    "direct_type": grut_schema.BT_TEXT if banner_id in self.text_banner_ids else grut_schema.BT_SMART,
                    "image_banner_id": image_banner_id
                },
                "spec": {}
            })

        objects.create_objects(grut_client, "banner_candidate", banners_to_create)

    def _create_bannerland_banners(self, grut_client: ObjectApiClient) -> None:
        for campaign_id in self.campaign_ids:
            for banner_id in self._generate_ids(self._banner_multiplier):
                self.bannerland_banner_id_2_campaign_id[banner_id] = campaign_id

        banners_to_create = list()
        for banner_id, campaign_id in self.bannerland_banner_id_2_campaign_id.items():
            banners_to_create.append({
                "meta": {
                    "id": banner_id,
                    "campaign_id": campaign_id
                },
                "spec": {}
            })

        objects.create_objects(grut_client, "bannerland_banner", banners_to_create)


def _to_unix_timestamp(timestamp) -> int:
    _TIMESTAMP_COUNTER_WIDTH = 30
    return timestamp >> _TIMESTAMP_COUNTER_WIDTH


def make_grut_supplier_config(params: Dict[str, Any]) -> ShardingConfig:
    with open(yatest.common.source_path("ads/bsyeti/big_rt/demo/sharding/read_from_grut_select.conf")) as f:
        conf_s = jinja2.Template(f.read()).render(params)

    return ShardingConfig(
        path=str(make_json_file(conf_s, name_template="sharding_config_{json_hash}.json")),
    )


def sharder_launch_k_process(
    stand: SimpleShardingTestStand,
    grut_address: str,
    object_type: str,
    origin_object_type: str,
    row_limit: int,
    query_filter: str,
    id_format: str,
    id_separator: str,
    index: str,
    additional_object_type: Optional[str],
    additional_filter: Optional[str],
    additional_id_format: Optional[str],
    additional_id_separator: Optional[str],
    additional_index: Optional[str],
    message_type: str,
    changed_column_id: int,
    profile_id_field: str,
    shards_count: int,
    processes_count: int,
    worker_name: str,
    check_func: Callable,
    timeout: int = 600,
    use_object_identity_as_id: bool = False,
    max_parallel_queries: int = 1,
    batch_size: int = 1,
    extra_id: Optional[str] = None,
) -> Iterable[ShardingProcess]:
    max_shards = int(math.ceil(shards_count / float(processes_count)))

    configs = [
        make_grut_supplier_config(
            dict(
                global_log=os.path.join(
                    yatest.common.output_path(),
                    "global_{}.log".format("{}-{}".format(worker_name, i)),
                ),
                port=stand.port_manager.get_port(),
                grut_address=grut_address,
                grut_secure="false",
                object_type=object_type,
                origin_object_type=origin_object_type,
                row_limit=row_limit,
                filter=query_filter,
                id_format=id_format,
                id_separator=id_separator,
                index=index,
                additional_object_type=additional_object_type,
                additional_filter=additional_filter,
                additional_id_format=additional_id_format,
                additional_id_separator=additional_id_separator,
                additional_index=additional_index,
                message_type=message_type,
                changed_column_id=changed_column_id,
                profile_id_field=profile_id_field,
                yt_master_cluster=os.environ["YT_HTTP_PROXY_ADDR"],
                input_queue=stand.input_queue.queue["path"],
                qyt_consumer=_QUEUE_CONSUMER,
                consuming_system_main_path=stand.consuming_system_path,
                shards_count=shards_count,
                max_shards=max_shards,
                worker_minor_name="{}-{}".format(worker_name, i),
                master_path=stand.master_path,
                output_shards_count=stand.output_queue["shards"],
                output_queue=stand.output_queue["path"],
                random_shard_on_parse_error="true",
                use_object_identity_as_id=str(use_object_identity_as_id).lower(),
                max_parallel_queries=max_parallel_queries,
                batch_size=batch_size,
                extra_id=extra_id,
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
    for shard in range(stand.output_queue["shards"]):
        shard_result = stand.output_queue["queue"].read(shard, 0, row_count_to_read)
        assert shard_result["offset_from"] == 0
        rows += shard_result["rows"]
    return rows


def create_read_complete_checker(stand: SimpleShardingTestStand, objects_count: int) -> Callable:
    def check_data_read():
        input_queue_is_read = check_queue_is_read_unsafe(stand.input_queue.queue, _QUEUE_CONSUMER) or objects_count == 0
        output_queue_rows_count = len(get_rows_in_queue(stand))
        return input_queue_is_read and output_queue_rows_count >= objects_count

    return check_data_read


def _run_grut_select(
    stand: SimpleShardingTestStand,
    object_api_client: ObjectApiClient,
    id_generator: IdGenerator,
    object_api_grpc_server_address: str,
    use_object_identity_as_id: bool,
    check_profiles: Callable,
    additional_object_type: Optional[str] = None,
    additional_filter: Optional[str] = None,
    additional_id_format: Optional[str] = None,
    additional_id_separator: Optional[str] = None,
    additional_index: Optional[str] = None,
    wait_all: bool = True,
    max_parallel_queries: int = 1,
    batch_size: int = 1,
    extra_id: Optional[str] = None,
) -> None:
    start_timestamp = object_api_client.generate_timestamp(grut_api.TReqGenerateTimestamp()).timestamp
    grut_objects = GrutObjects(id_generator, 100, 3, 5, bool(extra_id))
    grut_objects.generate(object_api_client, bool(additional_object_type))
    grut_objects.push_campaigns_to_queue(stand.input_queue)

    expected_banner_ids = grut_objects.expected_banner_ids()

    sharder_launch_k_process(
        stand=stand,
        object_type="banner_candidate",
        origin_object_type="campaign_v2",
        row_limit=5,
        query_filter="[/meta/campaign_id] IN (%v) AND [/meta/direct_type] = {}".format(int(grut_schema.BT_TEXT)),
        id_format="%vu",
        id_separator=",",
        index="banner_candidates_by_campaign",
        additional_object_type=additional_object_type,
        additional_filter=additional_filter,
        additional_id_format=additional_id_format,
        additional_id_separator=additional_id_separator,
        additional_index=additional_index,
        message_type="CAESAR_ORDER_PROFILE",
        changed_column_id=GrutObjects.FLAGS_COLUMN_ID,
        profile_id_field=_PROFILE_ID_FIELD,
        shards_count=3,
        processes_count=3,
        worker_name=stand.test_id,
        check_func=create_read_complete_checker(stand, len(expected_banner_ids) if wait_all else 0),
        grut_address=object_api_grpc_server_address,
        timeout=600 if wait_all else 60,
        use_object_identity_as_id=use_object_identity_as_id,
        max_parallel_queries=max_parallel_queries,
        batch_size=batch_size,
        extra_id=extra_id,
    )

    data_from_out_queue = [yt_get_bytes(d).rsplit(b" ", 1)[0] for d in get_rows_in_queue(stand)]

    parsed_events: Dict[Any, watch.TBigRtEvent] = dict()
    for row in data_from_out_queue:
        event = watch.TBigRtEvent()
        event.ParseFromString(row)
        if event.timestamp >= start_timestamp:
            if use_object_identity_as_id:
                meta = (
                    grut_schema.TBannerV2Meta()
                    if event.object_type == grut_schema.OT_BANNER_CANDIDATE
                    else grut_schema.TBannerlandBannerMeta()
                )
                meta.ParseFromString(event.object_id)
                key = (YsonUint64(meta.ad_group_id), YsonUint64(meta.id))
            else:
                key = YsonUint64(event.object_id)
            parsed_events[key] = event

    check_profiles(grut_objects, expected_banner_ids, parsed_events)


@pytest.mark.parametrize(
    ["use_object_identity_as_id", "extra_id"],
    [(False, None), (False, "image_banner_id"), (True, None)]
)
def test_grut_select_single(
    stand: SimpleShardingTestStand,
    object_api_client: ObjectApiClient,
    id_generator: IdGenerator,
    object_api_grpc_server_address: str,
    use_object_identity_as_id: bool,
    extra_id: Optional[str],
):
    def _check_profiles(grut_objects, expected_banner_ids, parsed_events):
        assert len(parsed_events) > 0
        assert len(expected_banner_ids) >= len(parsed_events)
        for banner_key, event in parsed_events.items():
            if use_object_identity_as_id:
                assert banner_key[1] in expected_banner_ids
                assert banner_key[1] in grut_objects.banner_id_2_group_id
                assert banner_key[0] == grut_objects.banner_id_2_group_id[banner_key[1]]
            else:
                assert banner_key in expected_banner_ids
            assert _to_unix_timestamp(event.timestamp) == event.unix_timestamp
            assert event.object_type == grut_schema.OT_BANNER_CANDIDATE
            assert event.origin_object_type == grut_schema.OT_CAMPAIGN_V2
            assert event.event_type == event_types.ET_OBJECT_UPDATED

    _run_grut_select(
        stand=stand,
        object_api_client=object_api_client,
        id_generator=id_generator,
        object_api_grpc_server_address=object_api_grpc_server_address,
        use_object_identity_as_id=use_object_identity_as_id,
        extra_id=extra_id,
        check_profiles=_check_profiles,
    )


@pytest.mark.parametrize(
    ["max_parallel_queries", "batch_size", "extra_id"],
    [(1, 1, None), (2, 10, "image_banner_id")]
)
def test_grut_select_multi(
    stand: SimpleShardingTestStand,
    object_api_client: ObjectApiClient,
    id_generator: IdGenerator,
    object_api_grpc_server_address: str,
    max_parallel_queries: int,
    batch_size: int,
    extra_id: Optional[str],
):
    def _check_profiles(grut_objects, expected_banner_ids, parsed_events):
        assert len(parsed_events) > 0
        assert len(expected_banner_ids) >= len(parsed_events)
        for banner_key, event in parsed_events.items():
            assert banner_key in expected_banner_ids
            assert _to_unix_timestamp(event.timestamp) == event.unix_timestamp
            assert event.object_type in (
                grut_schema.OT_BANNER_CANDIDATE,
                grut_schema.OT_BANNERLAND_BANNER,
            )
            assert event.origin_object_type == grut_schema.OT_CAMPAIGN_V2
            assert event.event_type == event_types.ET_OBJECT_UPDATED

    _run_grut_select(
        stand=stand,
        object_api_client=object_api_client,
        id_generator=id_generator,
        object_api_grpc_server_address=object_api_grpc_server_address,
        use_object_identity_as_id=False,
        check_profiles=_check_profiles,
        additional_object_type="bannerland_banner",
        additional_filter="[/meta/campaign_id] IN (%v)",
        additional_id_format="%vu",
        additional_id_separator=",",
        additional_index="bannerland_banners_by_campaign",
        max_parallel_queries=max_parallel_queries,
        batch_size=batch_size,
        extra_id=extra_id,
    )


def test_grut_select_bad_query(
    stand: SimpleShardingTestStand,
    object_api_client: ObjectApiClient,
    id_generator: IdGenerator,
    object_api_grpc_server_address: str,
):
    def _check_profiles(grut_objects, expected_banner_ids, parsed_events):
        assert len(parsed_events) == 0

    _run_grut_select(
        stand=stand,
        object_api_client=object_api_client,
        id_generator=id_generator,
        object_api_grpc_server_address=object_api_grpc_server_address,
        use_object_identity_as_id=False,
        check_profiles=_check_profiles,
        additional_object_type="bannerland_banner",
        additional_filter="[/spec/campaign_id] IN (%v)",
        additional_id_format="%vu",
        additional_id_separator=",",
        additional_index="bannerland_banners_by_campaign",
        wait_all=False,
    )
