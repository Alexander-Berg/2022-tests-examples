import datetime as dt
import mongomock
import pytest
import pytz

from maps.garden.sdk.module_rpc import common as rpc_common
from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType

from maps.garden.libs_server.common.log_types import LogRecordType
from maps.garden.libs_server.log_storage.module_log_storage import ModuleLogStorage
from maps.garden.tools.module_monitoring.lib.module_logs import generate_module_log_events


@pytest.mark.parametrize(
    "logs",
    [
        [],
        [
            {
                "added_at": dt.datetime(2019, 11, 27, 8, 12, 29, 187000, tzinfo=pytz.utc),
                "contour_name": "contour_name",
                "exceptions": None,
                "log_type": LogRecordType.AUTOSTARTER,
                "message": "message text",
                "module_name": "module_map",
                "module_version": "some_version",
                "username": "test_user"
            },
            {
                "added_at": dt.datetime(2019, 11, 27, 8, 12, 28, 187000, tzinfo=pytz.utc),
                "contour_name": "contour_name",
                "exceptions": None,
                "log_type": LogRecordType.SCAN_RESOURCES,
                "message": "message text",
                "module_name": "module_source",
                "module_version": "some_version",
                "username": "test_user"
            }
        ],
        [
            {
                "added_at": dt.datetime(2019, 11, 27, 8, 12, 29, 187000, tzinfo=pytz.utc),
                "contour_name": "contour_name",
                "exceptions": [
                    {
                        "type": "exception_type_1",
                        "message": "exception_message_1",
                        "traceback": (
                            "exception_traceback_1:\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 268, in process_protobuf_command_invoke_task\n"
                            "invoke_task(\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 234, in invoke_task\n"
                            "arguments.execute(task, demands + creates)\n"
                            "File \"maps/garden/sdk/core/core/arguments.py\", line 106, in execute\n"
                            "return func(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/core/tools/optional.py\", line 42, in __call__\n"
                            "super().__call__(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 48, in __call__\n"
                            "self._run_query(self._query_template, **resources)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 109, in _run_query\n"
                            "raise YqlError(errors)"
                        )
                    },
                    {
                        "type": "exception_type_2",
                        "message": "exception_message_2",
                        "traceback": (
                            "exception_traceback_2:\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 268, in process_protobuf_command_invoke_task\n"
                            "invoke_task(\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 234, in invoke_task\n"
                            "arguments.execute(task, demands + creates)\n"
                            "File \"maps/garden/sdk/core/core/arguments.py\", line 106, in execute\n"
                            "return func(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/core/tools/optional.py\", line 42, in __call__\n"
                            "super().__call__(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 48, in __call__\n"
                            "self._run_query(self._query_template, **resources)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 109, in _run_query\n"
                            "raise YqlError(errors)"
                        )
                    },
                    {
                        "type": "exception_type_3",
                        "message": "exception_message_3",
                        "traceback": (
                            "exception_traceback_3:\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 268, in process_protobuf_command_invoke_task\n"
                            "invoke_task(\n"
                            "File \"maps/garden/sdk/module_rpc/module_runner.py\", line 234, in invoke_task\n"
                            "arguments.execute(task, demands + creates)\n"
                            "File \"maps/garden/sdk/core/core/arguments.py\", line 106, in execute\n"
                            "return func(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/core/tools/optional.py\", line 42, in __call__\n"
                            "super().__call__(*args, **kwargs)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 48, in __call__\n"
                            "self._run_query(self._query_template, **resources)\n"
                            "File \"maps/garden/sdk/yt/yql_tools.py\", line 109, in _run_query\n"
                            "raise YqlError(errors)"
                        )
                    },
                ],
                "log_type": LogRecordType.AUTOSTARTER,
                "message": "message text",
                "module_name": "module_map",
                "module_version": "some_version",
                "username": "test_user"
            },
            {
                "added_at": dt.datetime(2019, 11, 27, 8, 12, 28, 187000, tzinfo=pytz.utc),
                "contour_name": "contour_name",
                "exceptions": [
                    {
                        "type": "exception_type",
                        "message": "exception_message",
                        "traceback": "exception_traceback",
                    }
                ],
                "log_type": LogRecordType.SCAN_RESOURCES,
                "message": "message text",
                "module_name": "module_source",
                "module_version": "some_version",
                "username": "test_user"
            },
        ],
    ]
)
def test_module_logs(logs):
    mocked_db = mongomock.MongoClient(tz_aware=True).db
    logs_collection = mocked_db.module_logs

    if logs:
        logs_collection.insert_many(logs)

    module_traits_source = ModuleTraits(
        name="module_source",
        type=ModuleType.SOURCE,
        capabilities=[rpc_common.Capabilities.SCAN_RESOURCES]
    )

    module_traits_map = ModuleTraits(
        name="module_map",
        type=ModuleType.MAP,
    )

    return list(generate_module_log_events(
        "contour_name",
        [module_traits_source, module_traits_map],
        ModuleLogStorage(mocked_db),
        ui_hostname="localhost",
    ))
