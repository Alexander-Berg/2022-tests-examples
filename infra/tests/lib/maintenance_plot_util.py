import dataclasses
import itertools
import typing

from walle.maintenance_plot.common_settings import MaintenanceApprovers
from walle.maintenance_plot.scenarios_settings.base import BaseScenarioMaintenancePlotSettings, HostTaskOffsetTimes


@dataclasses.dataclass(eq=True, frozen=True)
class NoopScenarioSettings(BaseScenarioMaintenancePlotSettings):
    foo: typing.Optional[int] = None

    jsonschema: typing.ClassVar = {
        "type": "object",
        "properties": {
            "foo": {
                "anyOf": [{"type": "integer", "min_value": 0}, {"type": "null"}],
            },
        },
        "additionalProperties": False,
        "description": "'noop' scenario settings",
    }

    @classmethod
    def from_dict(cls, values_dict: dict):
        return NoopScenarioSettings(foo=values_dict.get("foo"))

    def to_dict(self):
        return {"foo": self.foo}

    def get_offset_times_of_host_tasks(self) -> HostTaskOffsetTimes:
        return HostTaskOffsetTimes()


TEST_ROLE_CODE = "test-role-code"
TEST_APPROVERS = MaintenanceApprovers(abc_roles_codes=[TEST_ROLE_CODE])
TEST_DEFAULT_MAINTENANCE_APPROVERS: typing.List[MaintenanceApprovers] = [
    TEST_APPROVERS,
]


def generate_abc_service_members_response(role_codes=None, role_scope_slugs=None):
    """Generates ABC responses' items for `abc.get_service_members()` method."""
    whatever = "whatever"

    def _get_mr_everywhere(_role_code=whatever, _role_scope_slug=whatever):
        return [
            {
                "person": {"login": "mr-everywhere", "is_robot": False},
                "role": {"scope": {"slug": _role_scope_slug}, "code": _role_code},
            }
        ]

    def _get_mr_robot(_role_code=whatever, _role_scope_slug=whatever):
        return [
            {
                "person": {"login": "mr-everywhere", "is_robot": False},
                "role": {"scope": {"slug": _role_scope_slug}, "code": _role_code},
            }
        ]

    def _get_persons_for_role_code(_role_code):
        return (
            [
                {
                    "person": {"login": f"{_role_code}-login-1", "is_robot": False},
                    "role": {"scope": {"slug": whatever}, "code": _role_code},
                },
                {
                    "person": {"login": f"{_role_code}-login-2", "is_robot": False},
                    "role": {"scope": {"slug": whatever}, "code": _role_code},
                },
            ]
            + _get_mr_everywhere(_role_code=_role_code)
            + _get_mr_robot(_role_code=_role_code)
        )

    def _get_persons_for_role_scope_slug(_role_scope_slug):
        return (
            [
                {
                    "person": {"login": f"{_role_scope_slug}-login", "is_robot": False},
                    "role": {"scope": {"slug": _role_scope_slug}, "code": whatever},
                },
            ]
            + _get_mr_everywhere(_role_scope_slug=_role_scope_slug)
            + _get_mr_robot(_role_scope_slug=_role_scope_slug)
        )

    if role_codes and role_scope_slugs:
        return paginate_abc_results(
            [_get_mr_everywhere(role_code) for role_code in role_codes]
            + [_get_mr_everywhere(role_scope_slug) for role_scope_slug in role_scope_slugs]
        )

    if role_codes:
        return paginate_abc_results([_get_persons_for_role_code(role_code) for role_code in role_codes])

    if role_scope_slugs:
        return paginate_abc_results(
            [_get_persons_for_role_scope_slug(role_scope_slug) for role_scope_slug in role_scope_slugs]
        )

    return paginate_abc_results([])


def paginate_abc_results(results):
    return {
        "count": 1,
        "next": None,
        "previous": None,
        "total_pages": 1,
        "results": list(itertools.chain.from_iterable(results)),
    }
