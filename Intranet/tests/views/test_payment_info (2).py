# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import io
from mock import patch, Mock
import pytest

from django import test
from django.conf import settings
from django.core.urlresolvers import reverse

from core.models import Administrator, Reporter, NewPaymentInfo, Reward
from core.tests.tools import use_account_type
import core.utils.datasync

from app.tests.views import MessagesMixin
from app.tests.views import YauthAdminTestCase


pytestmark = pytest.mark.django_db

IMAGE_PATH = '/app/core/tests/static_data/Malcolm.png'


@pytest.fixture
def patch_mail_template_form_choices(monkeypatch):
    import app.forms
    app.forms.MailTemplateForm.declared_fields['code']._choices = [('badge', 'badge')]
    monkeypatch.setattr('app.views.mail_template.MailTemplateForm', app.forms.MailTemplateForm)


@pytest.fixture
def patch_tvm(monkeypatch):
    monkeypatch.setattr(core.utils.datasync, 'get_tvm_ticket', mocked_get_tvm_ticket())
    monkeypatch.setattr(core.utils.datasync.requests, 'get', mocked_get(json={}))
    monkeypatch.setattr(core.utils.datasync.requests, 'put', mocked_put())


def mocked_get_tvm_ticket():
    mock = Mock()
    mock.return_value = 'MOCKED_TVM_TICKET'
    return mock


def mocked_get(status_code=200, json=None):
    mock = Mock()
    mock.return_value = MockedResponse(status_code, json)
    return mock


def mocked_put(status_code=200, json=None):
    mock = Mock()
    mock.return_value = MockedResponse(status_code, json)
    return mock


@pytest.fixture
def form_data(request):
    buffer = io.BytesIO()
    with open(IMAGE_PATH, 'rb') as document_file:
        buffer.write(document_file.read())
        buffer.name = document_file.name
        buffer.seek(0)
    data = {
        'is_russian_resident': True,
        'fname': 'Vasya',
        'lname': 'Pupkin',
        'email': 'vasyan@yandex.ru',
        'phone': '+7(800)555-35-35',
        'mname': 'Vyatcheslavovitch',
        'pfr': '111-222-333-44',
        'inn': '1',
        'legal_address_postcode': '1',
        'legal_address_gni': '1',
        'legal_address_region': '1',
        'legal_address_city': '1',
        'legal_address_street': '1',
        'legal_address_home': '1',
        'legal_fias_guid': '1',
        'legaladdress': '1',
        'address_gni': '1',
        'address_region': '1',
        'address_postcode': '1',
        'address_code': '1',
        'birthday': '1999-12-31',
        'passport_d': '2010-03-10',
        'passport_s': '1',
        'passport_n': '1',
        'passport_e': '1',
        'passport_code': '1',
        'bank_type': '1',
        'person_account': '1',
        'bik': '041234567',
        'yamoney_wallet': '1',
        'payoneer_wallet': '1',
        'city': '1',
        'country': 'Narniya',
        'postaddress': '1',
        'postcode': '1',
        'ben_bank': '1',
        'swift': 'FOOBAROO',
        'document': buffer,
    }
    request.cls.form_data = data


class MockedResponse(object):
    def __init__(self, status_code=200, json=None):
        self.status_code = status_code
        self._json = json

    def json(self):
        return self._json


