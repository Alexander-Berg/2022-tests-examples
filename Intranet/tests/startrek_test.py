from unittest import mock

from django.conf import settings

from staff.headcounts.headcounts_credit_management import Startrek, CreditRepayment
from staff.headcounts.tests.factories import CreditRepaymentRowFactory


def test_startrek_calls_create_issue_task_for_tick():
    # given
    path = 'staff.headcounts.headcounts_credit_management.gateways.startrek.create_issue'
    with mock.patch(path) as create_issue_mock:
        startrek = Startrek()
        startrek._ancestors_chain = mock.Mock(return_value='')
        credit_repayment = CreditRepayment(
            id=100500,
            author_login='some',
            comment='test_comment',
            ticket=None,
            is_active=True,
            rows=[CreditRepaymentRowFactory()],
            closed_at=None,
        )

        # when
        startrek.create_ticket(credit_repayment)

        # then
        create_issue_mock.assert_called_once_with(
            queue=settings.HEADCOUNT_QUEUE_ID,
            createdBy=credit_repayment.author_login,
            summary='Погашение кредитной позиции',
            description=mock.ANY,
            followers=[],
            unique=f'credit_repayment_{credit_repayment.id}_{settings.STAFF_HOST}',
        )
