from typing import Optional

import pytest

from django.forms.models import model_to_dict

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.models import Check, CheckHistory

from billing.dcsaap.backend.tests.utils.models import create_check


class TestCheck:
    """
    Тестирование логики модели Check
    """

    @pytest.fixture
    def check_active(self):
        return create_check("active check", enum.HAHN, '/t1', '/t2', 'k1 k2', 'v1 v2', '/res')

    @pytest.fixture
    def check_disabled(self):
        return create_check("active check", enum.HAHN, '/t1', '/t2', 'k1 k2', 'v1 v2', '/res1', Check.STATUS_DISABLED)

    def test_is_able_to_run(self, check_active: Check, check_disabled: Check):
        assert check_active.is_able_to_run()
        assert not check_disabled.is_able_to_run()

    def test_keys_list(self, check_active: Check):
        assert check_active.keys_list == ['k1', 'k2']

    def test_history_for_create(self, check_active: Check, check_disabled: Check):
        assert CheckHistory.objects.filter(origin=check_active).count() == 1
        assert CheckHistory.objects.filter(origin=check_disabled).count() == 1

        check_history = CheckHistory.objects.get(origin=check_active)
        for k, v in model_to_dict(check_active, exclude=["id"]).items():
            assert v == getattr(check_history, k, "+++")

    def test_history_for_update(self, check_active: Check):
        new_table = "/t22"
        check_active.table2 = new_table
        check_active.save()
        assert CheckHistory.objects.filter(origin=check_active).count() == 2
        assert CheckHistory.objects.get(origin=check_active, table2=new_table).table2 == new_table

    @pytest.mark.parametrize(
        'workflow_id,instance_id,operation_id,expected',
        [
            (None, None, None, False),
            (None, 'any', None, False),
            (None, None, 'any', False),
            ('any', None, None, False),
            ('any', 'any', None, True),
            ('any', None, 'any', True),
        ],
    )
    def test_has_aa(
        self,
        check_active: Check,
        workflow_id: Optional[str],
        instance_id: Optional[str],
        operation_id: Optional[str],
        expected: bool,
    ):
        check_active.aa_workflow_id = workflow_id
        check_active.aa_instance_id = instance_id
        check_active.aa_operation_id = operation_id
        assert check_active.has_aa() is expected