@test.override_settings(YAUTH_TEST_USER={'uid': '1', 'login': '1', 'blackbox_result': {'password_verification_age': 1}})
@pytest.mark.usefixtures('patch_external_userinfo_by_login')
@pytest.mark.usefixtures('patch_external_userinfo_by_uid')
@pytest.mark.usefixtures('form_data', 'patch_tvm', 'patch_mail_template_form_choices')
class EditPaymentInfoTest(MessagesMixin, YauthAdminTestCase):
    @staticmethod
    def get_url(user):
        return reverse('user_profile:edit_payment', args=[user.id])

    @staticmethod
    def form_to_datasync(instance, data):
        datasync_data = {}
        if 'is_russian_resident' in data:
            datasync_data['type'] = 'ph' if data['is_russian_resident'] else 'ytph'
        if instance.pk:
            datasync_data['id'] = unicode(instance.pk)
        for key in data:
            if key == 'is_russian_resident':
                continue
            datasync_key = key.replace('_', '-')
            datasync_data[datasync_key] = data[key]
        return datasync_data

    @staticmethod
    def assert_datasync_push(saved_to_datasync, datasync_put_args):
        saved_to_datasync = saved_to_datasync.copy()
        datasync_put_args = datasync_put_args.copy()
        saved_to_datasync.pop('document')
        datasync_put_args.pop('cipher_key')

        # Это поле не передаётся в datasync (см. DATASYNC_FIELDS)
        saved_to_datasync.pop('account-type', None)

        assert saved_to_datasync == datasync_put_args

    @staticmethod
    def reset_form_document(form_data):
        buffer = io.BytesIO()
        with open(IMAGE_PATH, 'rb') as document_file:
            buffer.write(document_file.read())
            buffer.name = document_file.name
            buffer.seek(0)
        form_data['document'] = buffer

    def setUp(self):
        super(EditPaymentInfoTest, self).setUp()
        self.reporter = Reporter.objects.create(uid=1)
        NewPaymentInfo.objects.create(reporter=self.reporter)
        user = Administrator.objects.create(username='1')
        user.groups.create(name=settings.FINANCIAL_SUPPORT_GROUP)

    def _test_post_invalid_info(self, form_data):
        initial_data = self.form_to_datasync(self.reporter.new_payment_info, form_data)
        post_data = form_data.copy()
        post_data['pfr'] = '1'
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                    put.return_value = MockedResponse()
                    response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                    assert response.status_code == 200  # на фронтенде всегда отдаём 200
                    self.assert_error_message(response, 'Incorrect values were provided')
                    assert len(put.call_args_list) == 0

    test_post_invalid_info_for_account = use_account_type('account')(_test_post_invalid_info)
    test_post_invalid_info_for_ben_account = use_account_type('ben_account')(_test_post_invalid_info)

    def _test_post_valid_info_without_account_type(self, form_data):
        initial_data = self.form_to_datasync(self.reporter.new_payment_info, form_data)
        post_data = form_data.copy()
        post_data['account'] = '1234567'
        post_data['ben_account'] = '1234567'
        post_data['is_russian_resident'] = False
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                    put.return_value = MockedResponse()
                    response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                    assert response.status_code == 200  # на фронтенде всегда отдаём 200
                    self.assert_error_message(response, 'Incorrect values were provided')
                    assert len(put.call_args_list) == 0

    test_post_valid_info_without_account_type = use_account_type('none')(_test_post_valid_info_without_account_type)

    def _test_post_valid_info(self, form_data):
        initial_data = self.form_to_datasync(self.reporter.new_payment_info, form_data)
        post_data = form_data.copy()
        post_data['fname'] = 'Ivan'
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                    put.return_value = MockedResponse()
                    response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                    assert response.status_code == 200
                    self.assert_success_message(response, 'User payment info successfully updated')

                    new_data = initial_data.copy()
                    new_data['fname'] = 'Ivan'
                    self.assert_datasync_push(new_data, put.call_args_list[0][1]['json'])

    test_post_valid_info_for_account = use_account_type('account')(_test_post_valid_info)
    test_post_valid_info_for_ben_account = use_account_type('ben_account')(_test_post_valid_info)

    def _test_post_valid_info_with_different_types(self, form_data):
        # parametrize для бедных, потому что нормальный не работает
        for old_russian_resident in [True, False]:
            for new_russian_resident in [True, False]:
                old_form = form_data.copy()
                old_form['is_russian_resident'] = old_russian_resident
                initial_data = self.form_to_datasync(self.reporter.new_payment_info, old_form)
                post_data = form_data.copy()
                self.reset_form_document(post_data)
                post_data['address_code'] = '100500'
                post_data['ben_bank'] = '100600'
                post_data['is_russian_resident'] = new_russian_resident
                with patch('core.utils.datasync.requests.put') as put:
                    with patch('core.utils.datasync.requests.get') as get:
                        with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                            get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                            get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                            put.return_value = MockedResponse()
                            response = self.client.post(
                                self.get_url(Reporter.objects.get(uid=1)),
                                follow=True,
                                data=post_data
                            )
                            assert response.status_code == 200
                            self.assert_success_message(response, 'User payment info successfully updated')

                            new_data = initial_data.copy()
                            new_data['address-code'] = '100500'
                            new_data['ben-bank'] = '100600'
                            new_data['type'] = 'ph' if new_russian_resident else 'ytph'
                            self.assert_datasync_push(new_data, put.call_args_list[0][1]['json'])

    test_post_valid_info_with_different_types_for_account = use_account_type('account')(_test_post_valid_info_with_different_types)
    test_post_valid_info_with_different_types_for_ben_account = use_account_type('ben_account')(_test_post_valid_info_with_different_types)

    def _test_balance_validation(self, form_data):
        initial_data = self.form_to_datasync(self.reporter.new_payment_info, form_data)
        post_data = form_data.copy()
        post_data['bank_type'] = '2'
        post_data['bik'] = '041234567'
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    with patch('core.utils.balance.BalanceClient.send_data') as mocked_create_person:
                        description = 'Bank with BIK=041234567 not found in DB'
                        mocked_create_person.return_value = (({
                            'errors': [{'description': description, 'err_code': -1, 'field': 'BIK'}],
                            'state': -1
                        },), None)
                        get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                        get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                        put.return_value = MockedResponse()
                        response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                        assert response.status_code == 200
                        self.assert_error_message(response, 'Incorrect values were provided')
                        assert len(put.call_args_list) == 0
                        assert mocked_create_person.call_count == 1

    test_balance_validation_for_account = use_account_type('account')(_test_balance_validation)
    test_balance_validation_for_ben_account = use_account_type('ben_account')(_test_balance_validation)

    def test_get_empty_payment_info(self):
        Reporter.objects.get().new_payment_info.delete()
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(status_code=400)  # в датасинке нету данных
                    put.return_value = MockedResponse()
                    url = self.get_url(Reporter.objects.get(uid=1))
                    response = self.client.get(url, follow=True)
                    assert response.status_code == 200
                    assert not NewPaymentInfo.objects.exists()

    def _test_first_time_post(self, form_data):
        reporter = Reporter.objects.get()
        reporter.new_payment_info.delete()
        post_data = form_data.copy()
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(status_code=400)  #  Записи в датасинке нет
                    put.return_value = MockedResponse()
                    response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                    assert response.status_code == 200
                    self.assert_success_message(response, 'User payment info successfully updated')
                    instance = NewPaymentInfo.objects.get()
                    datasync_data = self.form_to_datasync(instance, post_data)

                    # account и ben-account должны передаваться с пустотой, если не заданы
                    datasync_data.setdefault('account', '')
                    datasync_data.setdefault('ben-account', '')

                    self.assert_datasync_push(datasync_data, put.call_args_list[0][1]['json'])

    test_first_time_post_for_account = use_account_type('account')(_test_first_time_post)
    test_first_time_post_for_ben_account = use_account_type('ben_account')(_test_first_time_post)

    def _test_update_rewards_statuses(self, form_data):
        # Создадим 2 выплаты - в статусе 1 и 2
        reward_kwargs = {
            'staff_uid': 1000,
            'staff_login': 'admin',
            'startrek_ticket_code': 1,
            'payment_amount_usd': 100,
            'payment_amount_rur': 3000,
            'points': 100,
            'ticket_created': '2014-10-30 10:38:06',
            'reporter': Reporter.objects.filter(uid=1).first(),
        }
        r1 = Reward.objects.create(status=Reward.ST_PAYABLE, **reward_kwargs)
        r2 = Reward.objects.create(status=Reward.ST_BAD_DETAILS, **reward_kwargs)

        initial_data = self.form_to_datasync(self.reporter.new_payment_info, form_data)
        post_data = form_data.copy()
        post_data['fname'] = 'Ivan'
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                with patch('core.utils.datasync.get_tvm_ticket') as get_tvm_ticket:
                    get_tvm_ticket.return_value = 'MOCKED_TVM_TICKET'
                    get.return_value = MockedResponse(json=initial_data)  # отдаём старые данные
                    put.return_value = MockedResponse()
                    response = self.client.post(self.get_url(Reporter.objects.get(uid=1)), follow=True, data=post_data)
                    assert response.status_code == 200
                    self.assert_success_message(response, 'User payment info successfully updated')

        r1.refresh_from_db()
        r2.refresh_from_db()
        assert r1.status == Reward.ST_PAYABLE
        assert r2.status == Reward.ST_NEW

    test_update_rewards_statuses_for_account = use_account_type('account')(_test_update_rewards_statuses)
    test_update_rewards_statuses_for_ben_account = use_account_type('ben_account')(_test_update_rewards_statuses)
