import dataclasses
import typing

import attr

from infra.walle.server.tests.lib.maintenance_plot_util import NoopScenarioSettings
from walle.scenario.constants import ScriptName
from walle.scenario.data_storage.data_storage import DefaultDataStorage
from walle.scenario.definitions.base import BaseScenarioDefinition
from walle.scenario.scenario import Scenario
from walle.scenario.scenario_fsm import _run_scenario  # noqa


@attr.s
class NoopParams:
    maintenance_start_time: typing.Optional[int] = attr.ib(
        default=None, validator=[attr.validators.optional(attr.validators.instance_of(int))]
    )
    maintenance_end_time: typing.Optional[int] = attr.ib(
        default=None, validator=[attr.validators.optional(attr.validators.instance_of(int))]
    )


class NoopDataStorage(DefaultDataStorage):
    scenario_parameters_class: typing.ClassVar = NoopParams


@dataclasses.dataclass(frozen=True)
class NoopScenarioDefinition(BaseScenarioDefinition):
    script_name = ScriptName.NOOP
    data_storage = NoopDataStorage
    scenario_parameters = NoopParams
    maintenance_plot_settings = NoopScenarioSettings


def launch_scenario(scenario: Scenario) -> Scenario:
    _run_scenario(scenario.id)
    return Scenario.objects.get(scenario_id=scenario.id)
