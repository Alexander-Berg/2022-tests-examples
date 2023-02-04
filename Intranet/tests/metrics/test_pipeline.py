import pytest
from django.utils import timezone

from idm.core.constants.action_tiny import ACTION_TINY, STATUS
from idm.core.models import ActionTiny, System
from idm.metrics.pipeline import useful_pipelines_time

pytestmark = [pytest.mark.django_db]


def test_count_successful_pipelines():
    system_slug = 'abc'
    system = System.objects.create(slug=system_slug)

    started = timezone.now() - timezone.timedelta(hours=1)
    finished = started
    for _ in range(11):
        ActionTiny.objects.create(
            action=ACTION_TINY.PIPELINE_FINISHED,
            status=STATUS.OK,
            start=timezone.now(),
            finish=finished,
        )
        finished += timezone.timedelta(hours=1)
    ActionTiny.objects.create(
        action=ACTION_TINY.PIPELINE_FINISHED, status=STATUS.ERROR, start=timezone.now(), finish=finished
    )
    ActionTiny.objects.update(system=system, start=started)

    assert useful_pipelines_time() == [
        {
            'context': {'system': 'abc'},
            'values': [{'slug': 'time_50', 'value': 14400.0}, {'slug': 'time_75', 'value': 25200.0},
                       {'slug': 'time_90', 'value': 28800.0}, {'slug': 'time_99', 'value': 32400.0},
                       {'slug': 'count', 'value': 11}]
        },
        {
            'context': {'system': '__ALL__'},
            'values': [{'slug': 'time_50', 'value': 14400.0}, {'slug': 'time_75', 'value': 25200.0},
                       {'slug': 'time_90', 'value': 28800.0}, {'slug': 'time_99', 'value': 32400.0},
                       {'slug': 'count', 'value': 11}]
        },
    ]
