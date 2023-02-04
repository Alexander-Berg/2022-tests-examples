import mongomock

from maps.garden.sdk.module_traits.module_traits import (
    BuildsLimit,
    ModuleTraits,
    ModuleType,
)
from maps.garden.libs_server.monitorings import juggler

from maps.garden.libs_server.build.build_defs import Build, BuildStatus, BuildStatusString
from maps.garden.libs_server.build.builds_storage import BuildsStorage

from maps.garden.tools.module_monitoring.lib.build_limits import (
    check_build_limits,
)

MODULE_NAME = 'test'


def test_build_limits():
    mocked_db = mongomock.MongoClient(tz_aware=True).db
    builds_storage = BuildsStorage(mocked_db)
    contour_name = "test_contour_name"
    traits = ModuleTraits(
        name=MODULE_NAME,
        type=ModuleType.MAP,
        build_limits=[
            BuildsLimit(builds_grouping=['property', 'release'], max=1),
            BuildsLimit(builds_grouping=['property'], max=3),
            BuildsLimit(max=5),
        ],
    )

    def check_on_insert(extras):
        builds_storage.save(Build(
            id=check_on_insert.build_id,
            name=MODULE_NAME,
            contour_name=contour_name,
            status=BuildStatus(string=BuildStatusString.COMPLETED),
            extras=extras,
        ))
        check_on_insert.build_id += 1
        return check_build_limits(contour_name, traits, builds_storage, ui_hostname="localhost")

    check_on_insert.build_id = 1

    extras1 = {'property': 1, 'release': 1}

    code, msg = check_on_insert(extras1)
    assert code == juggler.StatusCode.OK

    code, msg = check_on_insert(extras1)
    assert code == juggler.StatusCode.WARN, msg
    assert str(extras1) in msg

    extras2 = {'property': 1, 'release': 2}

    code, msg = check_on_insert(extras2)
    assert str(extras2) not in msg
    assert str({'property': 1}) not in msg

    code, msg = check_on_insert(extras2)
    assert code == juggler.StatusCode.WARN, msg
    assert str(extras2) in msg
    assert str({'property': 1}) in msg

    extras3 = {'property': 2}

    code, msg = check_on_insert(extras3)
    assert code == juggler.StatusCode.WARN, msg
    assert 'grouped by {}' not in msg

    code, msg = check_on_insert(extras3)
    code, msg = check_on_insert(extras3)
    assert code == juggler.StatusCode.WARN, msg
    assert str(extras1) in msg
    assert str(extras2) in msg
    assert str(extras3) not in msg
    assert 'current=7, max=5' in msg

    return msg
