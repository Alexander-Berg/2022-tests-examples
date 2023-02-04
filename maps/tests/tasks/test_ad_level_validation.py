import pytest

from maps.garden.sdk.test_utils.canonization import canonize_str
from maps.garden.modules.ymapsdf_osm.lib.validation import ad
from maps.libs.ymapsdf.py.ad import LevelKind


BASE_COUNT = 1000


@pytest.fixture(
    scope="function",
    params=[ad.LevelKindStats(
        total={LevelKind.PROVINCE: 0},
        deleted={LevelKind.PROVINCE: 0},
        added={LevelKind.PROVINCE: 0}
        ),
        ad.LevelKindStats(
        total={LevelKind.PROVINCE: BASE_COUNT},
        deleted={LevelKind.PROVINCE: 0},
        added={LevelKind.PROVINCE: 0}
        ),
        ad.LevelKindStats(
        total={LevelKind.PROVINCE: BASE_COUNT +
               int(BASE_COUNT * ad.LevelValidator._MAX_DIFF[LevelKind.PROVINCE]) / 100 + 1},
        deleted={LevelKind.PROVINCE: int(BASE_COUNT * ad.LevelValidator._MAX_DIFF[LevelKind.PROVINCE] / 100)},
        added={LevelKind.PROVINCE: int(BASE_COUNT * ad.LevelValidator._MAX_DIFF[LevelKind.PROVINCE] / 100)}
        )
    ],
    ids=[
        "zero_stats",
        "no_add_or_del",
        "MAX_DIFF_cap"
    ]
)
def valid_data(request):
    return request.param


def test_level_validation_pass(valid_data):
    validator = ad.LevelValidator(valid_data)
    assert validator.is_valid()


@pytest.fixture(
    scope="function",
    params=[
        ad.LevelKindStats(
        total={LevelKind.PROVINCE: BASE_COUNT},
        deleted={LevelKind.PROVINCE: BASE_COUNT - 1},
        added={LevelKind.PROVINCE: 0}
        ),
        ad.LevelKindStats(
        total={LevelKind.PROVINCE: BASE_COUNT},
        deleted={LevelKind.PROVINCE: 0},
        added={LevelKind.PROVINCE: BASE_COUNT - 1}
        ),
        ad.LevelKindStats(
        total={LevelKind.PROVINCE: BASE_COUNT},
        deleted={LevelKind.PROVINCE: 0},
        added={LevelKind.PROVINCE: BASE_COUNT}
        )
    ],
    ids=[
        "del_overflow",
        "add_overflow",
        "all_added"
    ]
)
def broken_data(request):
    return request.param


def test_level_validation_report(broken_data):
    validator = ad.LevelValidator(broken_data)
    assert not validator.is_valid()
    return canonize_str(
        data=validator.get_report(),
        file_name="data"
    )
