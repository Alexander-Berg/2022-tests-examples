# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import mock
import pytest
import requests

from django.core.management import call_command
from django.test import override_settings
from core.tests.factory import ReporterFactory, RewardFactory

from core import models
from core.utils.balance import BalanceClient


pytestmark = pytest.mark.django_db


RESIDENT_DS_VALUES = {
    'fname': 'Вася',
    'lname': 'Пупкин',
    'email': 'vasyan@yandex.r',
    'phone': '+7(800)555-35-35',
    'type': 'ph',
    'mname': 'Vyatcheslavovitch',
    'pfr': '111-222-333-44',
    'inn': '1',
    'legal-address-postcode': '1',
    'legal-address-gni': '1',
    'legal-address-region': '1',
    'legal-address-city': 'Kryzhopol',
    'legal-address-street': '1',
    'legal-address-home': '1',
    'legal-fias-guid': '1',
    'legaladdress': '1',
    'address-gni': '1',
    'address-region': '1',
    'address-postcode': '1',
    'address-code': '1',
    'birthday': '1999-12-31',
    'passport-d': '2010-03-10',
    'passport-s': '1',
    'passport-n': '1',
    'passport-e': '1',
    'passport-code': '1',
    'bank-type': '1',
    'bik': '041234567',
    'person-account': '1',
    'yamoney-wallet': '1',
    'payoneer-wallet': '1',
}


CREATE_CLIENT_ARGS = {
    'CITY': 'Kryzhopol',
    'NAME': 'Вася Vyatcheslavovitch',
    'IS_AGENCY': '0',
    'PHONE': '+7(800)555-35-35',
    'EMAIL': 'vasyan@yandex.r',
}


CREATE_PERSON_ARGS = {
    'bank-type': '1', 'legal-address-gni': '1', 'inn': '1', 'address-gni': '1',
    'address-region': '1', 'lname': 'Пупкин', 'address-postcode': '1', 'passport-code': '1',
    'fname': 'Вася', 'is_partner': '1', 'type': 'ph', 'email': 'vasyan@yandex.r',
    'legal-address-street': '1', 'mname': 'Vyatcheslavovitch', 'legal-address-home': '1',
    'phone': '+7(800)555-35-35', 'birthday': '1999-12-31', 'client_id': '106998205', 'address-code': '1',
    'passport-n': '1', 'passport-e': '1', 'passport-d': '2010-03-10', 'legal-address-postcode': '1',
    'pfr': '111-222-333-44', 'legal-address-region': '1', 'passport-s': '1', 'legal-address-city': 'Kryzhopol',
    'person-account': '1', 'bik': '041234567',
}


CREATE_CONTRACT_ARGS = {
    'currency': 'RUB', 'firm_id': '1', 'services': [207], 'signed': '1', 'client_id': '106998205',
    'person_id': '1234567', 'nds': '0', 'pay_to': '1', 'manager_uid': '010201030102010',
}


@pytest.mark.parametrize('current_contracts', [[], [{'PERSON_ID': '1234567', 'EXTERNAL_ID': 'БУЛ-10101'}]])
def test_create_balance_data(current_contracts):
    reporter = ReporterFactory(uid=1337)
    reward = RewardFactory(reporter=reporter, payment_currency=None)
    with mock.patch('core.utils.balance.BalanceClient.send_create_client') as mocked_create_client, \
        mock.patch('core.utils.balance.BalanceClient.send_create_person') as mocked_create_person, \
        mock.patch('core.utils.balance.BalanceClient.send_create_offer') as mocked_create_offer, \
        mock.patch('core.utils.balance.BalanceClient.send_get_client_contracts') as mocked_get_contracts, \
        mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_ds, \
        mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
                mocked_ds.return_value = RESIDENT_DS_VALUES
                mocked_create_client.return_value = [0, 'SUCCESS', 106998205]
                mocked_create_person.return_value = '1234567'
                mocked_create_offer.return_value = {'EXTERNAL_ID': 'ИНТ-32767'}
                mocked_get_contracts.return_value = current_contracts
                payment_info = models.NewPaymentInfo(reporter=reporter)
                payment_info.save()
                call_command('bugbounty_create_balance_data')
    reward.refresh_from_db()
    assert reward.status == models.Reward.ST_PAYABLE
    assert reward.payment_currency == models.Reward.C_RUR
    reward.reporter.refresh_from_db()
    assert reward.reporter.balance_client_id == '106998205'
    assert reward.reporter.balance_person_id == '1234567'
    expected_contract_id = 'ИНТ-32767' if not current_contracts else 'БУЛ-10101'
    assert reward.reporter.balance_contract_id == expected_contract_id

    assert mocked_create_client.call_args[0][1] == CREATE_CLIENT_ARGS
    assert mocked_create_person.call_args[0][1] == CREATE_PERSON_ARGS
    if current_contracts:
        assert mocked_create_offer.call_count == 0
    else:
        assert mocked_create_offer.call_args[0][1] == CREATE_CONTRACT_ARGS


