from maps.garden.sdk import core
from maps.garden.sdk import resources
from maps.garden.sdk.module_rpc.module_runner import run_module
from maps.garden.sdk.module_autostart import module_autostart as autostart


class CustomModuleError(Exception):
    pass


class TestTask(core.Task):
    def __call__(self, *args, **kwargs):
        pass


def fill_graph(builder, regions=[]):
    builder.add_resource(resources.PythonResource("output_resource"))

    builder.add_task(
        core.Demands(input_resource="input_resource"),
        core.Creates(output_resource="output_resource"),
        TestTask())


def handle_build_status(trigger_build, build_manager):
    test_command = trigger_build.properties.get("test_command")
    if test_command == "no_build":
        return
    elif test_command == "raise_exception":
        raise CustomModuleError("Something went wrong")
    elif test_command == "create_build":
        build_manager.create_build(
            source_ids=[
                source_build.full_id
                for source_build in build_manager.get_builds("test_src_module")
                if source_build.status == autostart.BuildStatus.COMPLETED
            ],
            properties={"test_key": "test_value"},
        )
    elif test_command == "find_region":
        last_build = build_manager.get_last_completed("test_module")
        for source_id in last_build.source_ids:
            region = build_manager.get_build(source_id).properties["region"]

        build_manager.create_build(
            source_ids=[
                build_manager.get_last_completed("test_src_module", region=region).full_id
            ],
            properties={"test_key": "test_value"},
        )
    else:
        raise RuntimeError(f"Got unexpected test_command {test_command}")


def run():
    run_module("test_module", fill_graph, handle_build_status=handle_build_status)
