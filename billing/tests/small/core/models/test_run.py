import decimal
import datetime
from typing import Any

import pytest

from django.db import IntegrityError
from django.test import override_settings

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models.run import RunOverridableFieldsCopier
from billing.dcsaap.backend.core.models import Run, Check, Diff, OverridableFields

from billing.dcsaap.backend.tests.utils.models import create_check, create_run, create_diff


class TestRun:
    """
    Тестирование логики модели Run
    """

    @pytest.fixture
    def check(self, result='/res'):
        return create_check('check', enum.HAHN, '/t1', '/t2', 'k1 k2', 'v1 v2', result)

    def test_result_inheritance(self, check: Check):
        """
        Проверяем что Run.result корректно наследуется от Check.result
        """
        expected_result = '/res'
        run = create_run(check)
        assert run.result == expected_result

    def test_result_overridden(self):
        """
        Проверяем, что наследование Check.result можно переопределить
        """
        expected_result = '/res'
        check = create_check('check', enum.HAHN, '/t1', '/t2', 'k1 k2', 'v1 v2', '/unexpected')
        run = create_run(check, expected_result)
        assert run.result == expected_result

    def test_unique_run(self, check: Check):
        """
        Проверяем, что Run(check_model, result, status=0) может быть только один
        """
        create_run(check)

        with pytest.raises(IntegrityError, match='UNIQUE constraint failed'):
            create_run(check)

    def test_not_unique_run(self, check: Check):
        """
        Проверяем, что несколько Run с разными result или state != 0 могут существовать
        """
        create_run(check)
        create_run(check, '/another')
        create_run(check, status=Run.STATUS_FINISHED)

    def test_work_time(self, check: Check):
        """
        Проверяем корректность подсчета времени работы сверки
        """
        run = create_run(check)
        assert run.work_time is None

        run.started = datetime.datetime(2000, 1, 1)
        assert run.work_time is None

        run.finished = datetime.datetime(2000, 1, 2)
        assert run.work_time == datetime.timedelta(days=1)

    def test_current_diffs_ui_url(self, check: Check):
        """
        Проверяем корректность получения URL до конкретного запуска
        """
        run = create_run(check)

        app_ui_url = 'http://dcs/ui#'
        with override_settings(APP_UI_URL=app_ui_url):
            expected_url = f'{app_ui_url}/diffs?filter=%7B%22run%22%3A%20{run.id}%7D'
            assert run.current_diffs_ui_url == expected_url

    def test_update_diffs_on_success(self, check: Check):
        """
        Проверяем подсчет расхождений по завершению работы запуска
        """
        run = create_run(check, status=Run.STATUS_FINISHED)
        create_diff(run, 'k1', 'k1_value1', 'column', '1', '2')
        create_diff(run, 'k1', 'k1_value2', 'column', '3', '4')
        run.done(is_success=True)
        assert run.diffs_count == 2

    def test_internal_result_with_ttl(self, some_run: Run):
        """
        Проверяем формирование пути до результатов конкретного запуска
        """
        expected = f'//home/balance_reports/dcsaap/dev/run-diffs-{some_run.id}'
        assert some_run.internal_result_with_ttl == expected

    def test_cluster_url(self, some_run: Run):
        assert some_run.cluster_url == 'hahn.yt.yandex.net'


class TestMaterialThreshold:
    def test_simple(self, some_run: Run):
        some_run.check_model.material_threshold = 'v2:100'

        create_diff(some_run, 'k1', 'key1', 'v1', '1', '2')
        create_diff(some_run, 'k1', 'key1', 'v2', '10', '20')
        create_diff(some_run, 'k1', 'key2', 'v1', '3', '4')
        create_diff(some_run, 'k1', 'key2', 'v2', '30', '40')
        create_diff(some_run, 'k1', 'key2', 'v1', '5', '6')
        create_diff(some_run, 'k1', 'key2', 'v2', '50', '60', Diff.STATUS_CLOSED)

        thresholds = some_run.calculate_material_thresholds()
        assert len(thresholds) == 1
        assert thresholds[0]['sum'] == decimal.Decimal('20')

    def test_expression(self, some_run: Run):
        some_run.check_model.material_threshold = 'v1/10:10 v2:100'

        create_diff(some_run, 'k1', 'key1', 'v1', '10', '20')
        create_diff(some_run, 'k1', 'key1', 'v2', '10', '20')
        create_diff(some_run, 'k1', 'key2', 'v1', '30', '40')
        create_diff(some_run, 'k1', 'key2', 'v2', '30', '40')

        thresholds = some_run.calculate_material_thresholds()
        result = {threshold['name']: threshold['sum'] for threshold in thresholds}
        expected = {
            'v1': decimal.Decimal('2'),
            'v2': decimal.Decimal('20'),
        }
        assert result == expected


class TestRunOverridableFieldsCopier:
    """
    Тестирование логики работы `RunOverridableFieldsCopier`.
    """

    def test_is_overridable_field(self):
        """
        Провреяем корректность определения переопределяемых полей.
        """
        for field_name in OverridableFields.fields():
            result = RunOverridableFieldsCopier.is_overridable_field(field_name)
            assert result, f'{field_name} is not overridable'

        for field_name in ('__module__', '_meta', 'Meta', 'fields'):
            result = RunOverridableFieldsCopier.is_overridable_field(field_name)
            assert not result, f'{field_name} is overridable'

    def test_invalid(self, some_run: Run):
        """
        Провреяем, что некорректное поле не будет переопределено.
        """
        some_run.check_model._meta.value = 'strange value'

        c = RunOverridableFieldsCopier(some_run)
        c.copy_field_value('_meta')

        result = getattr(some_run._meta, 'value', None)
        assert result is None, 'value copied'

    @pytest.mark.parametrize(
        'field_name,check_value',
        zip(
            OverridableFields.fields(),
            ('/expected', enum.HAHN, 123456),
        ),
    )
    def test_inheritance(self, some_run: Run, field_name: str, check_value: Any):
        """
        Провряем, что незаполненные поля корректно наследуются от сверки.
        """
        c = RunOverridableFieldsCopier(some_run)

        setattr(some_run, field_name, None)
        setattr(some_run.check_model, field_name, check_value)
        c.copy_field_value(field_name)

        assert getattr(some_run.check_model, field_name) == getattr(some_run, field_name)

    @pytest.mark.parametrize(
        'field_name,run_value,check_value',
        zip(
            OverridableFields.fields(),
            ('//any/expected/path', enum.FREUD, 123456),
            ('//any/unexpected/path', enum.ARNOLD, 654321),
        ),
    )
    def test_overridden(self, some_run: Run, field_name: str, run_value: Any, check_value: Any):
        """
        Провряем, что заполненные поля не перезаписываются.
        """
        c = RunOverridableFieldsCopier(some_run)

        setattr(some_run, field_name, run_value)
        setattr(some_run.check_model, field_name, check_value)
        c.copy_field_value(field_name)

        assert getattr(some_run, field_name) == run_value
