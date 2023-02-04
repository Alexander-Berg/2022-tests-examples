import datetime as dt
import json
import pytest

from maps.garden.sdk.module_autostart import module_autostart as autostart
from maps.garden.sdk.module_rpc import module_runner
from maps.garden.sdk.module_rpc.proto import common_pb2 as common
from maps.garden.sdk.module_rpc.proto import module_rpc_pb2 as module_rpc
from maps.garden.sdk.module_traits import module_traits as mt

from . import graph


def _make_command():
    command = module_rpc.Input()
    command.command = module_rpc.Input.Command.HANDLE_BUILD_STATUS
    command.handleBuildStatusInput.triggerId.moduleName = "ymapsdf"
    command.handleBuildStatusInput.triggerId.id = 1
    command.handleBuildStatusInput.attempt = 10

    for idx, region in enumerate(["cis1", "cis2"], start=1):
        build = command.handleBuildStatusInput.builds.add()
        build.fullId.moduleName = "ymapsdf"
        build.fullId.id = idx
        build.propertiesJson = json.dumps({"shipping_date": "20210113", "region": region})
        build.status = common.BuildStatus.BUILD_STATUS_COMPLETED
        build.finishedAt.FromDatetime(dt.datetime.utcnow())
        source_id = build.sourceIds.add()
        source_id.moduleName = "ymapsdf_src"
        source_id.id = idx

    return command


@pytest.fixture
def prepare_traits(mocker):
    mocker.patch(
        "maps.garden.sdk.module_traits.module_traits.load_traits_from_resource",
        return_value=mt.ModuleTraits(
            name="test_module",
            type=mt.ModuleType.REDUCE,
        ),
    )


def test_no_action(prepare_traits):
    def do_nothing(trigger_build, build_manager):
        # No new build is created
        pass

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
            handle_build_status=do_nothing,
        )
    )

    assert output.IsInitialized()
    assert not output.HasField("result")
    assert not output.HasField("exception")


def test_exception(prepare_traits):
    def raise_exception(trigger_build, build_manager):
        raise RuntimeError("Something went wrong")

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
            handle_build_status=raise_exception,
        )
    )

    assert output.IsInitialized()
    assert not output.HasField("result")
    assert output.HasField("exception")


def test_create_build(prepare_traits):
    properties = {
        "key": "value",
    }

    def handle_build_status(trigger_build, build_manager):
        assert build_manager.attempt == 10

        build_manager.create_build(
            source_ids=[
                autostart.BuildId(module_name="ymapsdf", id=1),
                autostart.BuildId(module_name="ymapsdf", id=2),
            ],
            properties=properties,
        )

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
            handle_build_status=handle_build_status,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result")
    assert not output.HasField("exception")

    assert list(output.result.createBuildParams.sourceIds) == [
        common.BuildId(moduleName="ymapsdf", id=1),
        common.BuildId(moduleName="ymapsdf", id=2),
    ]
    assert output.result.createBuildParams.propertiesJson == json.dumps(properties)


@pytest.mark.freeze_time(dt.datetime(2020, 6, 1,  12, 30, 45))
def test_delay(prepare_traits):
    def handle_build_status(trigger_build, build_manager):
        build_manager.delay_until(trigger_build.finished_at + dt.timedelta(hours=2))

    output = module_runner.process_protobuf_command(
        _make_command(),
        module_runner.ModuleProperties(
            module_name=graph.MODULE_NAME,
            fill_graph=None,
            handle_build_status=handle_build_status,
        )
    )

    assert output.IsInitialized()
    assert output.HasField("result")
    assert not output.HasField("exception")
    assert not output.result.HasField("createBuildParams")
    assert output.result.HasField("retryAt")
    assert output.result.retryAt.ToDatetime() == dt.datetime(2020, 6, 1, 14, 30, 45)


def test_build_status_enums():
    # protobuf enum has one additional INVALID value
    assert len(common.BuildStatus.keys()) == len(autostart.BuildStatus) + 1

    # skip INVALID value
    proto_items = common.BuildStatus.items()[1:]

    for proto_status, autostart_status in zip(proto_items, autostart.BuildStatus):
        assert proto_status[0][13:] == autostart_status.name
        assert proto_status[1] == autostart_status.value