@pytest.mark.parametrize('current_contracts', [[], [{'PERSON_ID': '1234567', 'EXTERNAL_ID': 'БУЛ-10101'}]])
def test_create_nonresident_contract(current_contracts):
    reporter = ReporterFactory(uid=1337, balance_client_id='106998205', balance_nonresident_person_id='1234567')
    reward = RewardFactory(reporter=reporter, payment_currency=None)
    with mock.patch('core.utils.balance.BalanceClient.send_create_offer') as mocked_create_offer, \
            mock.patch('core.utils.balance.BalanceClient.send_get_client_contracts') as mocked_get_contracts, \
            mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_ds, \
            mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
        mocked_ds.return_value = {'type': 'ytph'}
        mocked_create_offer.return_value = {'EXTERNAL_ID': 'ИНТ-32767'}
        mocked_get_contracts.return_value = current_contracts
        payment_info = models.NewPaymentInfo.objects.create(reporter=reporter)
        payment_info.refresh_from_db()  # Сходить в замоканный датасинк
        client = BalanceClient()
        client.create_contract(reward)
    assert reward.payment_currency == models.Reward.C_USD
    expected_contract_id = 'ИНТ-32767' if not current_contracts else 'БУЛ-10101'
    assert reward.reporter.balance_contract_id == expected_contract_id
    reporter.refresh_from_db()
    if current_contracts:
        assert mocked_create_offer.call_count == 0
        assert reporter.balance_contract_id == 'БУЛ-10101'
    else:
        expected_contract_args = CREATE_CONTRACT_ARGS.copy()
        expected_contract_args['currency'] = 'USD'
        assert mocked_create_offer.call_args[0][1] == expected_contract_args
        assert reporter.balance_contract_id == 'ИНТ-32767'


@pytest.mark.parametrize('current_contracts', [[], [{'PERSON_ID': '1234567', 'EXTERNAL_ID': '7654321'}]])
def test_create_balance_data_for_another_bank(current_contracts):
    reporter = ReporterFactory(uid=1337)
    reward = RewardFactory(reporter=reporter, payment_currency=1)
    with mock.patch('core.utils.balance.BalanceClient.send_create_client') as mocked_create_client, \
        mock.patch('core.utils.balance.BalanceClient.send_create_person') as mocked_create_person, \
        mock.patch('core.utils.balance.BalanceClient.send_create_offer') as mocked_create_offer, \
        mock.patch('core.utils.balance.BalanceClient.send_get_client_contracts') as mocked_get_contracts, \
        mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_ds, \
        mock.patch('core.models.user.datasync.update_or_create_payment_info_entry'):
            another_data = RESIDENT_DS_VALUES.copy()
            another_data['bank-type'] = '2'
            another_data['bik'] = '047654321'
            mocked_ds.return_value = another_data
            mocked_create_client.return_value = [0, 'SUCCESS', 106998205]
            mocked_create_person.return_value = '1234567'
            mocked_create_offer.return_value = {'EXTERNAL_ID': '1234567'}
            mocked_get_contracts.return_value = current_contracts
            payment_info = models.NewPaymentInfo(reporter=reporter)
            payment_info.save()
            call_command('bugbounty_create_balance_data')
    reward.refresh_from_db()
    assert reward.status == models.Reward.ST_PAYABLE
    assert mocked_create_client.call_args[0][1] == CREATE_CLIENT_ARGS
    another_bank_args = CREATE_PERSON_ARGS.copy()
    another_bank_args['bank-type'] = '2'
    another_bank_args['bik'] = '047654321'
    assert mocked_create_person.call_args[0][1] == another_bank_args
    if current_contracts:
        assert mocked_create_offer.call_count == 0
    else:
        assert mocked_create_offer.call_args[0][1] == CREATE_CONTRACT_ARGS


