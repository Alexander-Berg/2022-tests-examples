"""
Тесты шаблонов
"""

import datetime

import pytest

from django.conf import settings
from django.utils import dateformat
from django.template.loader import render_to_string

from billing.dcsaap.backend.core.models import Check, Run, Diff

from billing.dcsaap.backend.tests.utils.models import create_diff


class TestTrackerTemplates:
    """
    Тесты шаблонов для Tracker
    """

    @pytest.fixture
    def prepare_data(self, some_run):
        create_diff(some_run, 'k1', 'k1_value', 'column', '1', '2', diff_type=Diff.TYPE_DIFF)
        create_diff(some_run, 'k1', 'k1_value', 'column', '3', '4', diff_type=Diff.TYPE_DIFF)
        create_diff(some_run, 'k1', 'k1_value2', 'column', '3', '4', diff_type=Diff.TYPE_NOT_IN_T1)

        some_run.started = datetime.datetime(2000, 1, 1, 0, 0, 0)

    def test_run_finished_summary(self, some_check: Check, some_run: Run):
        """
        Проверяем, как отрисовывается заголовок тикета по завершению работы сверки
        """
        summary = render_to_string('tracker/run_finished_summary.md', context={'run': some_run}).strip()

        run_started_str = dateformat.format(some_run.started, settings.SHORT_DATETIME_FORMAT)
        expected = f'{some_check.title} от {run_started_str}'
        assert summary == expected

    def test_run_finished_description(self, some_run: Run, prepare_data):
        """
        Проверяем, как отрисовывается тело тикета по завершению работы сверки
        """
        some_run.done(is_success=True, finished='2000-01-01 10:11:12')

        run_id = some_run.id

        run_started_str = dateformat.format(some_run.started, settings.DATETIME_FORMAT)
        run_finished_str = dateformat.format(some_run.finished, settings.DATETIME_FORMAT)

        description = render_to_string('tracker/run_finished_description.md', context={'run': some_run}).strip()
        expected = (
            f'Сверка с id {run_id} успешно завершилась.\n'
            f'Время работы: 10:11:12 ({run_started_str} {run_finished_str})\n'
            'Найдено расхождений (всего): 3\n'
            '\n'
            'Статистика уникальных расхождений:\n'
            f'{dict(Diff.TYPES)[Diff.TYPE_NOT_IN_T1]}(##/t1##): 1\n'
            f'{dict(Diff.TYPES)[Diff.TYPE_DIFF]}: 1\n'
            'Итого расхождений: 2\n'
            '\n'
            f'Результаты запуска: {some_run.current_diffs_ui_url}'
        )
        assert description == expected

    def test_run_finished_description_limit_exceed(self, some_run: Run, prepare_data):
        """
        Проверяем, как отрисовывается тело тикета по завершению работы сверки
        При этом был превышен лимит расхождений
        """
        some_run.diffs_count_limit = 3
        some_run.diffs_count_yt = 5
        some_run.done(is_success=True, finished='2000-01-01 10:11:12')

        run_id = some_run.id

        run_started_str = dateformat.format(some_run.started, settings.DATETIME_FORMAT)
        run_finished_str = dateformat.format(some_run.finished, settings.DATETIME_FORMAT)

        description = render_to_string('tracker/run_finished_description.md', context={'run': some_run}).strip()
        expected = (
            f'Сверка с id {run_id} успешно завершилась.\n'
            f'Время работы: 10:11:12 ({run_started_str} {run_finished_str})\n'
            'Найдено расхождений (всего): 5\n'
            '\n'
            '**Внимание! Лимит расхождений был превышен. В БД загружено только первые 3 расхождений.**\n'
            '\n'
            f'Результаты запуска: {some_run.current_diffs_ui_url}'
        )
        assert description == expected
