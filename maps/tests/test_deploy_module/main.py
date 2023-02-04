import datetime as dt
from freezegun import freeze_time
import pytz

from maps.garden.sdk import core
from maps.garden.sdk import resources
from maps.garden.sdk.module_rpc.module_runner import run_module
from maps.garden.sdk.module_autostart import module_autostart as autostart


class TestTask(core.Task):
    def __call__(self, *args, **kwargs):
        pass


def fill_graph(builder, regions=[]):
    builder.add_resource(resources.PythonResource("output_resource"))

    builder.add_task(
        core.Demands(input_resource="input_resource"),
        core.Creates(output_resource="output_resource"),
        TestTask())


@freeze_time(dt.datetime(2020, 12, 25, 17, 45, 00))
def handle_build_status(trigger_build, build_manager):
    test_command = trigger_build.properties.get("test_command")
    if test_command == "no_build":
        return
    elif test_command == "raise_exception":
        raise RuntimeError("Something went wrong")
    elif test_command == "delay_start":
        build_manager.delay_until(dt.datetime(2020, 12, 25, 18, 45, tzinfo=pytz.utc))
        return
    elif test_command == "create_build":
        build_manager.create_build(
            source_ids=[
                source_build.full_id
                for source_build in build_manager.get_builds("test_module")
                if source_build.status == autostart.BuildStatus.COMPLETED
            ],
            properties={"deploy_step": "stable"},
        )
    else:
        raise RuntimeError(f"Got unexpected test_command {test_command}")


def run():
    run_module("test_deploy_module", fill_graph, handle_build_status=handle_build_status)
