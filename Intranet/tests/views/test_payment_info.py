# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import io

import freezegun
import pytest

import app.views.payment_info
import app.views.profile
import core.utils.datasync

from functools import partial

from django import test
from django.core.urlresolvers import reverse
from django.utils import timezone
from mock import Mock, patch

from app import forms
from app.tests.views import BaseProfileTestCase
from core import models
from core.misc import captcha
from core.tests.tools import use_account_type


use_account_type = partial(use_account_type, initial_data_attr='foreign_form_data')


IMAGE_PATH = '/app/core/tests/static_data/Malcolm.png'

@test.override_settings(YAUTH_TEST_USER={'uid': '1', 'login': '1', 'blackbox_result': {'password_verification_age': 1}})
@pytest.mark.usefixtures('patch_captcha')
@pytest.mark.usefixtures('patch_datasync')
@pytest.mark.usefixtures('foreign_form_data', 'russian_form_data')
class PaymentInfoTest(BaseProfileTestCase):
    url = reverse('payment-info')

    def _post_response_context(self, key='key', captcha='key', **kwargs):
        data = kwargs
        data['captcha_key'] = key
        data['captcha'] = captcha
        return self._post_response(data).context

    def test_form_in_context(self):
        context = self._get_response_context()
        assert isinstance(context['form'], forms.NewPaymentInfoForm)

    def test_invalid_captcha(self):
        context = self._post_response_context(captcha='captcha')
        assert len(context['form'].errors['captcha'])
        assert ('The characters were entered incorrectly. Please try again'
                in context['form'].errors['captcha'])

    def test_required_fields(self):
        form_data = self.russian_form_data
        del form_data['birthday']
        del form_data['pfr']
        del form_data['passport_s']
        context = self._post_response_context(**form_data)
        form_errors = context['form'].errors
        assert 'birthday' in form_errors
        assert 'pfr' in form_errors
        assert 'passport_s' in form_errors

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
        if not data.get('is_russian_resident'):
            # account и ben-account должны передаваться с пустотой, если не заданы
            datasync_data.setdefault('account', '')
            datasync_data.setdefault('ben-account', '')
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

    def test_fill_russian_info(self):
        assert models.NewPaymentInfo.objects.count() == 0

        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**self.russian_form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, self.russian_form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            new_data = self.russian_form_data.copy()
            self.reset_form_document(new_data)
            new_data['fname'] = 'Ivan'
            context = self._post_response_context(**new_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # переиспользуем существующий NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, new_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    def test_fill_russian_info_with_another_bank(self):
        assert models.NewPaymentInfo.objects.count() == 0

        form_data = self.russian_form_data.copy()
        form_data['bank_type'] = '2'
        form_data['bik'] = '041234567'

        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            new_data = form_data.copy()
            new_data['fname'] = 'Ivan'
            self.reset_form_document(new_data)
            context = self._post_response_context(**new_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # переиспользуем существующий NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, new_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    def test_russian_info_error(self):
        form_data = self.russian_form_data
        form_data['birthday'] = '0'  # некорректный формат
        context = self._post_response_context(**form_data)
        form_errors = context['form'].errors
        assert len(form_errors) == 1
        assert 'birthday' in form_errors

    def test_update_russian_info(self):
        assert models.NewPaymentInfo.objects.count() == 0

        # Сохраняем всё в DataSync в первый раз
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**self.russian_form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, self.russian_form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        update_data = {'phone': '+79876543210', 'is_russian_resident': True}  # меняем только телефон
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                get.return_value = MockedResponse(json=saved_to_datasync)  # отдаём старые данные
                put.return_value = MockedResponse()
                context = self._post_response_context(**update_data)
                assert len(context['form'].errors) == 0
                payment_info = models.NewPaymentInfo.objects.get()
                assert len(put.call_args_list) == 1
                updated_info = self.russian_form_data.copy()
                updated_info.update(update_data)
                saved_to_datasync = self.form_to_datasync(payment_info, updated_info)
                self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    def test_update_russian_info_with_another_bank(self):
        assert models.NewPaymentInfo.objects.count() == 0

        form_data = self.russian_form_data.copy()
        form_data['bank_type'] = '2'
        form_data['bik'] = '041234567'

        # Сохраняем всё в DataSync в первый раз
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        update_data = {'phone': '+79876543210', 'is_russian_resident': True}  # меняем только телефон
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                get.return_value = MockedResponse(json=saved_to_datasync)  # отдаём старые данные
                put.return_value = MockedResponse()
                context = self._post_response_context(**update_data)
                payment_info = models.NewPaymentInfo.objects.get()
                assert len(put.call_args_list) == 1

        update_data = {'phone': '+79876543210', 'is_russian_resident': True, 'bik': '041234567'}  # меняем только телефон, БИК прежний
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                get.return_value = MockedResponse(json=saved_to_datasync)  # отдаём старые данные
                put.return_value = MockedResponse()
                context = self._post_response_context(**update_data)
                assert len(context['form'].errors) == 0
                payment_info = models.NewPaymentInfo.objects.get()
                assert len(put.call_args_list) == 1
                updated_info = form_data.copy()
                updated_info.update(update_data)
                saved_to_datasync = self.form_to_datasync(payment_info, updated_info)
                self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    def _test_fill_foreign_info(self, form_data):
        assert models.NewPaymentInfo.objects.count() == 0
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert len(put.call_args_list) == 1
            assert models.NewPaymentInfo.objects.count() == 1
            payment_info = models.NewPaymentInfo.objects.get()
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    test_fill_foreign_info_for_account = use_account_type('account')(_test_fill_foreign_info)
    test_fill_foreign_info_for_ben_account = use_account_type('ben_account')(_test_fill_foreign_info)

    def _test_update_foreign_info(self, form_data):
        assert models.NewPaymentInfo.objects.count() == 0
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert len(put.call_args_list) == 1
            assert models.NewPaymentInfo.objects.count() == 1
            payment_info = models.NewPaymentInfo.objects.get()
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        update_data = {'fname': 'Ivan'}
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                put.return_value = MockedResponse()
                get.return_value = MockedResponse(json=saved_to_datasync)
                context = self._post_response_context(**update_data)

                assert len(context['form'].errors) == 0
                assert len(put.call_args_list) == 1
                assert models.NewPaymentInfo.objects.count() == 1
                payment_info = models.NewPaymentInfo.objects.get()
                new_data = form_data.copy()
                new_data.update(update_data)
                saved_to_datasync = self.form_to_datasync(payment_info, new_data)
                self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

    test_update_foreign_info_for_account = use_account_type('account')(_test_update_foreign_info)
    test_update_foreign_info_for_ben_account = use_account_type('ben_account')(_test_update_foreign_info)

    @use_account_type('account')
    def test_validate_russian_values(self, update_data):
        # При смене резидентства нужно провалидировать, что уже созраненные значения написаны латиницей
        assert models.NewPaymentInfo.objects.count() == 0

        # Сохраняем всё в DataSync в первый раз
        with patch('core.utils.datasync.requests.put') as put:
            form_data = self.russian_form_data.copy()
            form_data['fname'] = 'Вася'
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        update_data.pop('fname')
        update_data.pop('lname')
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                put.return_value = MockedResponse()
                get.return_value = MockedResponse(json=saved_to_datasync)
                self.reset_form_document(update_data)
                context = self._post_response_context(**update_data)
                form_errors = context['form'].errors
                assert len(form_errors) == 1
                assert form_errors.keys() == ['fname']

    @use_account_type('account')
    def test_foreign_info_error(self, form_data):
        del form_data['swift']
        context = self._post_response_context(**form_data)
        form_errors = context['form'].errors
        assert form_errors.keys() == ['swift']

    @use_account_type('account')
    def test_fill_foreign_info_latin_validation(self, form_data):
        form_data['country'] = 'Угандушка'
        context = self._post_response_context(**form_data)
        form_errors = context['form'].errors
        assert form_errors.keys() == ['country']
        assert form_errors['country'][0] == 'The field must contain only latin characters'

    @use_account_type('account')
    def test_update_foreign_info_latin_validation(self, form_data):
        assert models.NewPaymentInfo.objects.count() == 0
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert len(put.call_args_list) == 1
            assert models.NewPaymentInfo.objects.count() == 1
            payment_info = models.NewPaymentInfo.objects.get()
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        update_data = {'country': 'Угандушка'}
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                put.return_value = MockedResponse()
                get.return_value = MockedResponse(json=saved_to_datasync)
                context = self._post_response_context(**update_data)

                form_errors = context['form'].errors
                assert len(form_errors) == 1
                assert form_errors.keys() == ['country']
                assert form_errors['country'][0] == 'The field must contain only latin characters'
                assert len(put.call_args_list) == 0

    def _test_switch_from_russian_to_foreign(self, form_data):
        assert models.NewPaymentInfo.objects.count() == 0

        # Сохраняем всё в DataSync в первый раз
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**self.russian_form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            payment_info = models.NewPaymentInfo.objects.get()
            assert len(put.call_args_list) == 1
            saved_to_datasync = self.form_to_datasync(payment_info, self.russian_form_data)
            self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])

        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                get.return_value = MockedResponse(json=saved_to_datasync)  # отдаём старые данные
                put.return_value = MockedResponse()
                self.reset_form_document(form_data)
                context = self._post_response_context(**form_data)
                assert len(context['form'].errors) == 0
                payment_info = models.NewPaymentInfo.objects.get()
                assert len(put.call_args_list) == 1
                updated_info = self.russian_form_data.copy()
                updated_info.update(form_data)
                saved_to_datasync = self.form_to_datasync(payment_info, updated_info)
                self.assert_datasync_push(saved_to_datasync, put.call_args_list[0][1]['json'])
                assert put.call_args_list[0][1]['json']['type'] == 'ytph'

    test_switch_from_russian_to_foreign_for_account = use_account_type('account')(_test_switch_from_russian_to_foreign)
    test_switch_from_russian_to_foreign_for_ben_account = use_account_type('ben_account')(_test_switch_from_russian_to_foreign)

    def test_russian_fill_in_balance_validation(self):
        assert models.NewPaymentInfo.objects.count() == 0

        # Если баланс нам явно говорит, что в каком-то поле ошибка, прокидываем ее
        form_data = self.russian_form_data.copy()
        form_data['bank_type'] = '2'
        form_data['bik'] = '041234567'
        with patch('core.utils.balance.BalanceClient.send_data') as mocked_create_person:
            description = 'Bank with BIK=041234567 not found in DB'
            mocked_create_person.return_value = (({
                'errors': [{'description': description, 'err_code': -1, 'field': 'BIK'}],
                'state': -1
            },), None)
            context = self._post_response_context(**form_data)
        form_errors = context['form'].errors
        assert form_errors.keys() == ['bik']
        assert form_errors['bik'][0] == description
        assert mocked_create_person.call_count == 1

        # Если баланс отдает исключение, оставляем как есть
        def balance_fail(*args):
            assert False
        with patch('core.utils.balance.BalanceClient.send_data') as mocked_create_person:
            with patch('core.utils.datasync.requests.put') as put:
                put.return_value = MockedResponse()
                mocked_create_person.side_effect = balance_fail
                self.reset_form_document(form_data)
                context = self._post_response_context(**form_data)
        assert len(context['form'].errors) == 0
        assert len(put.call_args_list) == 1
        assert models.NewPaymentInfo.objects.count() == 1
        assert models.NewPaymentInfo.objects.get().balance_simulation_result == 'assert False'
        assert mocked_create_person.call_count == 3

    def test_too_big_document_scan(self):
        assert models.NewPaymentInfo.objects.count() == 0
        form_data = self.russian_form_data.copy()

        self.reset_form_document(form_data)
        form_data['document'].name = 'a'*65
        context = self._post_response_context(**form_data)
        assert context['form'].errors['document'] == ['Ensure this filename has at most 64 characters (it has 65).']

        self.reset_form_document(form_data)
        with test.override_settings(MAX_ATTACHMENT_SIZE=5):
            context = self._post_response_context(**form_data)
        assert context['form'].errors['document'] == ['File size cannot be more than 5 bytes']

        self.reset_form_document(form_data)
        with test.override_settings(MAX_ATTACHMENT_SIZE=2048):
            context = self._post_response_context(**form_data)
        assert context['form'].errors['document'] == ['File size cannot be more than 2.0 KB']

    def test_attach_document_scan(self):
        assert models.NewPaymentInfo.objects.count() == 0
        form_data = self.russian_form_data.copy()

        with freezegun.freeze_time('2019-09-01 11:00:00'),\
                patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0
            assert models.NewPaymentInfo.objects.count() == 1  # в случае отсутствия создаём объект NewPaymentInfo
            assert len(put.call_args_list) == 1
            put_values = put.call_args_list[0][1]['json']

        payment_info = models.NewPaymentInfo.objects.get()
        assert payment_info.encrypted_document_content
        assert payment_info.encrypted_document_name
        assert payment_info.document_uploaded_at == timezone.datetime(2019, 9, 1, 11, 0, 0, tzinfo=timezone.utc)

        with patch('core.utils.datasync.requests.get') as get:
            with open(IMAGE_PATH, 'rb') as input_image:
                get.return_value = MockedResponse(json=put_values.copy())  # отдаём старые данные
                assert payment_info.datasync_values['document'].read() == input_image.read()

        del form_data['document']
        with freezegun.freeze_time('2019-09-01 11:00:00'),\
                patch('core.utils.datasync.requests.put') as put,\
                patch('core.utils.datasync.requests.get') as get:
            put.return_value = MockedResponse()
            get.return_value = MockedResponse(json=put_values.copy())  # отдаём старые данные
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0

        payment_info = models.NewPaymentInfo.objects.get()
        assert payment_info.document_uploaded_at == timezone.datetime(2019, 9, 1, 11, 0, 0, tzinfo=timezone.utc)  # старый таймстемп

        self.reset_form_document(form_data)
        with freezegun.freeze_time('2019-09-02 12:00:00'),\
                patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            get.return_value = MockedResponse(json=put_values.copy())  # отдаём старые данные
            context = self._post_response_context(**form_data)
            assert len(context['form'].errors) == 0

        payment_info = models.NewPaymentInfo.objects.get()
        assert payment_info.document_uploaded_at == timezone.datetime(2019, 9, 2, 12, 0, 0, tzinfo=timezone.utc)  # новый таймстемп

    def _test_update_rewards_statuses(self, form_data):
        # Если изменится платёжная информация,
        # должны измениться статусы выплат (ST_BAD_DETAILS -> ST_NEW)
        assert models.NewPaymentInfo.objects.count() == 0
        with patch('core.utils.datasync.requests.put') as put:
            put.return_value = MockedResponse()
            self._post_response_context(**form_data)
            assert models.NewPaymentInfo.objects.count() == 1
            payment_info = models.NewPaymentInfo.objects.get()
            saved_to_datasync = self.form_to_datasync(payment_info, form_data)

        # Создадим 2 выплаты - в статусе 1 и 2
        reward_kwargs = {
            'staff_uid': 1000,
            'staff_login': 'admin',
            'startrek_ticket_code': 1,
            'payment_amount_usd': 100,
            'payment_amount_rur': 3000,
            'points': 100,
            'ticket_created': '2014-10-30 10:38:06',
            'reporter': models.Reporter.objects.filter(uid=1).first(),
        }
        r1 = models.Reward.objects.create(status=models.Reward.ST_PAYABLE, **reward_kwargs)
        r2 = models.Reward.objects.create(status=models.Reward.ST_BAD_DETAILS, **reward_kwargs)

        update_data = {'fname': 'Ivan'}
        with patch('core.utils.datasync.requests.put') as put:
            with patch('core.utils.datasync.requests.get') as get:
                put.return_value = MockedResponse()
                get.return_value = MockedResponse(json=saved_to_datasync)
                self._post_response_context(**update_data)

        r1.refresh_from_db()
        r2.refresh_from_db()
        assert r1.status == models.Reward.ST_PAYABLE
        assert r2.status == models.Reward.ST_NEW

    test_update_rewards_statuses_for_account = use_account_type('account')(_test_update_rewards_statuses)
    test_update_rewards_statuses_for_ben_account = use_account_type('ben_account')(_test_update_rewards_statuses)