def test_create_data_user_failure():
    reporter = ReporterFactory(uid=1337)
    reward = RewardFactory(reporter=reporter, payment_currency=1, startrek_ticket_code='1')
    with mock.patch('app.utils.startrek.comment_on_ticket_key') as mocked_notify:
        call_command('bugbounty_create_balance_data')
        reward.refresh_from_db()
        assert 'User\'s payment info was not submitted' in reward.balance_error_text
        assert len(mocked_notify.call_args_list) == 1
        assert reward.status == models.Reward.ST_BAD_DETAILS


def test_create_data_server_failure():
    reporter = ReporterFactory(uid=1337)
    reward = RewardFactory(reporter=reporter, payment_currency=1, startrek_ticket_code='1')
    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_get_ds,\
            mock.patch('core.models.user.datasync.update_or_create_payment_info_entry') as mocked_post_ds:
        mocked_get_ds.return_value = {}
        models.NewPaymentInfo.objects.create(reporter=reporter)

    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_ds,\
            mock.patch('app.utils.startrek.comment_on_ticket_key') as mocked_notify,\
            mock.patch('core.models.reward.send_mail') as mocked_send_mail:
        mocked_ds.side_effect = requests.exceptions.Timeout

        assert reward.balance_contract_attempts == 3

        call_command('bugbounty_create_balance_data')
        reward.refresh_from_db()
        assert 'Could not get payment info from Datasync' in reward.balance_error_text
        assert reward.balance_contract_attempts == 2
        assert mocked_notify.call_args is None
        assert mocked_send_mail.call_args is None
        assert reward.status == models.Reward.ST_NEW  # ретраимся

        call_command('bugbounty_create_balance_data')
        reward.refresh_from_db()
        assert 'Could not get payment info from Datasync' in reward.balance_error_text
        assert reward.balance_contract_attempts == 1
        assert mocked_notify.call_args is None
        assert mocked_send_mail.call_args is None
        assert reward.status == models.Reward.ST_NEW  # и снова ретраимся...

        call_command('bugbounty_create_balance_data')
        reward.refresh_from_db()
        assert 'Could not get payment info from Datasync' in reward.balance_error_text
        assert reward.balance_contract_attempts == 3  # попытки сбросились
        assert len(mocked_notify.call_args_list) == 1
        assert len(mocked_send_mail.call_args_list) == 1
        assert mocked_send_mail.call_args[0][0] == 'Reward {}: payment issues'.format(reward.pk)  # subject
        assert reward.status == models.Reward.ST_BAD_DETAILS  # не вышло :(


# proceed == True (переход в bad_details), когда хотя бы куда-то получилось отправить
@pytest.mark.parametrize('mail_failure,tracker_failure,proceed',
                         [
                             (False, False, True),
                             (False, True, True),
                             (True, False, True),
                             (True, True, False),
                         ])
def test_tracker_and_mail_failure(mail_failure, tracker_failure, proceed):
    reporter = ReporterFactory(uid=1337)
    reward = RewardFactory(reporter=reporter, payment_currency=1, startrek_ticket_code='1', balance_contract_attempts=1)
    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_get_ds,\
            mock.patch('core.models.user.datasync.update_or_create_payment_info_entry') as mocked_post_ds:
        mocked_get_ds.return_value = {}
        models.NewPaymentInfo.objects.create(reporter=reporter)

    with mock.patch('core.models.user.datasync.get_payment_info_entry') as mocked_ds,\
            mock.patch('app.utils.startrek.comment_on_ticket_key') as mocked_notify,\
            mock.patch('core.models.reward.send_mail') as mocked_send_mail:
        mocked_ds.side_effect = requests.exceptions.Timeout
        if tracker_failure:
            mocked_notify.side_effect = Exception
        if mail_failure:
            mocked_send_mail.side_effect = Exception

        assert reward.balance_contract_attempts == 1

        call_command('bugbounty_create_balance_data')
        reward.refresh_from_db()
        assert 'Could not get payment info from Datasync' in reward.balance_error_text
        assert len(mocked_notify.call_args_list) == 1
        assert len(mocked_send_mail.call_args_list) == 1
        if proceed:
            assert reward.status == models.Reward.ST_BAD_DETAILS  # не вышло :(
            assert reward.balance_contract_attempts == 3  # попытки сбросились
        else:
            assert reward.status == models.Reward.ST_NEW  # не вышло даже записать ошибку. Будем пытаться ещё раз
            assert reward.balance_contract_attempts == 1  # последняя попытка не прошла
