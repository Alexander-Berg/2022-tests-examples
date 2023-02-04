# coding: utf-8
import uuid
from copy import deepcopy

import mock
import pytest

from balance import mapper
from balance.constants import ServiceId
from balance.mapper import PartnerProcessingSource
from balance.processors import process_payments
from tests.balance_tests.invoices.invoice_common import create_personal_account, create_invoice


@pytest.fixture
def personal_account(session):
    return create_personal_account(session)


enqueue_params = [
    {"mquery": {"current_signed.services": {"$in": [ServiceId.DIRECT]}}, "queue": "PARTNER_FAST_BALANCE",
     "enqueue_object": "contract", "priority": 1, "input": None}]


def get_expected_enqueue_params(contract):
    return {(contract, 'PARTNER_FAST_BALANCE'): ({}, 1)}


@pytest.mark.parametrize("process_payments_enqueue_params", [enqueue_params, None])
@pytest.mark.parametrize("personal_account_enqueue_params", [enqueue_params, None])
def test_partner_processing_enqueuer(session, process_payments_enqueue_params,
                                     personal_account_enqueue_params):
    # Создаем процессинг
    source = PartnerProcessingSource(
        code='test_%s_partner_processing' % uuid.uuid4(),
        queue='PARTNER_PROCESSING',
        process_payments_enqueue_params=process_payments_enqueue_params,
        personal_account_enqueue_params=personal_account_enqueue_params,
        enabled=1
    )
    session.add(source)
    session.flush()

    with mock.patch(
        'balance.processors.enqueue_partner_processing.payment_partner_processing_enqueuer._enqueue') as payment_enqueue_mock, mock.patch(
        'balance.processors.enqueue_partner_processing.personal_account_partner_processing_enqueuer._enqueue') as personal_account_enqueue_mock:
        # Создаем аккаунт
        account = create_personal_account(session=session)
        session.add(account)
        session.flush()

        # Ожидаемые значения для простановки в PARTNER_FAST_BALANCE в зависимости от колонки
        expected_personal_account_enqueue_params = get_expected_enqueue_params(
            account.contract) if personal_account_enqueue_params else {}
        expected_process_payments_enqueue_params = get_expected_enqueue_params(
            account.contract) if process_payments_enqueue_params else {}

        # Вызывается personal_account_partner_processing_enqueuer.process в enqueue_ng
        personal_account_enqueue_mock.assert_called_once_with(session, expected_personal_account_enqueue_params)
        # payment_partner_processing_enqueuer.process не вызывается
        payment_enqueue_mock.assert_not_called()

        process_payments.handle_invoice(account)

        # Вызывается payment_partner_processing_enqueuer.process в handle_invoice
        payment_enqueue_mock.assert_called_once_with(session, expected_process_payments_enqueue_params)


def test_personal_account_partner_processing_enqueuer_not_called_for_invoice(session):
    """простановщик PersonalAccountPartnerProcessingEnqueuer вызывается только для Personal Account, не для всех Invoice"""

    with mock.patch(
        'balance.processors.enqueue_partner_processing.personal_account_partner_processing_enqueuer.process') as process_mock:
        account = create_invoice(session=session)
        session.add(account)
        session.flush()
        process_mock.assert_not_called()


def test_processing_exception(session):
    params = deepcopy(enqueue_params)
    del params[0]['enqueue_object']

    # Создаем кривой процессинг
    source = PartnerProcessingSource(
        code='test_%s_partner_processing' % uuid.uuid4(),
        queue='PARTNER_PROCESSING',
        personal_account_enqueue_params=params,
        enabled=1
    )
    session.add(source)
    session.flush()
    with mock.patch(
        'balance.processors.enqueue_partner_processing.log.info') as log_mock:
        account = create_personal_account(session=session)
        session.add(account)
        session.flush()
        log_mock.assert_called_once_with(
            'Enqueuer:PersonalAccountPartnerProcessingEnqueuer, %s: mquery, queue, or enqueue_object not specified in enqueue_params' % source.code)


def test_querying_exception(session):
    exception_msg = 'problems on querying'
    with mock.patch(
        'balance.processors.enqueue_partner_processing.personal_account_partner_processing_enqueuer.query_processors',
        side_effect=Exception(exception_msg)), \
         mock.patch('balance.processors.enqueue_partner_processing.log.error') as error_log_mock:
        account = create_personal_account(session=session)
        session.add(account)
        session.flush()
        error_log_mock.assert_called_once_with("Error on invoice: {}, {}", account, exception_msg)