@pytest.fixture
def patch_captcha(monkeypatch):
    monkeypatch.setattr(
        app.views.payment_info.captcha, 'Captcha', captcha.CaptchaMock)
    monkeypatch.setattr(
        app.forms, 'Captcha', captcha.CaptchaMock)


@pytest.fixture
def patch_datasync(monkeypatch):
    monkeypatch.setattr(core.utils.datasync, 'get_tvm_ticket', mocked_get_tvm_ticket())
    monkeypatch.setattr(core.utils.datasync.requests, 'get', mocked_get(json={}))
    monkeypatch.setattr(core.utils.datasync.requests, 'put', mocked_put())


@pytest.fixture
def common_form_data():
    buffer = io.BytesIO()
    with open(IMAGE_PATH, 'rb') as document_file:
        buffer.write(document_file.read())
        buffer.name = document_file.name
        buffer.seek(0)
    return {
        'fname': 'Vasya',
        'lname': 'Pupkin',
        'email': 'vasyan@yandex.ru',
        'phone': '+7(800)555-35-35',
        'document': buffer,
    }


@pytest.fixture
def russian_form_data(request, common_form_data):
    form_data = {
        'is_russian_resident': True,
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
    }
    form_data.update(common_form_data)
    request.cls.russian_form_data = form_data


@pytest.fixture
def foreign_form_data(request, common_form_data):
    form_data = {
        'is_russian_resident': False,
        'country': 'Uganda',
        'city': '1',
        'postaddress': '1',
        'postcode': '1',
        'ben_bank': '1',
        'swift': 'FOOBAROO',
        'birthday': '1999-12-31',
    }
    form_data.update(common_form_data)
    request.cls.foreign_form_data = form_data


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


class MockedResponse(object):
    def __init__(self, status_code=200, json=None):
        self.status_code = status_code
        self._json = json

    def json(self):
        return self._json
