from mock import patch
import pytest

from dateutil import parser
from decimal import Decimal

from django.conf import settings

from staff.budget_position import const
from staff.budget_position.workflow_service.gateways import OebsHireService
from staff.budget_position.workflow_service.entities import Change, BudgetPosition
from staff.lib.testing import BudgetPositionFactory


@pytest.fixture(autouse=True)
def mock_oebs_hire_service():
    # override autouse fixture to avoid monkeypatching OebsHireService._try_send
    return


@pytest.mark.django_db
@patch('staff.budget_position.workflow_service.gateways.oebs_hire_service.get_tvm_ticket_by_deploy')
def test_oebs_assignment_request(get_tvm_ticket_by_deploy_mock):
    oebs_hire_service = OebsHireService()

    budget_position = BudgetPositionFactory()

    with patch('staff.lib.requests.post') as requests_patch:
        # in order to result.get('id') return an Int
        requests_patch.return_value.json.return_value = {'id': 42}

        oebs_hire_service.send_change(Change(
            join_at=parser.parse('2019-08-13').date(),
            salary=Decimal('42000.00'),
            budget_position=BudgetPosition(budget_position.id, budget_position.code),
            position_type=const.PositionType.OFFER,
            ticket='SALARY-123',
        ))

        requests_patch.assert_called_once()
        # @see regarding [-1]: https://docs.python.org/3/library/unittest.mock.html#calls-as-tuples
        call_kwargs = requests_patch.call_args[-1]

        assert call_kwargs['url'] == settings.OEBS_ASSIGNMENT_CREATE_URL
        assert call_kwargs['data'] == (
            f'{{"positionNum": {budget_position.code}, '
            '"salary": 42000.0, '
            '"personId": null, '
            '"dateHire": "2019-08-13"}'
        )

        oebs_hire_service.send_change(Change(
            join_at=parser.parse('2020-01-01').date(),
            salary=Decimal('0.00'),
            budget_position=BudgetPosition(budget_position.id, budget_position.code),
            position_type=const.PositionType.OFFER,
            ticket='SALARY-123',
        ))

        call_kwargs = requests_patch.call_args[-1]

        assert call_kwargs['url'] == settings.OEBS_ASSIGNMENT_CREATE_URL
        assert call_kwargs['data'] == (
            f'{{"positionNum": {budget_position.code}, '
            '"salary": 0.0, '
            '"personId": null, '
            '"dateHire": "2020-01-01"}'
        )
