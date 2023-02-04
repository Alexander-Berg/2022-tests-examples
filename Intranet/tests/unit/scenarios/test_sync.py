import itertools
import pytest

from dataclasses import dataclass, asdict
from unittest.mock import patch

from ok.scenarios.choices import SCENARIO_TRACKER_MACRO_SOURCES
from ok.scenarios.models import ScenarioTrackerMacro, Scenario
from ok.tracker.choices import TRACKER_VARIABLES
from ok.tracker.helpers import get_macro_body
from ok.tracker.models import Queue

from tests import factories as f


pytestmark = pytest.mark.django_db


@dataclass
class FakeTrackerMacro:

    id: int
    name: str
    body: str

    @property
    def _value(self):
        return asdict(self)


@dataclass
class FakeMacro:

    tracker_id: int
    name: str
    body: str
    tracker_queue: Queue
    scenario: Scenario = None
    source: str = SCENARIO_TRACKER_MACRO_SOURCES.ok
    is_active: bool = True
    is_compatible: bool = True

    def as_ok(self):
        return asdict(self)

    def as_tracker(self):
        return FakeTrackerMacro(
            id=self.tracker_id,
            name=self.name,
            body=self.body,
        )


def test_macros_sync():
    default_macro_body = get_macro_body({})
    queue_a, queue_b = f.QueueFactory.create_batch(2)
    new_compatible_macro = FakeMacro(
        tracker_id=10,
        name='New compatible',
        body=default_macro_body,
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
    )
    new_incompatible_macro_unknown_param = FakeMacro(
        tracker_id=20,
        name='New incompatible: unknown param',
        body=get_macro_body({'unknown': 'true'}),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_readonly_param = FakeMacro(
        tracker_id=25,
        name='New incompatible: readonly param',
        body=get_macro_body({'create_comment': 'true'}),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_invalid_author = FakeMacro(
        tracker_id=30,
        name='New incompatible: invalid author',
        body=default_macro_body.replace(TRACKER_VARIABLES.current_user_login, 'author'),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_invalid_type = FakeMacro(
        tracker_id=40,
        name='New incompatible: invalid type',
        body=get_macro_body({'type': 'xxx'}),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_missing_required_param = FakeMacro(
        tracker_id=50,
        name='New incompatible: missing required param',
        body=default_macro_body.replace('_embedded=1', ''),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_has_scenario = FakeMacro(
        tracker_id=60,
        name='New incompatible: has scenario',
        body=get_macro_body({'scenario': 'filled'}),
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    new_incompatible_macro_too_long_name = FakeMacro(
        tracker_id=65,
        name='Too long'.ljust(256, '!'),
        body=default_macro_body,
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
        is_compatible=False,
    )
    non_ok_macro = FakeMacro(
        tracker_id=70,
        name='Non-OK',
        body='Body',
        tracker_queue=queue_a,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
    )
    unchanged_macro = FakeMacro(
        tracker_id=80,
        name='Unchanged',
        body=default_macro_body,
        tracker_queue=queue_b,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
    )
    changed_macro = FakeMacro(
        tracker_id=90,
        name='Changed',
        body=default_macro_body,
        tracker_queue=queue_b,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
    )
    changed_linked_macro = FakeMacro(
        tracker_id=100,
        name='Changed linked to scenario',
        body=default_macro_body,
        tracker_queue=queue_b,
        source=SCENARIO_TRACKER_MACRO_SOURCES.ok,
        scenario=f.ScenarioFactory(),
    )
    deleted_macro = FakeMacro(
        tracker_id=110,
        name='Deleted',
        body=default_macro_body,
        tracker_queue=queue_b,
        source=SCENARIO_TRACKER_MACRO_SOURCES.tracker,
    )
    deleted_linked_macro = FakeMacro(
        tracker_id=120,
        name='Deleted',
        body=default_macro_body,
        tracker_queue=queue_b,
        source=SCENARIO_TRACKER_MACRO_SOURCES.ok,
        scenario=f.ScenarioFactory(),
    )
    fake_macros = [
        new_compatible_macro,
        new_incompatible_macro_unknown_param,
        new_incompatible_macro_readonly_param,
        new_incompatible_macro_invalid_author,
        new_incompatible_macro_invalid_type,
        new_incompatible_macro_missing_required_param,
        new_incompatible_macro_has_scenario,
        new_incompatible_macro_too_long_name,
        non_ok_macro,
        unchanged_macro,
        changed_macro,
        changed_linked_macro,
    ]

    f.ScenarioTrackerMacroFactory(**unchanged_macro.as_ok())
    f.ScenarioTrackerMacroFactory(**changed_macro.as_ok())
    f.ScenarioTrackerMacroFactory(**changed_linked_macro.as_ok())
    f.ScenarioTrackerMacroFactory(**deleted_macro.as_ok())
    f.ScenarioTrackerMacroFactory(**deleted_linked_macro.as_ok())
    changed_macro.body = get_macro_body({'unknown': 'true'})
    changed_macro.is_compatible = False
    changed_linked_macro.name = '~Changed'

    @staticmethod
    def _get_macros(queue_name):
        yield from (m.as_tracker() for m in fake_macros if m.tracker_queue.name == queue_name)

    with patch('ok.scenarios.sync.ScenarioTrackerMacroSynchronizer._get_macros', _get_macros):
        from ok.scenarios.sync import sync_macros
        sync_macros()

    # Меняем обратно, чтобы проверить, что ничего не изменилось в БД,
    # т.к. макрос уже привязан к какому-то сценарию
    changed_linked_macro.name = 'Changed linked to scenario'
    # Обрезаем название, т.к. допустимо макс. 255 символов
    new_incompatible_macro_too_long_name.name = new_incompatible_macro_too_long_name.name[:255]
    expected_fake_macros = sorted([
        new_compatible_macro,
        new_incompatible_macro_unknown_param,
        new_incompatible_macro_readonly_param,
        new_incompatible_macro_invalid_author,
        new_incompatible_macro_invalid_type,
        new_incompatible_macro_missing_required_param,
        new_incompatible_macro_has_scenario,
        new_incompatible_macro_too_long_name,
        unchanged_macro,
        changed_macro,
        changed_linked_macro,
        deleted_linked_macro,
    ], key=lambda x: x.tracker_id)

    macros = list(ScenarioTrackerMacro.objects.order_by('tracker_id'))
    for macro, fake_macro in itertools.zip_longest(macros, expected_fake_macros):
        assert macro is not None, f'No macro {fake_macro.tracker_id}'
        assert fake_macro is not None, f'No fake macro {macro.tracker_id}'
        for attr, value in fake_macro.as_ok().items():
            assert getattr(macro, attr) == value, f'{macro.tracker_id}: {attr}'
